# hammer-plugin-sdk

The official kits for writing runtime plugins for [Hammer](https://github.com/Wavesonics/hammer-editor):
WebAssembly modules that follow the [Extism](https://extism.org) plugin convention, plus the
Hammer-specific import and exports described in hammer-editor's `docs/CLIENT-PLUGINS.md`.

| Directory | What |
| --- | --- |
| `c/include` | The C kit: `hammer.h`, on top of Extism's `extism-pdk.h` (BSD-3, see `EXTISM-PDK-LICENSE`); `hammer_json.h`, a JSON reader that needs no allocation; `hammer_out.h` for output, `hammer_text.h` for words, `hammer_action.h` for actions' requests and replies (messages, documents, and pages of fields and buttons), `hammer_diagnose.h` for checks underlined in the editor, `hammer_progress` for long actions, and `hammer_test.h` for native unit tests |
| `c/build.sh`, `c/test.sh` | Build C plugin directories into packages, and run their native unit tests |
| `rust/hammer` | The Rust kit: Hammer's imports, operations, and the cache, with serde for JSON; `action` and `diagnose` types, an action's pages among them, `progress`, byte offsets, an optional `getrandom` backend, and a zeroing allocator for Wizer builds |
| `rust/tools/finish.py` | Makes a module built from crates written for the browser loadable by Hammer: drops wasm-bindgen's leftovers and checks the imports |
| `kotlin/hammer` | The Kotlin kit: typed requests and replies, an action's pages among them, operations, the cache, and for JVM tests a fake Hammer and `ActionDriver`, which runs an action through its pages as Hammer does |
| `kotlin/gradle-plugin` | The `hammer.plugin` Gradle plugin: sets up a plugin project and builds its `.hammerplugin` |
| `kotlin/hello` | One small example of each kind of plugin, which [RECIPES.md](RECIPES.md) walks through: an export, actions replying with a message, with a document (one with fields and progress), and with two pages carrying state, a text check, and a command |
| `tools/package.sh` | Packs a plugin directory and its built module into a `.hammerplugin` |

New to writing plugins? [RECIPES.md](RECIPES.md) has a hello-world version of each kind, in Kotlin.
Hammer's own plugins, in hammer-plugins, are fuller examples in each language.

## Using the kits

The kits are not published yet: a plugin repository builds against a checkout of this one beside it.
[PUBLISHING.md](PUBLISHING.md) has the plan for publishing each.

- C: `#include "hammer.h"`, and build with `c/build.sh <plugin directory>`, which puts `c/include` on
  the include path and packs the directory's `manifest.toml`, `settings.toml`, `locales/`, and
  `resources/` with the module. `c/test.sh <plugin directory>` builds and runs its `test.c`.
- Rust: depend on the kit by path, `hammer = { path = "../hammer-plugin-sdk/rust/hammer" }`, build for
  `wasm32-unknown-unknown`, and pack the module with `tools/package.sh`.
- Kotlin: in `settings.gradle.kts`, `includeBuild("../hammer-plugin-sdk/kotlin/gradle-plugin")` under
  `pluginManagement`, and `includeBuild("../hammer-plugin-sdk/kotlin")` so the kit comes from here; a
  plugin project then applies `id("hammer.plugin")`, and `gradle :<plugin>:hammerPlugin` builds it.

## Building

- C: clang with the wasm32 target and lld (`sudo apt install clang lld`). `c/test.sh c/tests` runs the
  kit's own tests.
- Rust: [rustup](https://rustup.rs) and `rustup target add wasm32-unknown-unknown`, then `cargo build` in
  `rust/`. `cargo test --target x86_64-unknown-linux-gnu` (or your machine's target) runs the kit's tests.
- Kotlin: a JDK and any Gradle 9, then `kotlin/build.sh` for the example's package (set `GRADLE` to a
  Gradle launcher if `gradle` is not on the path). `gradle jvmTest` in `kotlin/` runs the kit's and the
  example's unit tests.

## Installing a plugin

In Hammer on desktop: Settings, Plugins, Install plugin, pick the `.hammerplugin`, approve its
permissions, and restart Hammer.

## License

MIT; see [LICENSE](LICENSE). `c/include/extism-pdk.h` is Extism's, under BSD-3
([EXTISM-PDK-LICENSE](c/include/EXTISM-PDK-LICENSE)).
