//! The `action` export: an item on the menus of the screens in the manifest entry's `places`. Hammer
//! asks for any `[[actions.field]]`s first, then calls it, and every call returns a [Reply] as JSON.
//! A reply with a [Page] shows it in the action's dialog; pressing one of the page's buttons calls the
//! action again with the button's id, the page's values as [Request::input], and the page's `state`.
//! A reply with `errors` keeps the user on the page they were on, one with `open` ends the action and
//! goes to an item, and one with none of these ends the action. A `message` is shown briefly either way.
//!
//! ```ignore
//! let page = Page {
//!     title: Some("Who?".into()),
//!     fields: vec![Field::text("name", "Name").required()],
//!     buttons: vec![Button::new("back", "Back").skip_checks(), Button::new("next", "Next").primary()],
//!     ..Page::default()
//! }
//! .with_state(&Step { step: 2 })?;
//! Reply::page(page).to_json()
//! ```

use serde::de::DeserializeOwned;
use serde::{Deserialize, Serialize};
use serde_json::{Map, Value};
use std::collections::BTreeMap;

#[derive(Deserialize)]
pub struct Request {
    /// Which of the manifest's `[[actions]]` to run.
    pub action: String,
    pub project: String,
    /// The plugin's declared settings, every key present.
    #[serde(default)]
    pub settings: Map<String, Value>,
    /// A value for each field of the page the user was on; on the first call, the action's
    /// `[[actions.field]]`s.
    #[serde(default)]
    pub input: Map<String, Value>,
    #[serde(default)]
    pub context: Context,
    /// The id of the button pressed; none on the first call, which opens the action or runs the
    /// manifest's fields.
    #[serde(default)]
    pub button: Option<String>,
    /// Reserved for fields that call the action when they change; always none.
    #[serde(default)]
    pub changed: Option<String>,
    /// The last page's `state`, as it was written; none on the first call.
    #[serde(default)]
    pub state: Option<Value>,
    /// Set on the first call after Hammer restored a run the user had left.
    #[serde(default)]
    pub resumed: bool,
    /// The UI's BCP 47 tag, for replying in the user's language; not the project's language.
    #[serde(default)]
    pub locale: Option<String>,
}

impl Request {
    /// The input read into a `T`, with a field per field of the page.
    pub fn input<T: DeserializeOwned>(&self) -> crate::Result<T> {
        Ok(serde_json::from_value(Value::Object(self.input.clone()))?)
    }

    /// The state read into a `T`: none when there is none, or it is not a `T`.
    pub fn state<T: DeserializeOwned>(&self) -> Option<T> {
        self.state.as_ref().and_then(|state| T::deserialize(state).ok())
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

/// What an `action` export returns, as [Reply::to_json]. Every part is optional; the default ends the
/// action without a word.
#[derive(Serialize, Default)]
pub struct Reply {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub page: Option<Page>,
    /// Shown briefly, whatever else the reply does.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Messages by field key, shown under each field, keeping the user on the page they were on with
    /// what they entered; a key of `""`, or one that is no field's, is shown for the whole page.
    #[serde(skip_serializing_if = "BTreeMap::is_empty")]
    pub errors: BTreeMap<String, String>,
    /// An item to go to, ending the action.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub open: Option<Open>,
}

impl Reply {
    /// Ends the action, showing `message` briefly.
    pub fn message(message: impl Into<String>) -> Reply {
        Reply { message: Some(message.into()), ..Reply::default() }
    }

    /// Shows `markdown` alone, a report the user can copy or save as a note.
    pub fn document(markdown: impl Into<String>) -> Reply {
        Reply::page(Page { markdown: Some(markdown.into()), ..Page::default() })
    }

    pub fn page(page: Page) -> Reply {
        Reply { page: Some(page), ..Reply::default() }
    }

    /// Keeps the user on the page, showing `message` under the field `key`, or for the whole page when
    /// `key` is `""`.
    pub fn error(key: impl Into<String>, message: impl Into<String>) -> Reply {
        Reply { errors: BTreeMap::from([(key.into(), message.into())]), ..Reply::default() }
    }

    /// Ends the action and goes to an item.
    pub fn open(open: Open) -> Reply {
        Reply { open: Some(open), ..Reply::default() }
    }

    /// What the `action` export returns for this reply.
    pub fn to_json(&self) -> crate::Result<String> {
        Ok(serde_json::to_string(self)?)
    }
}

/// What the action's dialog shows: a title, markdown, the fields in order, and buttons.
#[derive(Serialize, Default)]
pub struct Page {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub markdown: Option<String>,
    #[serde(skip_serializing_if = "Vec::is_empty")]
    pub fields: Vec<Field>,
    #[serde(skip_serializing_if = "Vec::is_empty")]
    pub buttons: Vec<Button>,
    /// Handed back untouched as the next call's [Request::state]. Hammer never reads it, and fails a
    /// call whose state is over 1 MiB as JSON.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub state: Option<Value>,
}

impl Page {
    /// The page with `state` as its state, for [Request::state] to read back.
    pub fn with_state(mut self, state: &impl Serialize) -> crate::Result<Page> {
        self.state = Some(serde_json::to_value(state)?);
        Ok(self)
    }
}

/// A field of a page. Its value comes back in the next call's input under its key: a string for
/// `text` and `choice`, a bool, an integer, or for `scenes` a list of scene ids (one id or null when
/// [Field::single]). Hammer checks `required`, an `int`'s bounds, and a `choice`'s options before
/// calling.
#[derive(Serialize, Clone)]
pub struct Field {
    pub key: String,
    #[serde(rename = "type")]
    pub kind: FieldKind,
    pub label: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub hint: Option<String>,
    /// For `text`, not blank; for `scenes`, not empty.
    #[serde(skip_serializing_if = "std::ops::Not::not")]
    pub required: bool,
    /// The initial value; none for the kind's own: false, `min` or 0, the first option, "", or none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub min: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max: Option<i64>,
    #[serde(skip_serializing_if = "Vec::is_empty")]
    pub options: Vec<Choice>,
    #[serde(skip_serializing_if = "TextFormat::is_plain")]
    pub format: TextFormat,
    #[serde(skip_serializing_if = "std::ops::Not::not")]
    pub multiline: bool,
    #[serde(skip_serializing_if = "is_true")]
    pub multiple: bool,
}

#[derive(Serialize, Clone, Copy, PartialEq, Eq, Debug)]
#[serde(rename_all = "lowercase")]
pub enum FieldKind {
    Text,
    Bool,
    Int,
    Choice,
    Scenes,
}

/// What edits a `text` field: an ordinary text field, or the scene editor's, whose value is markdown,
/// with its format bar or the extended one (highlight, task lists, tables).
#[derive(Serialize, Clone, Copy, PartialEq, Eq, Default, Debug)]
#[serde(rename_all = "kebab-case")]
pub enum TextFormat {
    #[default]
    Plain,
    Rich,
    RichExtended,
}

impl TextFormat {
    fn is_plain(&self) -> bool {
        *self == TextFormat::Plain
    }
}

/// One of a `choice` field's options: `value` is what comes back in the input, `label` what is shown.
#[derive(Serialize, Clone)]
pub struct Choice {
    pub value: String,
    pub label: String,
}

impl Choice {
    pub fn new(value: impl Into<String>, label: impl Into<String>) -> Choice {
        Choice { value: value.into(), label: label.into() }
    }
}

impl Field {
    fn new(key: impl Into<String>, kind: FieldKind, label: impl Into<String>) -> Field {
        Field {
            key: key.into(),
            kind,
            label: label.into(),
            hint: None,
            required: false,
            default: None,
            min: None,
            max: None,
            options: Vec::new(),
            format: TextFormat::Plain,
            multiline: false,
            multiple: true,
        }
    }

    /// A text field, plain unless given a [Field::format].
    pub fn text(key: impl Into<String>, label: impl Into<String>) -> Field {
        Field::new(key, FieldKind::Text, label)
    }

    pub fn bool(key: impl Into<String>, label: impl Into<String>) -> Field {
        Field::new(key, FieldKind::Bool, label)
    }

    pub fn int(key: impl Into<String>, label: impl Into<String>, min: Option<i64>, max: Option<i64>) -> Field {
        Field { min, max, ..Field::new(key, FieldKind::Int, label) }
    }

    /// A choice of `options`, at least one.
    pub fn choice(key: impl Into<String>, label: impl Into<String>, options: Vec<Choice>) -> Field {
        Field { options, ..Field::new(key, FieldKind::Choice, label) }
    }

    /// Scenes picked from the project's tree: a list, or one with [Field::single].
    pub fn scenes(key: impl Into<String>, label: impl Into<String>) -> Field {
        Field::new(key, FieldKind::Scenes, label)
    }

    pub fn hint(mut self, hint: impl Into<String>) -> Field {
        self.hint = Some(hint.into());
        self
    }

    pub fn required(mut self) -> Field {
        self.required = true;
        self
    }

    /// The initial value, such as `"elvish"`, `8`, `true`, or `vec![3, 4]`.
    pub fn default(mut self, value: impl Into<Value>) -> Field {
        self.default = Some(value.into());
        self
    }

    pub fn format(mut self, format: TextFormat) -> Field {
        self.format = format;
        self
    }

    /// A plain text field of several lines.
    pub fn multiline(mut self) -> Field {
        self.multiline = true;
        self
    }

    /// Scenes as one scene, or null, instead of a list.
    pub fn single(mut self) -> Field {
        self.multiple = false;
        self
    }
}

/// A button of a page. Pressing it calls the action again with its id as [Request::button]. It sits in
/// the dialog's footer, or with `inline` under the markdown, for acting on something in it.
#[derive(Serialize, Clone)]
pub struct Button {
    pub id: String,
    pub label: String,
    #[serde(skip_serializing_if = "std::ops::Not::not")]
    pub primary: bool,
    #[serde(skip_serializing_if = "std::ops::Not::not")]
    pub inline: bool,
    /// Whether Hammer checks the page's fields before calling.
    #[serde(skip_serializing_if = "is_true")]
    pub validate: bool,
}

impl Button {
    pub fn new(id: impl Into<String>, label: impl Into<String>) -> Button {
        Button { id: id.into(), label: label.into(), primary: false, inline: false, validate: true }
    }

    pub fn primary(mut self) -> Button {
        self.primary = true;
        self
    }

    pub fn inline(mut self) -> Button {
        self.inline = true;
        self
    }

    /// Calls the action without Hammer's checks of the fields, as Back or Cancel should.
    pub fn skip_checks(mut self) -> Button {
        self.validate = false;
        self
    }
}

/// An item for Hammer to go to: `place` is `project`, `scene`, `note`, `entry`, or `event`, and `id`
/// the item's (none for `project`). An id that does not exist fails.
#[derive(Serialize, Clone)]
pub struct Open {
    pub place: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<i64>,
}

impl Open {
    pub fn new(place: impl Into<String>, id: Option<i64>) -> Open {
        Open { place: place.into(), id }
    }
}

fn is_true(value: &bool) -> bool {
    *value
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn replies_leave_out_what_is_unset() {
        assert_eq!(Reply::default().to_json().unwrap(), "{}");
        assert_eq!(Reply::message("Hi").to_json().unwrap(), r#"{"message":"Hi"}"#);
        assert_eq!(Reply::document("# Hi").to_json().unwrap(), r##"{"page":{"markdown":"# Hi"}}"##);
        assert_eq!(Reply::error("", "No").to_json().unwrap(), r#"{"errors":{"":"No"}}"#);
        let open = Reply { message: Some("Added".into()), ..Reply::open(Open::new("entry", Some(12))) };
        assert_eq!(open.to_json().unwrap(), r#"{"message":"Added","open":{"place":"entry","id":12}}"#);
    }

    #[test]
    fn a_page_has_its_fields_buttons_and_state() {
        #[derive(Serialize)]
        struct Step {
            step: u32,
        }
        let page = Page {
            title: Some("Who?".into()),
            fields: vec![
                Field::text("name", "Name").required(),
                Field::text("want", "Wants").format(TextFormat::RichExtended).hint("A line"),
                Field::text("notes", "Notes").multiline(),
                Field::choice("set", "Set", vec![Choice::new("quick", "Quick")]).default("quick"),
                Field::int("count", "Count", Some(1), None).default(8),
                Field::bool("all", "All"),
                Field::scenes("scene", "Scene").single(),
            ],
            buttons: vec![Button::new("back", "Back").skip_checks(), Button::new("next", "Next").primary().inline()],
            ..Page::default()
        }
        .with_state(&Step { step: 2 })
        .unwrap();
        assert_eq!(
            serde_json::to_value(Reply::page(page)).unwrap(),
            json!({"page": {
                "title": "Who?",
                "fields": [
                    {"key": "name", "type": "text", "label": "Name", "required": true},
                    {"key": "want", "type": "text", "label": "Wants", "hint": "A line", "format": "rich-extended"},
                    {"key": "notes", "type": "text", "label": "Notes", "multiline": true},
                    {"key": "set", "type": "choice", "label": "Set", "default": "quick",
                     "options": [{"value": "quick", "label": "Quick"}]},
                    {"key": "count", "type": "int", "label": "Count", "default": 8, "min": 1},
                    {"key": "all", "type": "bool", "label": "All"},
                    {"key": "scene", "type": "scenes", "label": "Scene", "multiple": false},
                ],
                "buttons": [
                    {"id": "back", "label": "Back", "validate": false},
                    {"id": "next", "label": "Next", "primary": true, "inline": true},
                ],
                "state": {"step": 2},
            }})
        );
    }

    #[test]
    fn a_request_carries_state() {
        #[derive(Deserialize)]
        struct Step {
            step: u32,
        }
        let request: Request = serde_json::from_value(json!({
            "action": "wave", "project": "Storm", "settings": {}, "locale": null,
            "context": {"place": "project", "id": null}, "input": {}, "button": "next",
            "changed": null, "state": {"step": 2}, "resumed": true,
        }))
        .unwrap();
        assert_eq!(request.state::<Step>().map(|it| it.step), Some(2));
        assert!(request.state::<String>().is_none());
        assert!(request.resumed);
        let first: Request = serde_json::from_value(json!({"action": "wave", "project": "Storm", "state": null})).unwrap();
        assert!(first.state.is_none() && !first.resumed && first.button.is_none());
    }
}
