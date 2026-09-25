//! The `action` export: an item on the menus of the screens in the manifest entry's `places`. Hammer
//! asks for any `[[actions.field]]`s first, then calls it; what it returns is shown as the entry's
//! `output` says. An `interactive` action returns a [Reply] as JSON.

use serde::de::DeserializeOwned;
use serde::{Deserialize, Serialize};
use serde_json::{Map, Value};

#[derive(Deserialize)]
pub struct Request {
    /// Which of the manifest's `[[actions]]` to run.
    pub action: String,
    pub project: String,
    /// The plugin's declared settings, every key present.
    #[serde(default)]
    pub settings: Map<String, Value>,
    /// A value for each of the action's `[[actions.field]]`s.
    #[serde(default)]
    pub input: Map<String, Value>,
    #[serde(default)]
    pub context: Context,
    /// The id of the button pressed, for an interactive action's later calls; none on the first.
    #[serde(default)]
    pub button: Option<String>,
    /// The UI's BCP 47 tag, for replying in the user's language; not the project's language.
    #[serde(default)]
    pub locale: Option<String>,
}

impl Request {
    /// The input read into a `T`, with a field per `[[actions.field]]`.
    pub fn input<T: DeserializeOwned>(&self) -> crate::Result<T> {
        Ok(serde_json::from_value(Value::Object(self.input.clone()))?)
    }
}

/// The screen an action was run from, and the id of the item it shows there (none on Project Home).
#[derive(Deserialize)]
pub struct Context {
    /// `project`, `scene`, `note`, `entry`, or `event`.
    pub place: String,
    #[serde(default)]
    pub id: Option<i64>,
}

impl Default for Context {
    fn default() -> Self {
        Context { place: "project".into(), id: None }
    }
}

/// An `interactive` action's output: markdown shown with buttons under it, each calling the action
/// again with its id as [Request::button], and a message shown briefly. No markdown closes the dialog.
#[derive(Serialize, Default)]
pub struct Reply {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub markdown: Option<String>,
    pub buttons: Vec<Button>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
}

#[derive(Serialize)]
pub struct Button {
    pub id: String,
    pub label: String,
}

impl Reply {
    /// What the `action` export returns for this reply.
    pub fn to_json(&self) -> crate::Result<String> {
        Ok(serde_json::to_string(self)?)
    }
}
