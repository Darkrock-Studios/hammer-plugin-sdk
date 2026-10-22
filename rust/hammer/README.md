# hammer-plugin-kit

The Rust kit for writing plugins for [Hammer](https://hammer.ink), the novel writing app. A plugin is a
WebAssembly module that Hammer runs in a sandbox; this crate gives it Hammer's imports, a typed function
for each of Hammer's operations (`hammer::ops`), and the types for actions and text checks.

```toml
[lib]
crate-type = ["cdylib"]

[dependencies]
hammer-plugin-kit = "0.5"
```

```rust
use hammer::ops;

let scene = ops::scene::read(&project, id)?;
```

Build for `wasm32-unknown-unknown`. The [Rust template](https://github.com/Darkrock-Studios/hammer-plugin-rust)
is a working plugin to start from, and the
[SDK](https://github.com/Darkrock-Studios/hammer-plugin-sdk) has the recipes and the Kotlin and C kits.
