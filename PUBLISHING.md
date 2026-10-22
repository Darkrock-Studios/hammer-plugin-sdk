# Publishing the SDK

The Kotlin kit and its Gradle plugin are published to Maven Central, and the Rust kit to crates.io. The C
kit is not yet: plugin repositories using it build against a checkout of this one beside them. The goal is for a plugin
author to need nothing but the published kit for their language.

## Where things stand

| Language | What ships | Name | Version |
| --- | --- | --- | --- |
| Kotlin | The kit, a Kotlin Multiplatform library (`jvm` and `wasmWasi`), on Maven Central | `com.darkrockstudios.hammer:hammer-plugin-kit` | 0.6.0, in `kotlin/versions.properties` |
| Kotlin | The Gradle plugin, on Maven Central, which adds the kit at its own version | `com.darkrockstudios.hammer.plugin` (artifact `com.darkrockstudios.hammer:hammer-gradle-plugin`) | The kit's |
| Rust | The kit crate, on crates.io, whose library is `hammer` | `hammer-plugin-kit` | The SDK's, in `rust/hammer/Cargo.toml` |
| Rust | `rust/tools/finish.py` | None | None |
| C | The headers in `c/include`, `c/build.sh`, `c/test.sh` | None | None |
| All | `tools/package.sh` | None | None |

Every kit states the host API version it targets (`HAMMER_API_VERSION`, `API_VERSION`), and the Gradle
plugin refuses a manifest whose `api` differs.

## Releasing

Every kit is released together under one version, so a plugin author reads one number, and a host API
change moves every language at once. It's `kitVersion` in `kotlin/versions.properties` and `version` in
`rust/hammer/Cargo.toml`, and CI fails when they differ. The versions
stay 0.x until Hammer ships plugins in a release; after that, a new host API version is a new major
version.

1. Set the version in `kotlin/versions.properties` and `rust/hammer/Cargo.toml`, and merge to `main`
   with CI passing.
2. Tag the commit `v<kitVersion>`, such as `v0.5.0`, and push the tag.
3. `.github/workflows/release.yml` runs CI, checks that the tag matches the version, publishes the Kotlin
   kit and the Gradle plugin to Maven Central (released automatically, so they appear within the hour) and
   the Rust kit to crates.io, and makes a GitHub release with generated notes and `hello.hammerplugin`. A tag with a `-`, such as
   `v0.6.0-rc1`, is marked a prerelease.

The workflow runs in the repository's `Publish` environment, which holds the Central Portal token as
`OSSRH_USERNAME` and `OSSRH_PASSWORD`, and the signing key as `SIGNING_KEY` (ASCII-armored) and
`SIGNING_PASSWORD`, the same account and key ComposeTextEditor publishes with. crates.io needs no
secret: the crate's settings there name this repository's `release.yml`, in the `Publish` environment, as
a trusted publisher, and the workflow trades GitHub's OIDC token for a short-lived one. Trusted publishing
can't create a crate, so 0.6.0 was published by hand with an API token; the workflow skips a version
crates.io already has. To try a release locally,
`./gradlew :hammer-plugin-kit:publishToMavenLocal` and `./gradlew -p gradle-plugin publishToMavenLocal` in
`kotlin/` publish unsigned to `~/.m2`.

## Kotlin

Both artifacts are built with `com.vanniktech.maven.publish`, as ComposeTextEditor's are. Kotlin
Multiplatform publishes the `jvm` and `wasmWasi` variants and the root module, so a plugin's `commonMain`
resolves the right one; the `jvm` variant, which plugins use for tests, also depends on tomlkt, for
`ActionDriver` to read the manifest. The Gradle plugin is published to Central with its plugin marker, so
an author adds `mavenCentral()` to `pluginManagement`'s repositories and writes only:

```kotlin
plugins {
	id("com.darkrockstudios.hammer.plugin") version "0.6.0"
}
```

The Gradle plugin pins the Kotlin Gradle plugin it applies (2.4.10 today). Each SDK release records which
Kotlin version it was tested with, since Hammer is tested against one Kotlin/Wasm output.

## Rust: crates.io

- **The crate** is published as `hammer-plugin-kit`, matching the Kotlin artifact (`hammer` is taken).
  Its library is named `hammer`, so an author depends on `hammer-plugin-kit = "0.6"` and writes
  `use hammer::…`. The `getrandom` feature stays optional. docs.rs builds its documentation.
- **The build.** A Rust plugin needs the `wasm32-unknown-unknown` target, sometimes `finish.py` and
  Wizer, and then packing. At first these ship in the GitHub release (below). Later, a
  `cargo-hammer` subcommand (`cargo install cargo-hammer`, then `cargo hammer package`) replaces them,
  building, finishing, and packing in one step, the way the Gradle plugin does for Kotlin.

## C: GitHub releases

C has no package registry worth targeting, so the C kit is a release asset:
`hammer-plugin-sdk-c-0.6.0.tar.gz` with `include/`, `build.sh`, `test.sh`, `package.sh`, both licenses,
and a SHA-256 checksum next to it. An author unpacks it anywhere and passes its directory to the
scripts, or vendors `include/` into their repository. The headers gain a
`HAMMER_KIT_VERSION` beside `HAMMER_API_VERSION`.

## The same GitHub release

Each `v*` tag makes one GitHub release holding the C tarball, `tools/package.sh` and
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
- **Templates.** A starter repository per language, with the build, a test, and a CI workflow, for
  GitHub's "Use this template". Kotlin's is
  [hammer-plugin-kotlin](https://github.com/Darkrock-Studios/hammer-plugin-kotlin) and Rust's
  [hammer-plugin-rust](https://github.com/Darkrock-Studios/hammer-plugin-rust); `hammer-plugin-c` follows
  once that kit is published.

## Order

The license, the GitHub repository, the one SDK version, CI, and the Kotlin and Rust releases are in place. What's
left, in order:

1. The C tarball, added to the release workflow's GitHub release.
2. Switch hammer-plugins and hammer-plugin-development to the published kits.
3. `cargo-hammer`, the documentation move, and the C template.
