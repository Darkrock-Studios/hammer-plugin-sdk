# Publishing the SDK

For now, plugin repositories build against a checkout of this one beside them. The goal is for a plugin
author to need nothing but the published kit for their language. This is the plan for getting there.

## Where things stand

| Language | What ships | Name today | Version today |
| --- | --- | --- | --- |
| Kotlin | The kit, a Kotlin Multiplatform library (`jvm` and `wasmWasi`) | `com.darkrockstudios.hammer:plugin-kit` | 0.4.0, in `kotlin/versions.properties` |
| Kotlin | The Gradle plugin, which adds the kit by those coordinates when no kit project is named | `hammer.plugin` | The kit's, compiled in |
| Rust | The kit crate, and `rust/tools/finish.py` | `hammer` (already taken on crates.io) | 0.1.0 |
| C | The headers in `c/include`, `c/build.sh`, `c/test.sh` | None | None |
| All | `tools/package.sh` | None | None |

Every kit already states the host API version it targets (`HAMMER_API_VERSION`, `API_VERSION`), and the
Gradle plugin refuses a manifest whose `api` differs.

## Before any release

1. **License.** MIT, for this repository, hammer-plugins, and hammer-plugin-development: a `LICENSE`
   in each, and the license named in every package's metadata. The C kit also ships Extism's BSD-3
   notice (`c/include/EXTISM-PDK-LICENSE`) with `extism-pdk.h`.
2. **A public home.** Push this repository to GitHub (`Wavesonics/hammer-plugin-sdk`). Releases,
   issues, and the crate's and POM's `repository` links point there.
3. **One version for the SDK.** Every kit is released together under one version (`sdk-v0.5.0`), so a
   plugin author reads one number, and a host API change moves every language at once. The versions
   stay 0.x until Hammer ships plugins in a release; after that, a new host API version is a new major
   version. `kotlin/versions.properties` becomes the SDK's one version file, read by every build.
4. **Names that are free.** Proposed below; each needs claiming before the first release.
5. **CI.** A GitHub Actions workflow that builds and tests every kit, plus `hello`, on each push and pull
   request, and a release workflow started by an `sdk-v*` tag that publishes all of them.
6. **Timing.** The first public release goes out with the Hammer release that ships runtime plugins
   (4.0), so nothing is published against a host API that may still change.

## Kotlin: Maven Central and the Gradle Plugin Portal

The easiest of the three: ComposeTextEditor already publishes to Maven Central under
`com.darkrockstudios` with `com.vanniktech.maven.publish`, `publishToMavenCentral`, and
`signAllPublications`, so the namespace, the signing key, and the Central Portal account exist.

- **The kit** goes to Maven Central as `com.darkrockstudios.hammer:hammer-plugin-kit`, with the same
  plugin setup as ComposeTextEditor. Kotlin Multiplatform publishes the `jvm` and `wasmWasi` variants
  and the root module, so a plugin's `commonMain` resolves the right one.
- **The Gradle plugin** goes to the Gradle Plugin Portal with an id under a domain we own,
  `com.darkrockstudios.hammer.plugin`, since the Portal expects ids to be namespaced. `hammer.plugin`
  stays as a second id through 0.x so existing builds keep working. It keeps adding the kit by
  coordinates, so an author writes only:

  ```kotlin
  plugins {
  	id("com.darkrockstudios.hammer.plugin") version "0.5.0"
  }
  ```

- The Gradle plugin also pins the Kotlin Gradle plugin it applies (2.4.10 today). Each SDK release
  records which Kotlin version it was tested with, since Hammer is tested against one Kotlin/Wasm
  output.

## Rust: crates.io

- **The crate** is published as `hammer-plugin` (`hammer` is taken; `hammer-plugin` and `hammer-pdk`
  looked free when this was written). An author who wants to keep writing `hammer::` renames it on
  import: `hammer = { package = "hammer-plugin", version = "0.5" }`. The `getrandom` feature stays
  optional.
- **The build.** A Rust plugin needs the `wasm32-unknown-unknown` target, sometimes `finish.py` and
  Wizer, and then packing. At first these ship in the GitHub release (below). Later, a
  `cargo-hammer` subcommand (`cargo install cargo-hammer`, then `cargo hammer package`) replaces them,
  building, finishing, and packing in one step, the way the Gradle plugin does for Kotlin.

## C: GitHub releases

C has no package registry worth targeting, so the C kit is a release asset:
`hammer-plugin-sdk-c-0.5.0.tar.gz` with `include/`, `build.sh`, `test.sh`, `package.sh`, both licenses,
and a SHA-256 checksum next to it. An author unpacks it anywhere and passes its directory to the
scripts, or vendors `include/` into their repository. The headers gain a
`HAMMER_KIT_VERSION` beside `HAMMER_API_VERSION`.

## The same GitHub release

Each `sdk-v*` tag makes one GitHub release holding the C tarball, `tools/package.sh` and
`rust/tools/finish.py` (until `cargo-hammer` exists), the built `hello.hammerplugin`, and release
notes listing the host API version, the Kotlin version, and what changed in each kit.

## After the first release

- **hammer-plugins and hammer-plugin-development switch** to the published kits: a version in
  `Cargo.toml`, the Gradle plugin by id and version, and the C tarball fetched at a pinned version and
  checksum by `build.sh`. Each keeps a way back to a local checkout for working on the SDK and a plugin
  together: a `[patch.crates-io]` in `.cargo/config.toml`, a Gradle `includeBuild` used when
  `../hammer-plugin-sdk` exists, and a `HAMMER_SDK` directory for C.
- **Documentation moves out of hammer-editor.** The plugin author's parts of `docs/CLIENT-PLUGINS.md`
  (manifest, exports, operations, settings, resources) move here, where they are versioned with the
  kits; hammer-editor keeps the host's design.
- **Templates.** A starter repository per language (`hammer-plugin-template-kotlin`, `-rust`, `-c`)
  with the build, a test, and a CI workflow, for GitHub's "Use this template".

## Order

1. License, GitHub repository, one SDK version, CI.
2. Set up Kotlin publishing on the account and setup that already exist, tried against `mavenLocal`.
3. Set up the Rust crate, tried with `cargo publish --dry-run`. crates.io holds no names in reserve, so
   if `hammer-plugin` is at risk, an early 0.x release claims it.
4. The C tarball and the release workflow that makes the GitHub release.
5. The first release of all three, with Hammer 4.0.
6. Switch hammer-plugins and hammer-plugin-development to the published kits.
7. `cargo-hammer`, the documentation move, and the templates.
