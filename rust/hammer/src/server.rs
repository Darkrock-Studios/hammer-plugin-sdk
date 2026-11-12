//! Requests to a service on the user's own Hammer sync server, such as `languagetool`, through
//! `hammer_server_request`. Hammer sends them as the signed-in user, so the plugin never holds their
//! credentials, and only to the services the manifest names under `permissions.hammerServer`.
//! [languagetool] has typed functions for the `languagetool` service.

pub mod languagetool;

use serde::{Deserialize, Serialize};
use std::collections::BTreeMap;
use std::fmt;

#[link(wasm_import_module = "extism:host/user")]
unsafe extern "C" {
    fn hammer_server_request(request: u64, body: u64) -> u64;
}

pub struct Response {
    pub status: u16,
    pub body: String,
}

impl Response {
    pub fn is_success(&self) -> bool {
        (200..300).contains(&self.status)
    }

    /// What a status other than success means, with the message the sync server gave for the user
    /// when it gave one; None on success.
    pub fn error(&self) -> Option<ServerError> {
        if self.is_success() {
            return None;
        }
        let kind = match self.status {
            429 => ErrorKind::RateLimited,
            400..500 => ErrorKind::Rejected,
            _ => ErrorKind::ServiceFailed,
        };
        let message = display_message(&self.body).unwrap_or_else(|| {
            let detail: String = self.body.chars().take(200).collect();
            match detail.trim().is_empty() {
                true => format!("The service replied {}", self.status),
                false => format!("The service replied {}: {}", self.status, detail.trim()),
            }
        });
        Some(ServerError { kind, message })
    }
}

/// The sync server's own refusals carry a message meant for the user.
fn display_message(body: &str) -> Option<String> {
    let reply: serde_json::Value = serde_json::from_str(body).ok()?;
    reply.get("displayMessage")?.as_str().filter(|it| !it.is_empty()).map(str::to_owned)
}

/// Why a request failed: the first five are from Hammer, before any reply; the rest are what
/// [Response::error] makes of a reply's status.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[non_exhaustive]
pub enum ErrorKind {
    /// No sync account in this workspace, or its session has ended.
    NotSignedIn,
    /// The sync server doesn't offer the service.
    Unavailable,
    /// The sync server couldn't be reached.
    Network,
    /// The manifest doesn't name the service.
    NotPermitted,
    /// A refused method or path, or a reply that isn't text, or isn't what the service should send.
    Invalid,
    /// Too many requests: 429. The sync server allows more after a minute.
    RateLimited,
    /// The service refused the request, such as one too large or in a language it doesn't check: 4xx.
    Rejected,
    /// The service failed, or the sync server couldn't reach it: 5xx.
    ServiceFailed,
}

#[derive(Debug)]
pub struct ServerError {
    pub kind: ErrorKind,
    pub message: String,
}

impl fmt::Display for ServerError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.message)
    }
}

impl std::error::Error for ServerError {}

/// Sends a request to `service`, at `path` under it (such as `/v2/check`), and waits for its response.
/// `method` is `GET` or `POST`; only `Content-Type` and `Accept` headers are sent. A response of any
/// status is Ok.
pub fn request(
    service: &str,
    method: &str,
    path: &str,
    headers: &[(&str, &str)],
    body: Option<&[u8]>,
) -> Result<Response, ServerError> {
    let request = crate::to_kernel(&encode_request(service, method, path, headers));
    let body = body.filter(|it| !it.is_empty()).map_or(0, crate::to_kernel);
    let reply = unsafe { hammer_server_request(request, body) };
    unsafe {
        crate::free(request);
        if body != 0 {
            crate::free(body);
        }
    }
    if reply == 0 {
        return Err(ServerError { kind: ErrorKind::Invalid, message: "The sync server gave no reply".into() });
    }
    let bytes = crate::from_kernel(reply);
    unsafe { crate::free(reply) };
    parse_reply(&bytes)
}

/// The JSON `hammer_server_request` reads: `headers` is an object.
fn encode_request(service: &str, method: &str, path: &str, headers: &[(&str, &str)]) -> Vec<u8> {
    #[derive(Serialize)]
    struct Request<'a> {
        service: &'a str,
        method: &'a str,
        path: &'a str,
        headers: BTreeMap<&'a str, &'a str>,
    }
    let request = Request { service, method, path, headers: headers.iter().copied().collect() };
    serde_json::to_vec(&request).expect("strings always serialize")
}

fn parse_reply(bytes: &[u8]) -> Result<Response, ServerError> {
    #[derive(Deserialize)]
    struct Reply {
        status: Option<u16>,
        body: Option<String>,
        error: Option<String>,
        message: Option<String>,
    }
    let reply: Reply = serde_json::from_slice(bytes).map_err(|e| ServerError { kind: ErrorKind::Invalid, message: e.to_string() })?;
    if let Some(status) = reply.status {
        return Ok(Response { status, body: reply.body.unwrap_or_default() });
    }
    let kind = match reply.error.as_deref() {
        Some("not_signed_in") => ErrorKind::NotSignedIn,
        Some("unavailable") => ErrorKind::Unavailable,
        Some("network") => ErrorKind::Network,
        Some("not_permitted") => ErrorKind::NotPermitted,
        _ => ErrorKind::Invalid,
    };
    Err(ServerError { kind, message: reply.message.unwrap_or_else(|| "The sync server gave no reply".into()) })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_request_names_its_headers_in_an_object() {
        let json = encode_request("languagetool", "POST", "/v2/check", &[("Content-Type", "application/x-www-form-urlencoded")]);
        assert_eq!(
            String::from_utf8(json).unwrap(),
            r#"{"service":"languagetool","method":"POST","path":"/v2/check","headers":{"Content-Type":"application/x-www-form-urlencoded"}}"#
        );
    }

    #[test]
    fn a_status_is_a_response() {
        let response = parse_reply(br#"{"status":200,"body":"{\"matches\":[]}"}"#).unwrap();
        assert_eq!(response.status, 200);
        assert_eq!(response.body, r#"{"matches":[]}"#);
        assert!(response.is_success());
    }

    #[test]
    fn an_error_keeps_its_kind_and_message() {
        let error = parse_reply(br#"{"error":"not_signed_in","message":"Sign in"}"#).err().unwrap();
        assert_eq!(error.kind, ErrorKind::NotSignedIn);
        assert_eq!(error.to_string(), "Sign in");
    }

    #[test]
    fn a_status_names_its_error_with_the_servers_message() {
        let response = |status, body: &str| Response { status, body: body.into() };
        assert!(response(200, "").error().is_none());
        assert_eq!(response(429, "").error().unwrap().kind, ErrorKind::RateLimited);
        let refused = response(413, r#"{"error":"api_services_error_toolarge","displayMessage":"Too large"}"#).error().unwrap();
        assert_eq!((refused.kind, refused.message.as_str()), (ErrorKind::Rejected, "Too large"));
        let failed = response(502, "").error().unwrap();
        assert_eq!((failed.kind, failed.message.as_str()), (ErrorKind::ServiceFailed, "The service replied 502"));
        assert_eq!(response(400, "Bad language").error().unwrap().message, "The service replied 400: Bad language");
    }

    #[test]
    fn an_unknown_error_is_invalid() {
        let error = parse_reply(br#"{"error":"something_new"}"#).err().unwrap();
        assert_eq!(error.kind, ErrorKind::Invalid);
    }
}
