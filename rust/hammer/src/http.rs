//! HTTP requests, through Extism's `http_request`. Hammer allows only the hosts the manifest names
//! under `permissions.http`, which the user approves at install; a request to any other fails.

use serde::Serialize;
use std::collections::BTreeMap;

#[link(wasm_import_module = "extism:host/env")]
unsafe extern "C" {
    fn http_request(request: u64, body: u64) -> u64;
    fn http_status_code() -> i32;
}

pub struct Response {
    pub status: u16,
    pub body: Vec<u8>,
}

impl Response {
    pub fn is_success(&self) -> bool {
        (200..300).contains(&self.status)
    }
}

/// Sends a request and waits for its response. A response of any status is Ok; Err means there was
/// none, such as for a host the manifest does not name or a network that is down.
pub fn request(method: &str, url: &str, headers: &[(&str, &str)], body: Option<&[u8]>) -> crate::Result<Response> {
    #[derive(Serialize)]
    struct Request<'a> {
        url: &'a str,
        method: &'a str,
        headers: BTreeMap<&'a str, &'a str>,
    }
    let request = Request { url, method, headers: headers.iter().copied().collect() };
    let request = crate::to_kernel(&serde_json::to_vec(&request)?);
    let body = body.filter(|it| !it.is_empty()).map_or(0, crate::to_kernel);
    let reply = unsafe { http_request(request, body) };
    let status = unsafe { http_status_code() };
    unsafe {
        crate::free(request);
        if body != 0 {
            crate::free(body);
        }
    }
    let body = if reply == 0 { Vec::new() } else { crate::from_kernel(reply) };
    if reply != 0 {
        unsafe { crate::free(reply) };
    }
    match u16::try_from(status) {
        Ok(status) if status > 0 => Ok(Response { status, body }),
        _ => Err(format!("No response from {url}").into()),
    }
}

/// `application/x-www-form-urlencoded` form data from its fields.
pub fn form(fields: &[(&str, &str)]) -> String {
    fields.iter().map(|(name, value)| format!("{}={}", encode(name), encode(value))).collect::<Vec<_>>().join("&")
}

fn encode(text: &str) -> String {
    let mut encoded = String::with_capacity(text.len());
    for byte in text.bytes() {
        match byte {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'.' | b'_' | b'*' => encoded.push(byte as char),
            b' ' => encoded.push('+'),
            _ => encoded.push_str(&format!("%{byte:02X}")),
        }
    }
    encoded
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn form_encodes_reserved_and_non_ascii() {
        assert_eq!(form(&[("text", "a b&c=d"), ("x", "é")]), "text=a+b%26c%3Dd&x=%C3%A9");
    }
}
