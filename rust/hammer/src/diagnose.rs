//! The `diagnose` export: Hammer sends paragraphs of a scene, and the plugin replies with the issues
//! in them, which the editor underlines. A check's manifest `scope` says which paragraphs: `paragraph`,
//! the default, sends only those whose text the check has not seen; `scene` sends every paragraph of
//! the scene each time, blank ones included, for issues that depend on other paragraphs.

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
    /// The name of the project the text belongs to, for operations such as `entry.list`.
    pub project: String,
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
    /// Offered to the user.
    pub fixes: Vec<Fix>,
    #[serde(skip_serializing_if = "Severity::is_error")]
    pub severity: Severity,
}

#[derive(Serialize, Clone, Copy, PartialEq, Eq, Default, Debug)]
#[serde(rename_all = "lowercase")]
pub enum Severity {
    /// A mistake: a wavy underline.
    #[default]
    Error,
    /// Something the writer may want to change: a quieter, dotted underline.
    Suggestion,
}

impl Severity {
    fn is_error(&self) -> bool {
        *self == Severity::Error
    }
}

/// A replacement for a [Diagnostic]'s range, shown to the user as itself, or as a label where the
/// replacement alone would not say what it does, such as removing a word.
#[derive(Serialize)]
#[serde(untagged)]
pub enum Fix {
    Replace(String),
    Labelled { replacement: String, label: String },
}
