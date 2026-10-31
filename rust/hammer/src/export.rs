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
//!
//! An exporter whose manifest entry sets `output = "archive"` returns an [Archive] instead: files that
//! Hammer zips, in order, into the one export file, whose `extension` and `mime` the manifest still
//! gives (`zip` and `application/zip`). An image Hammer sent is named by its id, and Hammer writes its
//! bytes, so the reply carries none.
//!
//! ```ignore
//! let mut archive = hammer::export::Archive::new();
//! archive.text("story.md", story)?;
//! for image in &request.images {
//!     archive.image(&format!("images/{}.{}", image.id, image.extension), image)?;
//! }
//! archive.to_json()
//! ```

use serde::{Deserialize, Serialize};
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

/// What the `export` export returns, as [Archive::to_json], for a format whose manifest entry sets
/// `output = "archive"`: files Hammer zips in the order they were added.
///
/// A path is relative, with `/` between its segments: not empty, not starting with `/`, no `\`, no
/// empty segment (so no `//` or trailing `/`), no `.` or `..` segment, and not a path already added.
/// Adding a file with any other path fails, naming it, as Hammer would refuse the reply.
#[derive(Default)]
pub struct Archive {
    files: Vec<File>,
}

#[derive(Serialize)]
struct File {
    path: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    text: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    content: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    image: Option<i32>,
}

/// What [Archive::image] names an image of [Request::images] by: its id, or the [Image] itself.
pub trait ArchiveImage {
    /// The image's id.
    fn id(&self) -> i32;
}

impl ArchiveImage for i32 {
    fn id(&self) -> i32 {
        *self
    }
}

impl ArchiveImage for Image {
    fn id(&self) -> i32 {
        self.id
    }
}

impl<T: ArchiveImage + ?Sized> ArchiveImage for &T {
    fn id(&self) -> i32 {
        (**self).id()
    }
}

impl Archive {
    /// An archive without files.
    pub fn new() -> Archive {
        Archive::default()
    }

    /// Adds `text` as the file at `path`, written as UTF-8.
    pub fn text(&mut self, path: impl Into<String>, text: impl Into<String>) -> crate::Result<&mut Archive> {
        self.add(File { path: path.into(), text: Some(text.into()), content: None, image: None })
    }

    /// Adds `bytes` as the file at `path`.
    pub fn bytes(&mut self, path: impl Into<String>, bytes: &[u8]) -> crate::Result<&mut Archive> {
        self.add(File { path: path.into(), text: None, content: Some(crate::base64::encode(bytes)), image: None })
    }

    /// Adds the file at `path` whose bytes `encoded` holds as standard padded base64, passed on without
    /// decoding it, for base64 a plugin kept itself; Hammer fails the export when it is not base64.
    pub fn base64(&mut self, path: impl Into<String>, encoded: impl Into<String>) -> crate::Result<&mut Archive> {
        self.add(File { path: path.into(), text: None, content: Some(encoded.into()), image: None })
    }

    /// Adds an image of [Request::images] as the file at `path`, named by its id: `5` or `&image`. Hammer
    /// writes its bytes, so the reply carries none; an id it did not send fails the export.
    pub fn image(&mut self, path: impl Into<String>, image: impl ArchiveImage) -> crate::Result<&mut Archive> {
        self.add(File { path: path.into(), text: None, content: None, image: Some(image.id()) })
    }

    /// The `export` export's reply; fails when no file was added.
    pub fn to_json(&self) -> crate::Result<String> {
        if self.files.is_empty() {
            return Err("An archive needs at least one file".into());
        }
        #[derive(Serialize)]
        struct Reply<'a> {
            files: &'a [File],
        }
        Ok(serde_json::to_string(&Reply { files: &self.files })?)
    }

    fn add(&mut self, file: File) -> crate::Result<&mut Archive> {
        if let Some(rule) = self.refuses(&file.path) {
            return Err(format!("Archive path \"{}\" {rule}", file.path).into());
        }
        self.files.push(file);
        Ok(self)
    }

    fn refuses(&self, path: &str) -> Option<&'static str> {
        if path.is_empty() {
            return Some("is empty");
        }
        if path.starts_with('/') {
            return Some("starts with /");
        }
        if path.contains('\\') {
            return Some("has a backslash");
        }
        for segment in path.split('/') {
            match segment {
                "" => return Some("has an empty segment"),
                "." => return Some("has a . segment"),
                ".." => return Some("has a .. segment"),
                _ => {}
            }
        }
        self.files.iter().any(|file| file.path == path).then_some("is already in the archive")
    }
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

    #[test]
    fn an_archive_lists_its_files_in_order_each_with_text_or_content() {
        let mut archive = Archive::new();
        archive.text("story.md", "# One\n\"Rain.\" é").unwrap();
        archive.bytes("images/1.png", b"\x89PNG").unwrap().base64("cover.jpg", "AP8=").unwrap();
        assert_eq!(
            archive.to_json().unwrap(),
            r##"{"files":[{"path":"story.md","text":"# One\n\"Rain.\" é"},"##.to_owned()
                + r#"{"path":"images/1.png","content":"iVBORw=="},{"path":"cover.jpg","content":"AP8="}]}"#
        );
    }

    #[test]
    fn archive_bytes_are_standard_base64_with_padding() {
        let mut archive = Archive::new();
        for (path, bytes) in [("1", &b"f"[..]), ("2", b"fo"), ("3", b"foo"), ("4", b"\xFB\xFF\xBF"), ("5", b"")] {
            archive.bytes(path, bytes).unwrap();
        }
        let reply: Value = serde_json::from_str(&archive.to_json().unwrap()).unwrap();
        assert_eq!(
            reply,
            json!({"files": [
                {"path": "1", "content": "Zg=="}, {"path": "2", "content": "Zm8="}, {"path": "3", "content": "Zm9v"},
                {"path": "4", "content": "+/+/"}, {"path": "5", "content": ""},
            ]})
        );
    }

    #[test]
    fn archive_base64_is_passed_on_as_it_is() {
        // Decoding and encoding again would give Zg== and Zm8=.
        let mut archive = Archive::new();
        archive.base64("a", "Zh==").unwrap().base64("b", String::from("Zm8")).unwrap();
        assert_eq!(
            archive.to_json().unwrap(),
            r#"{"files":[{"path":"a","content":"Zh=="},{"path":"b","content":"Zm8"}]}"#
        );
    }

    #[test]
    fn an_archive_image_is_its_id_a_json_integer() {
        let mut archive = Archive::new();
        archive.image("images/5.png", 5).unwrap().image("images/0.jpg", 0).unwrap();
        archive.image("big.gif", i32::MAX).unwrap();
        assert_eq!(
            archive.to_json().unwrap(),
            r#"{"files":[{"path":"images/5.png","image":5},{"path":"images/0.jpg","image":0},"#.to_owned()
                + r#"{"path":"big.gif","image":2147483647}]}"#
        );
    }

    #[test]
    fn an_archive_image_hammer_sent_is_named_by_its_id() {
        let request: Request = serde_json::from_value(json!({"format": "book.zip", "projectName": "Storm", "images": [
            {"id": 2, "name": "Cover", "extension": "jpg", "mimeType": "image/jpeg", "content": "AP8="},
            {"id": 5, "name": "Map", "extension": "png", "mimeType": "image/png", "content": "iVBORw0KGgo="},
        ], "coverImageId": 2}))
        .unwrap();
        let mut archive = Archive::new();
        archive.image("images/5.png", request.image(5).unwrap()).unwrap();
        archive.image("cover.jpg", request.cover().unwrap().clone()).unwrap();
        for image in &request.images {
            archive.image(format!("all/{}.{}", image.id, image.extension), image).unwrap();
        }
        assert_eq!(
            archive.to_json().unwrap(),
            r#"{"files":[{"path":"images/5.png","image":5},{"path":"cover.jpg","image":2},"#.to_owned()
                + r#"{"path":"all/2.jpg","image":2},{"path":"all/5.png","image":5}]}"#
        );
    }

    #[test]
    fn archive_text_bytes_and_images_keep_their_order() {
        let mut archive = Archive::new();
        archive.image("images/5.png", 5).unwrap().text("story.md", "![Map](images/5.png)").unwrap();
        archive.bytes("data.bin", b"\x01").unwrap().image("cover.jpg", 2).unwrap();
        archive.base64("notes.txt", "Zg==").unwrap();
        assert_eq!(
            archive.to_json().unwrap(),
            r#"{"files":[{"path":"images/5.png","image":5},{"path":"story.md","text":"![Map](images/5.png)"},"#
                .to_owned()
                + r#"{"path":"data.bin","content":"AQ=="},{"path":"cover.jpg","image":2},"#
                + r#"{"path":"notes.txt","content":"Zg=="}]}"#
        );
    }

    #[test]
    fn an_archive_image_path_is_checked_like_any_other() {
        let mut archive = Archive::new();
        archive.text("images/5.png", "a").unwrap();
        let error = archive.image("images/5.png", 5).err().unwrap();
        assert_eq!(error.to_string(), "Archive path \"images/5.png\" is already in the archive");
        assert_eq!(archive.image("/5.png", 5).err().unwrap().to_string(), "Archive path \"/5.png\" starts with /");
    }

    #[test]
    fn each_archive_path_rule_refuses_a_file_naming_its_path() {
        for (path, rule) in [
            ("", "is empty"),
            ("/story.md", "starts with /"),
            ("images\\1.png", "has a backslash"),
            ("images//1.png", "has an empty segment"),
            ("images/", "has an empty segment"),
            ("./story.md", "has a . segment"),
            ("images/../story.md", "has a .. segment"),
            ("images/.", "has a . segment"),
            (".", "has a . segment"),
            ("..", "has a .. segment"),
        ] {
            let mut archive = Archive::new();
            let error = archive.text(path, "x").err().unwrap();
            assert_eq!(error.to_string(), format!("Archive path \"{path}\" {rule}"));
            assert!(archive.to_json().is_err(), "{path:?}");
        }
    }

    #[test]
    fn an_archive_path_already_added_is_refused() {
        let mut archive = Archive::new();
        archive.text("story.md", "a").unwrap();
        let error = archive.bytes("story.md", b"\x01").err().unwrap();
        assert_eq!(error.to_string(), "Archive path \"story.md\" is already in the archive");
        archive.text("Story.md", "b").unwrap().text("a", "c").unwrap().text("ab", "d").unwrap();
        assert_eq!(archive.text("a", "e").err().unwrap().to_string(), "Archive path \"a\" is already in the archive");
        assert_eq!(
            archive.to_json().unwrap(),
            r#"{"files":[{"path":"story.md","text":"a"},{"path":"Story.md","text":"b"},"#.to_owned()
                + r#"{"path":"a","text":"c"},{"path":"ab","text":"d"}]}"#
        );
    }

    #[test]
    fn nested_archive_paths_and_dots_inside_a_segment_are_fine() {
        let mut archive = Archive::new();
        archive.bytes("images/2026/a b.png", b"\x01").unwrap().text(".hidden/...", "").unwrap();
        archive.text("a..b/c.d", "").unwrap().text("images/carte été.png", "").unwrap();
        let paths: Vec<_> = archive.files.iter().map(|file| file.path.as_str()).collect();
        assert_eq!(paths, ["images/2026/a b.png", ".hidden/...", "a..b/c.d", "images/carte été.png"]);
    }

    #[test]
    fn an_archive_without_files_has_no_reply() {
        assert_eq!(Archive::new().to_json().err().unwrap().to_string(), "An archive needs at least one file");
    }
}
