#!/usr/bin/env python3
"""Makes a cargo-built module loadable by Hammer.

Crates written for the browser, such as wasm-bindgen's, leave exports and imports only a JavaScript
host provides. This keeps only the plugin's own exports (those not starting with "__"), drops the code
nothing reaches from them, stands in a trap for any wasm-bindgen import still reachable, and fails if
the module still imports anything Hammer does not offer.

Usage: finish.py <in.wasm> <out.wasm>. Needs wabt (wasm-objdump, wat2wasm) and Binaryen (wasm-metadce,
wasm-merge) on PATH, or Binaryen's bin directory in BINARYEN.
"""
import json
import os
import re
import subprocess
import sys
import tempfile

ALLOWED_MODULES = ("extism:host/env", "extism:host/user")
ALLOWED_WASI = {"random_get", "clock_time_get"}
STUBBED_MODULES = ("__wbindgen_placeholder__", "__wbindgen_externref_xform__")


def tool(name):
    binaryen = os.environ.get("BINARYEN")
    return os.path.join(binaryen, name) if binaryen and name.startswith("wasm-") and name != "wasm-objdump" else name


def run(*args):
    return subprocess.run([tool(args[0]), *args[1:]], check=True, capture_output=True, text=True).stdout


def section(wasm, name):
    return run("wasm-objdump", "-x", "-j", name, wasm)


def exports(wasm):
    return re.findall(r'-> "([^"]+)"', section(wasm, "Export"))


def types(wasm):
    found = {}
    for index, params, results in re.findall(r"type\[(\d+)\] \(([^)]*)\) -> (\S.*)", section(wasm, "Type")):
        results = results.strip()
        found[int(index)] = (
            [p.strip() for p in params.split(",") if p.strip()],
            [] if results == "nil" else [r.strip() for r in results.strip("()").split(",")],
        )
    return found


def imports(wasm):
    return [
        (int(sig), module, field)
        for sig, module, field in re.findall(r"func\[\d+\] sig=(\d+) <([^.>]+(?::[^.>]+)*)\.([^>]+)>", section(wasm, "Import"))
    ]


def main(source, target):
    with tempfile.TemporaryDirectory() as scratch:
        kept = [name for name in exports(source) if not name.startswith("__")]
        graph = [{"name": "root", "root": True, "reaches": kept}] + [{"name": n, "export": n} for n in kept]
        graph_path = os.path.join(scratch, "graph.json")
        with open(graph_path, "w") as f:
            json.dump(graph, f)
        reduced = os.path.join(scratch, "reduced.wasm")
        run("wasm-metadce", "--all-features", source, "-f", graph_path, "-o", reduced)

        signatures = types(reduced)
        stubs = {}
        for sig, module, field in imports(reduced):
            if module in STUBBED_MODULES:
                stubs.setdefault(module, []).append((field, signatures[sig]))
        merged = reduced
        for module, functions in stubs.items():
            lines = ["(module"]
            for field, (params, results) in functions:
                param = f" (param {' '.join(params)})" if params else ""
                result = f" (result {' '.join(results)})" if results else ""
                lines.append(f'  (func (export "{field}"){param}{result} unreachable)')
            lines.append(")")
            wat = os.path.join(scratch, "stub.wat")
            with open(wat, "w") as f:
                f.write("\n".join(lines))
            stub = os.path.join(scratch, "stub.wasm")
            run("wat2wasm", wat, "-o", stub)
            out = os.path.join(scratch, f"merged-{len(os.listdir(scratch))}.wasm")
            run("wasm-merge", "--all-features", merged, "plugin", stub, module, "-o", out)
            merged = out

        # Again, since merging exports the stand-ins too. cargo's release profile has optimized the
        # code already; wasm-opt -Os crashes on modules this size.
        run("wasm-metadce", "--all-features", merged, "-f", graph_path, "-o", target)

    refused = [
        f"{module}.{field}"
        for _, module, field in imports(target)
        if module not in ALLOWED_MODULES and not (module == "wasi_snapshot_preview1" and field in ALLOWED_WASI)
    ]
    if refused:
        sys.exit(f"{target} imports what Hammer does not offer: {', '.join(refused)}")
    print(f"{target}: exports {', '.join(exports(target))}; {os.path.getsize(target)} bytes")


if __name__ == "__main__":
    main(*sys.argv[1:])
