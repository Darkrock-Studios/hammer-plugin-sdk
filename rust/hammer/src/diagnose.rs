//! The `diagnose` export: Hammer sends the paragraphs that changed, and the plugin replies with the
//! issues in them, which the editor underlines.

use serde::{Deserialize, Serialize};
use serde_json::{Map, Value};

#[derive(Deserialize)]
pub struct Request {
    /// Which of the manifest's `[[diagnostics]]` checks to run.
    pub diagnostics: String,
    /// Plain text, without markdown.
    pub paragraphs: Vec<String>,
    /// The project's BCP 47 tag, or none.
    pub language: Option<String>,
    /// The plugin's declared settings, every key present.
    #[serde(default)]
    pub settings: Map<String, Value>,
}

#[derive(Serialize, Default)]
pub struct Reply {
    pub diagnostics: Vec<Diagnostic>,
}

/// An issue in one paragraph, from `start` to `end` as UTF-8 byte offsets into it.
#[derive(Serialize)]
pub struct Diagnostic {
    /// Its index in [Request::paragraphs].
    pub paragraph: usize,
    pub start: usize,
    pub end: usize,
    pub message: String,
    /// Replacements for the range, offered to the user.
    pub fixes: Vec<String>,
}
