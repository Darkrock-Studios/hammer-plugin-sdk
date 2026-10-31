# hammer-plugin-sdk

The official kits for writing runtime plugins for [Hammer](https://github.com/Wavesonics/hammer-editor):
WebAssembly modules that follow the [Extism](https://extism.org) plugin convention, plus the
Hammer-specific import and exports described in hammer-editor's `docs/CLIENT-PLUGINS.md`.

| Directory | What |
| --- | --- |
| `c/include` | The C kit: `hammer.h`, on top of Extism's `extism-pdk.h` (BSD-3, see `EXTISM-PDK-LICENSE`); `hammer_json.h`, a JSON reader that needs no allocation; `hammer_out.h` for output, `hammer_text.h` for words, `hammer_image.h` for image references in Markdown, `hammer_base64.h` for an export's images, `hammer_action.h` for actions' requests and replies (messages, documents, and pages of fields and buttons), `hammer_diagnose.h` for checks underlined in the editor, `hammer_progress` for long actions, and `hammer_test.h` for native unit tests |
| `c/build.sh`, `c/test.sh` | Build C plugin directories into packages, and run their native unit tests |
| `rust/hammer` | The Rust kit: Hammer's imports, operations (typed, in `ops`), and the cache, with serde for JSON; `export`, `action`, and `diagnose` types, an export's images and an action's pages among them, `progress`, `http` requests to the hosts the manifest names, image references in Markdown (`images`), byte offsets, an optional `getrandom` backend, and a zeroing allocator for Wizer builds |
| `rust/tools/finish.py` | Makes a module built from crates written for the browser loadable by Hammer: drops wasm-bindgen's leftovers and checks the imports |
| `kotlin/hammer` | The Kotlin kit: typed requests and replies, an export's images and an action's pages among them, typed operations (`hammer.ops`), the cache, image references in Markdown, and for JVM tests a fake Hammer and `ActionDriver`, which runs an action through its pages as Hammer does |
| `api/operations.json` | Hammer's operations as `ops.list` describes them, from which both kits' typed operations are generated: Kotlin's `hammer.ops` and Rust's `hammer::ops` |
| `kotlin/codegen` | The generator, with KotlinPoet for Kotlin. `gradle :codegen:updateOperations` refreshes the snapshot from hammer-editor (`tools/dump-operations.sh`) and regenerates; `:codegen:checkOperations` fails when the generated code is out of date |
| `kotlin/gradle-plugin` | The `com.darkrockstudios.hammer.plugin` Gradle plugin: sets up a plugin project and builds its `.hammerplugin` |
| `kotlin/hello` | One small example of each kind of plugin, which [RECIPES.md](RECIPES.md) walks through: an export, actions replying with a message, with a document (one with fields and progress, one picking a character, one listing a scene's images), and with two pages carrying state, a text check, and a command |
| `tools/package.sh` | Packs a plugin directory and its built module into a `.hammerplugin` |
| `tools/pack-c.sh` | Packs the C kit into the tarball each release carries |

New to writing plugins? [RECIPES.md](RECIPES.md) has a hello-world version of each kind, in Kotlin.
Hammer's own plugins, in hammer-plugins, are fuller examples in each language.

## Using the kits

The Kotlin kit and its Gradle plugin are on Maven Central, the Rust kit on crates.io, and the C kit on
each [GitHub release](https://github.com/Darkrock-Studios/hammer-plugin-sdk/releases) as
`hammer-plugin-sdk-c-<version>.tar.gz`. [PUBLISHING.md](PUBLISHING.md) has how releases work, and the plan
for the rest.

- C: `#include "hammer.h"`, and build with the tarball's `build.sh <plugin directory>` (`c/build.sh` here),
  which puts `include/` on the include path and packs the directory's `manifest.toml`, `settings.toml`,
  `locales/`, and `resources/` with the module. `test.sh <plugin directory>` builds and runs its
  `test.c`. The [C template](https://github.com/Darkrock-Studios/hammer-plugin-c) is a working plugin to
  start from, with the headers vendored.
- Rust: depend on `hammer-plugin-kit = "<version>"` and write `use hammer::…`; build for
  `wasm32-unknown-unknown`, and pack the module with `tools/package.sh`. The
  [Rust template](https://github.com/Darkrock-Studios/hammer-plugin-rust) is a working plugin to start
  from.
- Kotlin: apply `id("com.darkrockstudios.hammer.plugin") version "<version>"`, with `mavenCentral()` among
  the repositories in `pluginManagement` and `dependencyResolutionManagement`; it adds
  `com.darkrockstudios.hammer:hammer-plugin-kit` at the same version, and `gradle hammerPlugin` builds the
  package. [RECIPES.md](RECIPES.md) starts with a new plugin. To build against a checkout of this repo
  instead, `includeBuild("../hammer-plugin-sdk/kotlin/gradle-plugin")` under `pluginManagement` and
  `includeBuild("../hammer-plugin-sdk/kotlin")`, and apply the plugin without a version.

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
