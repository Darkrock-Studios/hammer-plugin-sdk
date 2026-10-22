//! The generated `ops` types read and write the JSON Hammer's operations use.

use crate::ops::*;
use serde_json::json;

#[test]
fn outputs_read_hammer_json() {
    let scene: SceneText = serde_json::from_value(json!({
        "id": 7, "name": "Storm", "archived": false, "markdown": "Rain fell.", "wordCount": 2, "extra": 1,
        "meta": {
            "outline": "", "notes": "", "tags": ["draft"], "currentDraft": "", "created": null,
            "lastEdited": "2026-03-01T09:30:00Z", "references": [3], "dismissedReferences": []
        }
    }))
    .unwrap();
    assert_eq!((scene.word_count, scene.meta.references, scene.meta.created), (2, vec![3], None));
}

#[test]
fn options_leave_out_what_is_unset() {
    let options = scene::meta::SceneMetaWriteOptions { tags: Some(vec!["draft".into()]), ..Default::default() };
    assert_eq!(serde_json::to_value(options).unwrap(), json!({"tags": ["draft"]}));
}

#[test]
fn enums_and_bytes_use_hammer_names() {
    assert_eq!(serde_json::to_value(EntryKind::Person).unwrap(), json!("person"));
    let file: ExportedFile =
        serde_json::from_value(json!({"fileName": "a.txt", "mimeType": "text/plain", "content": "Zm9vYmFy"})).unwrap();
    assert_eq!(file.content, b"foobar");
}

#[test]
fn unknown_enum_values_read_as_unknown() {
    let kinds: Vec<EntryKind> = serde_json::from_value(json!(["person", "organization"])).unwrap();
    assert_eq!(kinds, vec![EntryKind::Person, EntryKind::Unknown]);
}
