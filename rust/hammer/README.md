# hammer-plugin-kit

The Rust kit for writing plugins for [Hammer](https://hammer.ink), the novel writing app. A plugin is a
WebAssembly module that Hammer runs in a sandbox; this crate gives it Hammer's imports, a typed function
for each of Hammer's operations (`hammer::ops`), and the types for exports, actions, and text checks.

```toml
[lib]
crate-type = ["cdylib"]

[dependencies]
hammer-plugin-kit = "0.7"
```

```rust
use hammer::ops;

let scene = ops::scene::read(&project, id)?;
for image_id in hammer::images::find_ids(&scene.markdown) {
    let image = ops::image::get(&project, image_id)?;
    hammer::log::info(&format!("{}.{}: {} bytes", image.name, image.extension, image.content.len()));
}
```

`hammer::images` reads the `hammer://image/<id>` references Hammer writes for a project's images.

Build for `wasm32-unknown-unknown`. The [Rust template](https://github.com/Darkrock-Studios/hammer-plugin-rust)
is a working plugin to start from, and the
[SDK](https://github.com/Darkrock-Studios/hammer-plugin-sdk) has the recipes and the Kotlin and C kits.
