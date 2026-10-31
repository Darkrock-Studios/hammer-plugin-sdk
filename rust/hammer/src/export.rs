//! The `export` export: renders one of the manifest's `[[exporters]]` formats from the story, and
//! returns the file's bytes.
//!
//! An exporter whose manifest entry sets `images = true` gets the files of the images the story shows
//! in [Request::images], and with `cover = true` as well, the project's cover. In markdown, an image
//! is `![image](hammer://image/<id>)`, which [crate::images] reads; in prose, a line holding only an
//! image is a block `{"type": "image", "id": 5, "name": "Map"}`, which [prose_image_id] reads. An image
//! inside a line, or one whose file Hammer could not read, is its name as text, never a block.
//!
//! ```ignore
//! let request: hammer::export::Request = hammer::input_json()?;
//! for scene in request.chapters.iter().flat_map(|chapter| &chapter.scenes) {
//!     for id in hammer::images::find_ids(scene) {
//!         if let Some(image) = request.image(id) { /* image.content holds the file */ }
//!     }
//! }
//! ```

use serde::Deserialize;
use serde_json::{Map, Value};

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Request {
    /// Which of the manifest's `[[exporters]]` formats to render.
    pub format: String,
    pub project_name: String,
    /// The project's BCP 47 tag.
    #[serde(default = "english")]
    pub language: String,
    /// Whether the scene tree's top-level nodes are chapters; when not, the story is one chapter.
    #[serde(default = "yes")]
    pub top_level_as_chapters: bool,
    #[serde(default)]
    pub chapters: Vec<Chapter>,
    /// The plugin's declared settings, every key present.
    #[serde(default)]
    pub settings: Map<String, Value>,
    /// The UI's BCP 47 tag, for replying in the user's language; not [Request::language], the story's.
    #[serde(default)]
    pub locale: Option<String>,
    /// For a format whose manifest entry sets `images = true`, each project image the story refers to
    /// whose file Hammer could read, sorted by id, and the cover with `cover = true`; empty otherwise.
    #[serde(default)]
    pub images: Vec<Image>,
    /// The cover's id among [Request::images], with `cover = true`; none when the project has no cover
    /// or its file could not be read.
    #[serde(default)]
    pub cover_image_id: Option<i32>,
}

impl Request {
    /// The image `id` among [Request::images], or none when it is not there.
    pub fn image(&self, id: i32) -> Option<&Image> {
        self.images.iter().find(|image| image.id == id)
    }

    /// The cover among [Request::images], or none when there is none.
    pub fn cover(&self) -> Option<&Image> {
        self.cover_image_id.and_then(|id| self.image(id))
    }
}

/// A chapter's scenes: as markdown in `scenes`, or, for a format whose manifest entry sets
/// `input = "prose"`, in `prose` as Hammer's parsed blocks, one list of blocks per scene.
///
/// With `images = true`, a line holding only an image is an `image` block, which [prose_image_id]
/// reads; an image inside a line, or one whose file Hammer could not read, is its name as text, never a
/// block.
#[derive(Debug, Clone, PartialEq, Deserialize)]
pub struct Chapter {
    pub name: String,
    #[serde(default)]
    pub scenes: Vec<String>,
    #[serde(default)]
    pub prose: Vec<Vec<Value>>,
}

/// A project image and its file: `image.get`'s [crate::ops::ImageContent] with `mime_type` added.
#[derive(Debug, Clone, PartialEq, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Image {
    pub id: i32,
    pub name: String,
    pub extension: String,
    /// The file's media type; `application/octet-stream` when Hammer does not know the extension.
    pub mime_type: String,
    #[serde(with = "crate::base64::bytes")]
    pub content: Vec<u8>,
}

/// The id of a prose `image` block, `{"type": "image", "id": 5, "name": "Map"}`; none for any other
/// block.
pub fn prose_image_id(block: &Value) -> Option<i32> {
    if block.get("type")?.as_str()? != "image" {
        return None;
    }
    block.get("id")?.as_i64()?.try_into().ok()
}

fn english() -> String {
    "en".into()
}

fn yes() -> bool {
    true
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn a_request_carries_its_images_and_cover() {
        let request: Request = serde_json::from_value(json!({
            "format": "book.epub", "projectName": "Storm", "language": "fr", "topLevelAsChapters": false,
            "chapters": [{"name": "One", "scenes": [], "prose": [
                [{"type": "paragraph", "spans": [{"text": "Rain."}]}, {"type": "image", "id": 5, "name": "Map"}],
                [{"type": "paragraph", "spans": [{"text": "Map"}]}],
            ]}],
            "images": [
                {"id": 2, "name": "Cover", "extension": "jpg", "mimeType": "image/jpeg", "content": "AP8="},
                {"id": 5, "name": "Map", "extension": "png", "mimeType": "image/png", "content": "iVBORw=="},
            ],
            "coverImageId": 2, "settings": {}, "locale": null,
        }))
        .unwrap();
        assert_eq!(request.project_name, "Storm");
        assert_eq!(request.language, "fr");
        assert!(!request.top_level_as_chapters);
        assert_eq!(request.images.iter().map(|image| image.id).collect::<Vec<_>>(), vec![2, 5]);
        let map = request.image(5).unwrap();
        assert_eq!((map.name.as_str(), map.extension.as_str(), map.mime_type.as_str()), ("Map", "png", "image/png"));
        assert_eq!(map.content, b"\x89PNG");
        assert!(request.image(3).is_none());
        assert_eq!(request.cover_image_id, Some(2));
        assert_eq!(request.cover().map(|image| image.content.as_slice()), Some(&[0u8, 255][..]));
        let prose = &request.chapters[0].prose;
        assert_eq!(prose.len(), 2);
        assert_eq!(prose.iter().flatten().map(prose_image_id).collect::<Vec<_>>(), vec![None, Some(5), None]);
        let ids: Vec<_> =
            request.chapters.iter().flat_map(|chapter| &chapter.prose).flatten().filter_map(prose_image_id).collect();
        assert_eq!(ids, vec![5]);
    }

    #[test]
    fn a_request_from_a_host_without_images_has_none() {
        let request: Request = serde_json::from_value(json!({
            "format": "hello.txt", "projectName": "Storm",
            "chapters": [{"name": "One", "scenes": ["![image](hammer://image/5)"]}], "settings": {},
        }))
        .unwrap();
        assert_eq!(request.language, "en");
        assert!(request.top_level_as_chapters);
        assert!(request.images.is_empty());
        assert_eq!(request.cover_image_id, None);
        assert!(request.cover().is_none());
        assert!(request.image(5).is_none());
        assert!(request.chapters[0].prose.is_empty());
    }

    #[test]
    fn a_cover_not_among_the_images_is_none() {
        let request: Request =
            serde_json::from_value(json!({"format": "f", "projectName": "p", "images": [], "coverImageId": 9}))
                .unwrap();
        assert!(request.cover().is_none());
    }

    #[test]
    fn content_that_is_not_base64_fails() {
        let request = serde_json::from_value::<Request>(json!({"format": "f", "projectName": "p", "images": [
            {"id": 1, "name": "a", "extension": "png", "mimeType": "image/png", "content": "not base64!"},
        ]}));
        assert!(request.is_err());
    }

    #[test]
    fn only_an_image_block_has_an_image_id() {
        for block in [
            json!({"type": "paragraph", "spans": [{"text": "Map"}]}),
            json!({"type": "image", "name": "Map"}),
            json!({"type": "image", "id": "5"}),
            json!({"type": "image", "id": 99999999999i64}),
            json!({"id": 5}),
            json!([]),
            json!("image"),
            Value::Null,
        ] {
            assert_eq!(prose_image_id(&block), None, "{block}");
        }
        assert_eq!(prose_image_id(&json!({"name": "Map", "id": 7, "type": "image"})), Some(7));
    }
}
