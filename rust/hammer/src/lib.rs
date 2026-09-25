//! Hammer's plugin kit for Rust. See hammer-editor's docs/CLIENT-PLUGINS.md for the exports a plugin
//! provides (`export`, `action`, `command`) and the JSON each one is given.
//!
//! An export is a `#[unsafe(no_mangle)] pub extern "C" fn` returning `i32`, whose body is [run]:
//!
//! ```ignore
//! #[unsafe(no_mangle)]
//! pub extern "C" fn action() -> i32 {
//!     hammer::run(|| {
//!         let request: Request = hammer::input_json()?;
//!         Ok(format!("Hello, {}", request.project))
//!     })
//! }
//! ```

use serde::Serialize;
use serde::de::DeserializeOwned;
use std::sync::Once;

/// The host API version this kit targets; the manifest's `api` must match.
pub const API_VERSION: u32 = 1;

pub const CACHE_MAX_KEY: usize = 1024;
pub const CACHE_MAX_VALUE: usize = 1024 * 1024;

// Extism's kernel keeps the call's input, output, and any memory passed to or from Hammer. Offsets are
// into the kernel's memory, not the module's.
#[link(wasm_import_module = "extism:host/env")]
unsafe extern "C" {
    fn input_length() -> u64;
    fn input_load_u8(index: u64) -> u8;
    fn input_load_u64(index: u64) -> u64;
    fn alloc(length: u64) -> u64;
    fn free(offset: u64);
    fn length(offset: u64) -> u64;
    fn load_u8(offset: u64) -> u8;
    fn load_u64(offset: u64) -> u64;
    fn store_u8(offset: u64, value: u8);
    fn store_u64(offset: u64, value: u64);
    fn output_set(offset: u64, length: u64);
    fn error_set(offset: u64);
    fn log_debug(offset: u64);
    fn log_info(offset: u64);
    fn log_warn(offset: u64);
    fn log_error(offset: u64);
}

#[link(wasm_import_module = "extism:host/user")]
unsafe extern "C" {
    fn hammer_dispatch(request: u64) -> u64;
    fn hammer_cache_get(key: u64) -> u64;
    fn hammer_cache_set(key: u64, value: u64);
}

fn to_kernel(bytes: &[u8]) -> u64 {
    unsafe {
        let offset = alloc(bytes.len() as u64);
        let mut chunks = bytes.chunks_exact(8);
        let mut at = offset;
        for chunk in &mut chunks {
            store_u64(at, u64::from_le_bytes(chunk.try_into().unwrap()));
            at += 8;
        }
        for &byte in chunks.remainder() {
            store_u8(at, byte);
            at += 1;
        }
        offset
    }
}

fn from_kernel(offset: u64) -> Vec<u8> {
    unsafe {
        let size = length(offset) as usize;
        let mut bytes = Vec::with_capacity(size);
        let mut at = 0;
        while at + 8 <= size {
            bytes.extend_from_slice(&load_u64(offset + at as u64).to_le_bytes());
            at += 8;
        }
        while at < size {
            bytes.push(load_u8(offset + at as u64));
            at += 1;
        }
        bytes
    }
}

/// A failure to report to the user. `?` converts any error into one, and `.into()` a message.
pub type Error = Box<dyn std::error::Error>;

pub type Result<T> = std::result::Result<T, Error>;

/// Runs an export's body: its output on success, its message on failure, and a panic's message too.
pub fn run<O: AsRef<[u8]>>(body: impl FnOnce() -> Result<O>) -> i32 {
    static PANIC_HOOK: Once = Once::new();
    // A panic aborts the call with a trap; the message set here is what the user sees.
    PANIC_HOOK.call_once(|| std::panic::set_hook(Box::new(|info| set_error(&info.to_string()))));
    match body() {
        Ok(output) => {
            let output = output.as_ref();
            unsafe { output_set(to_kernel(output), output.len() as u64) };
            0
        }
        Err(error) => {
            set_error(&error.to_string());
            1
        }
    }
}

fn set_error(message: &str) {
    unsafe { error_set(to_kernel(message.as_bytes())) };
}

pub fn input() -> Vec<u8> {
    unsafe {
        let size = input_length() as usize;
        let mut bytes = Vec::with_capacity(size);
        let mut at = 0;
        while at + 8 <= size {
            bytes.extend_from_slice(&input_load_u64(at as u64).to_le_bytes());
            at += 8;
        }
        while at < size {
            bytes.push(input_load_u8(at as u64));
            at += 1;
        }
        bytes
    }
}

pub fn input_json<T: DeserializeOwned>() -> Result<T> {
    Ok(serde_json::from_slice(&input())?)
}

#[derive(serde::Deserialize)]
struct Reply {
    #[serde(default)]
    output: serde_json::Value,
    error: Option<ReplyError>,
}

#[derive(serde::Deserialize)]
struct ReplyError {
    kind: String,
    #[serde(default)]
    message: String,
}

/// Runs a Hammer operation the manifest has permission for, such as
/// `call::<Tree>("scene.tree", &json!({"project": name}))`. A failure's message starts with its kind:
/// NotFound, InvalidInput, PermissionDenied, or Failed.
pub fn call<T: DeserializeOwned>(operation: &str, input: &impl Serialize) -> Result<T> {
    #[derive(Serialize)]
    struct Request<'a, I> {
        operation: &'a str,
        input: &'a I,
    }
    let request = to_kernel(&serde_json::to_vec(&Request { operation, input })?);
    let reply = unsafe { hammer_dispatch(request) };
    unsafe { free(request) };
    let bytes = from_kernel(reply);
    unsafe { free(reply) };
    let reply: Reply = serde_json::from_slice(&bytes)?;
    match reply.error {
        Some(error) => Err(format!("{}: {}", error.kind, error.message).into()),
        // An output of null is still a success.
        None => Ok(serde_json::from_value(reply.output)?),
    }
}

// The plugin's own cache, kept by Hammer between calls and runs. Hammer may drop any value at any
// time, and clears them all when the plugin is installed again or uninstalled. A call still running on
// the old version during an install may write after the clear, so put a version in keys whose values'
// format may change.

/// The value cached under `key`, or None when there is none.
pub fn cache_get(key: &str) -> Option<Vec<u8>> {
    if key.len() > CACHE_MAX_KEY {
        return None;
    }
    let key = to_kernel(key.as_bytes());
    let value = unsafe { hammer_cache_get(key) };
    unsafe { free(key) };
    if value == 0 {
        return None;
    }
    let bytes = from_kernel(value);
    unsafe { free(value) };
    Some(bytes)
}

/// Caches `value` under `key`; an empty value removes it. False, storing nothing, when either is over
/// its limit.
pub fn cache_set(key: &str, value: &[u8]) -> bool {
    if key.len() > CACHE_MAX_KEY || value.len() > CACHE_MAX_VALUE {
        return false;
    }
    let key = to_kernel(key.as_bytes());
    let value = if value.is_empty() { 0 } else { to_kernel(value) };
    unsafe {
        hammer_cache_set(key, value);
        free(key);
        if value != 0 {
            free(value);
        }
    }
    true
}

pub mod log {
    fn send(to: unsafe extern "C" fn(u64), message: &str) {
        let handle = super::to_kernel(message.as_bytes());
        unsafe {
            to(handle);
            super::free(handle);
        }
    }

    pub fn debug(message: &str) {
        send(super::log_debug, message)
    }

    pub fn info(message: &str) {
        send(super::log_info, message)
    }

    pub fn warn(message: &str) {
        send(super::log_warn, message)
    }

    pub fn error(message: &str) {
        send(super::log_error, message)
    }
}
