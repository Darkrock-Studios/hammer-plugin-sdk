//! Hammer's operations, typed. Each function runs one operation, as [`crate::call`] would. A plugin may
//! call only those its manifest's `[permissions]` ask for: each function's documentation names what it needs.
//!
//! Generated from api/operations.json by kotlin/codegen: regenerate rather than edit.
#![allow(clippy::all, unused_imports)]

use serde::{Deserialize, Serialize};
use std::collections::BTreeMap;

/// A value this kit doesn't know, from a later Hammer, reads as [`Access::Unknown`].
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum Access {
    #[default]
    #[serde(rename = "read")]
    Read,
    #[serde(rename = "write")]
    Write,
    #[serde(rename = "prose")]
    Prose,
    #[serde(rename = "destructive")]
    Destructive,
    /// A value this kit doesn't know. Hammer rejects it as an input.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct AccountStatus {
    pub server: Option<String>,
    pub ssl: Option<bool>,
    pub email: Option<String>,
    #[serde(rename = "loggedIn")]
    pub logged_in: bool,
    /// Null unless checked.
    #[serde(rename = "tokenValid")]
    pub token_valid: Option<bool>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Activity {
    pub days: Vec<DayWords>,
    /// Lifetime totals, not limited to the range.
    pub devices: Vec<DeviceWords>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Appearance {
    #[serde(rename = "entryId")]
    pub entry_id: i32,
    pub name: String,
    pub r#type: EntryKind,
    #[serde(rename = "sceneCount")]
    pub scene_count: i32,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct ArchivedScene {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct ArchivedScenes {
    pub scenes: Vec<ArchivedScene>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Backup {
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    pub date: String,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Backups {
    pub backups: Vec<Backup>,
}

/// A value this kit doesn't know, from a later Hammer, reads as [`Cadence::Unknown`].
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum Cadence {
    #[default]
    #[serde(rename = "day")]
    Day,
    #[serde(rename = "week")]
    Week,
    /// A value this kit doesn't know. Hammer rejects it as an input.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct ChapterWords {
    pub id: i32,
    pub name: String,
    pub words: i32,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct DayWords {
    /// An ISO 8601 date, such as `2026-03-01`.
    pub date: String,
    pub words: i32,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct DeviceWords {
    pub device: String,
    pub words: i32,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct DraftApplied {
    #[serde(rename = "sceneId")]
    pub scene_id: i32,
    /// The draft holding the text it replaced; null when the text was unchanged.
    #[serde(rename = "previousDraftId")]
    pub previous_draft_id: Option<i32>,
    #[serde(rename = "wordCount")]
    pub word_count: i32,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct DraftInfo {
    pub id: i32,
    pub name: String,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    pub created: String,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct DraftText {
    pub id: i32,
    #[serde(rename = "sceneId")]
    pub scene_id: i32,
    pub name: String,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    pub created: String,
    pub markdown: String,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Drafts {
    pub drafts: Vec<DraftInfo>,
}

/// A value this kit doesn't know, from a later Hammer, reads as [`EntityKind::Unknown`].
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum EntityKind {
    #[default]
    #[serde(rename = "scene")]
    Scene,
    #[serde(rename = "note")]
    Note,
    #[serde(rename = "entry")]
    Entry,
    #[serde(rename = "timeline")]
    Timeline,
    /// A value this kit doesn't know. Hammer rejects it as an input.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Entries {
    pub entries: Vec<EntrySummary>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Entry {
    pub id: i32,
    pub name: String,
    pub r#type: EntryKind,
    pub text: String,
    pub tags: Vec<String>,
    pub aliases: Vec<String>,
    #[serde(rename = "hasImage")]
    pub has_image: bool,
    /// Scene ids whose references to this entry have been confirmed.
    #[serde(rename = "referencedBy")]
    pub referenced_by: Vec<i32>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct EntryCount {
    pub r#type: EntryKind,
    pub count: i32,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct EntryImage {
    /// jpg, jpeg, png, or webp.
    pub extension: String,
    #[serde(with = "crate::base64::bytes")]
    pub content: Vec<u8>,
}

/// A value this kit doesn't know, from a later Hammer, reads as [`EntryKind::Unknown`].
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum EntryKind {
    #[default]
    #[serde(rename = "person")]
    Person,
    #[serde(rename = "place")]
    Place,
    #[serde(rename = "thing")]
    Thing,
    #[serde(rename = "event")]
    Event,
    #[serde(rename = "idea")]
    Idea,
    /// A value this kit doesn't know. Hammer rejects it as an input.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct EntrySummary {
    pub id: i32,
    pub name: String,
    pub r#type: EntryKind,
    /// Other names the entry goes by.
    #[serde(skip_serializing_if = "Option::is_none", default)]
    pub aliases: Option<Vec<String>>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct ExportFormat {
    pub id: String,
    #[serde(rename = "fileExtension")]
    pub file_extension: String,
    #[serde(rename = "mimeType")]
    pub mime_type: String,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct ExportFormats {
    pub formats: Vec<ExportFormat>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct ExportedFile {
    #[serde(rename = "fileName")]
    pub file_name: String,
    #[serde(rename = "mimeType")]
    pub mime_type: String,
    #[serde(with = "crate::base64::bytes")]
    pub content: Vec<u8>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Idea {
    pub id: String,
    pub title: Option<String>,
    pub content: String,
    pub tags: Vec<String>,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    pub created: String,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    pub updated: String,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    pub promoted: Option<String>,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    pub archived: Option<String>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Ideas {
    pub ideas: Vec<Idea>,
}

/// A value this kit doesn't know, from a later Hammer, reads as [`ImportFileFormat::Unknown`].
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ImportFileFormat {
    #[default]
    #[serde(rename = "markdown")]
    Markdown,
    #[serde(rename = "rtf")]
    Rtf,
    /// A value this kit doesn't know. Hammer rejects it as an input.
    #[serde(other)]
    Unknown,
}

/// A value this kit doesn't know, from a later Hammer, reads as [`MarkdownSplit::Unknown`].
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum MarkdownSplit {
    #[default]
    #[serde(rename = "auto")]
    Auto,
    #[serde(rename = "h1")]
    H1,
    #[serde(rename = "h2")]
    H2,
    #[serde(rename = "pattern")]
    Pattern,
    /// A value this kit doesn't know. Hammer rejects it as an input.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Note {
    pub id: i32,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    pub created: String,
    pub tags: Vec<String>,
    pub content: String,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Notes {
    pub notes: Vec<Note>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct OperationDescriptor {
    pub name: String,
    pub description: String,
    pub access: Access,
    pub scope: OperationScope,
    pub input: BTreeMap<String, serde_json::Value>,
    pub output: BTreeMap<String, serde_json::Value>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct OperationList {
    pub operations: Vec<OperationDescriptor>,
}

/// A value this kit doesn't know, from a later Hammer, reads as [`OperationScope::Unknown`].
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum OperationScope {
    #[default]
    #[serde(rename = "content")]
    Content,
    #[serde(rename = "account")]
    Account,
    #[serde(rename = "plugins")]
    Plugins,
    /// A value this kit doesn't know. Hammer rejects it as an input.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct PagedScene {
    pub id: i32,
    pub name: String,
    /// Names of the groups containing the scene, outermost first.
    pub path: Vec<String>,
    pub markdown: String,
    #[serde(rename = "wordCount")]
    pub word_count: i32,
    pub meta: SceneMeta,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct PluginList {
    pub plugins: Vec<PluginSummary>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct PluginRemoved {
    pub id: String,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct PluginSummary {
    pub id: String,
    /// Null, as are [version] and [description], when its package can no longer be read.
    #[serde(skip_serializing_if = "Option::is_none", default)]
    pub name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none", default)]
    pub version: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none", default)]
    pub description: Option<String>,
    pub enabled: bool,
    /// The operations it may use.
    #[serde(skip_serializing_if = "Option::is_none", default)]
    pub operations: Option<Vec<String>>,
    /// Hosts it may send what the user writes to.
    #[serde(skip_serializing_if = "Option::is_none", default)]
    pub http: Option<Vec<String>>,
    /// The setting holding an address it may also send what the user writes to.
    #[serde(rename = "userHostSetting")]
    #[serde(skip_serializing_if = "Option::is_none", default)]
    pub user_host_setting: Option<String>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct ProjectImported {
    pub name: String,
    pub scenes: i32,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct ProjectInfo {
    pub name: String,
    #[serde(rename = "projectId")]
    pub project_id: String,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    pub created: Option<String>,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    #[serde(rename = "lastAccessed")]
    pub last_accessed: Option<String>,
    #[serde(rename = "authorName")]
    pub author_name: Option<String>,
    pub language: Option<String>,
    pub tags: Vec<String>,
    #[serde(rename = "wordCountGoal")]
    pub word_count_goal: Option<WordGoal>,
    #[serde(rename = "wordCount")]
    pub word_count: Option<i32>,
    #[serde(rename = "sceneCount")]
    pub scene_count: Option<i32>,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    #[serde(rename = "lastEdited")]
    pub last_edited: Option<String>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct ProjectList {
    pub projects: Vec<ProjectSummary>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct ProjectName {
    pub name: String,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct ProjectStats {
    #[serde(rename = "totalWords")]
    pub total_words: i32,
    #[serde(rename = "sceneCount")]
    pub scene_count: i32,
    /// Top-level scenes and groups, in story order.
    pub chapters: Vec<ChapterWords>,
    #[serde(rename = "longestScene")]
    pub longest_scene: Option<SceneWords>,
    #[serde(rename = "shortestSceneWords")]
    pub shortest_scene_words: i32,
    #[serde(rename = "medianSceneWords")]
    pub median_scene_words: i32,
    #[serde(rename = "noteCount")]
    pub note_count: i32,
    #[serde(rename = "timelineEventCount")]
    pub timeline_event_count: i32,
    #[serde(rename = "entryCounts")]
    pub entry_counts: Vec<EntryCount>,
    /// People appearing in the most scenes.
    #[serde(rename = "topAppearances")]
    pub top_appearances: Vec<Appearance>,
    #[serde(rename = "wordCountGoal")]
    pub word_count_goal: Option<WordGoal>,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    #[serde(rename = "lastEdited")]
    pub last_edited: Option<String>,
    #[serde(rename = "lastEditedScene")]
    pub last_edited_scene: Option<SceneRef>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct ProjectSummary {
    pub name: String,
    #[serde(rename = "projectId")]
    pub project_id: String,
    /// From the statistics cache; null until statistics are first calculated.
    #[serde(rename = "wordCount")]
    pub word_count: Option<i32>,
    /// Null when the project's metadata can't be read. An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    pub created: Option<String>,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    #[serde(rename = "lastAccessed")]
    pub last_accessed: Option<String>,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    #[serde(rename = "lastEdited")]
    pub last_edited: Option<String>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct ProjectSyncStatus {
    pub name: String,
    /// True once the project has synced.
    pub linked: bool,
    /// Null when never synced. An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    #[serde(rename = "lastSync")]
    pub last_sync: Option<String>,
    #[serde(rename = "pendingChanges")]
    pub pending_changes: i32,
}

/// A value this kit doesn't know, from a later Hammer, reads as [`RtfSplit::Unknown`].
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum RtfSplit {
    #[default]
    #[serde(rename = "formatting")]
    Formatting,
    #[serde(rename = "pattern")]
    Pattern,
    #[serde(rename = "single")]
    Single,
    /// A value this kit doesn't know. Hammer rejects it as an input.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct SceneItemRef {
    pub id: i32,
    pub name: String,
    pub kind: SceneKind,
}

/// A value this kit doesn't know, from a later Hammer, reads as [`SceneKind::Unknown`].
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SceneKind {
    #[default]
    #[serde(rename = "scene")]
    Scene,
    #[serde(rename = "group")]
    Group,
    /// A value this kit doesn't know. Hammer rejects it as an input.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct SceneMeta {
    pub outline: String,
    pub notes: String,
    pub tags: Vec<String>,
    #[serde(rename = "currentDraft")]
    pub current_draft: String,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    pub created: Option<String>,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    #[serde(rename = "lastEdited")]
    pub last_edited: Option<String>,
    /// Entry ids confirmed as in the scene.
    pub references: Vec<i32>,
    /// Entry ids the writer dismissed, which name matching no longer adds.
    #[serde(rename = "dismissedReferences")]
    pub dismissed_references: Vec<i32>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct SceneNode {
    pub id: i32,
    pub name: String,
    pub kind: SceneKind,
    #[serde(rename = "wordCount")]
    pub word_count: i32,
    pub children: Vec<SceneNode>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct ScenePage {
    pub scenes: Vec<PagedScene>,
    /// The last scene's id when more follow, to pass as after; null on the last page.
    pub next: Option<i32>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct SceneRef {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct SceneText {
    pub id: i32,
    pub name: String,
    pub archived: bool,
    pub markdown: String,
    #[serde(rename = "wordCount")]
    pub word_count: i32,
    pub meta: SceneMeta,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct SceneTree {
    pub nodes: Vec<SceneNode>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct SceneWordCount {
    #[serde(rename = "wordCount")]
    pub word_count: i32,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct SceneWords {
    pub id: i32,
    pub name: String,
    pub words: i32,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct SceneWriteResult {
    /// The new draft, in draft mode.
    #[serde(rename = "draftId")]
    pub draft_id: Option<i32>,
    /// In live mode, the draft holding the text it replaced; null when the text was unchanged.
    #[serde(rename = "previousDraftId")]
    pub previous_draft_id: Option<i32>,
    #[serde(rename = "wordCount")]
    pub word_count: i32,
}

/// A value this kit doesn't know, from a later Hammer, reads as [`SearchFilter::Unknown`].
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SearchFilter {
    #[default]
    #[serde(rename = "all")]
    All,
    #[serde(rename = "scenes")]
    Scenes,
    #[serde(rename = "notes")]
    Notes,
    #[serde(rename = "encyclopedia")]
    Encyclopedia,
    #[serde(rename = "timeline")]
    Timeline,
    /// A value this kit doesn't know. Hammer rejects it as an input.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct SearchHit {
    pub kind: EntityKind,
    pub id: i32,
    pub title: String,
    pub snippet: String,
    /// The match's range within [snippet].
    #[serde(rename = "matchStart")]
    pub match_start: i32,
    #[serde(rename = "matchEnd")]
    pub match_end: i32,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct SearchResults {
    pub results: Vec<SearchHit>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Session {
    pub device: String,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    #[serde(rename = "startedAt")]
    pub started_at: String,
    /// An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
    #[serde(rename = "endedAt")]
    pub ended_at: String,
    pub words: i32,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Sessions {
    pub sessions: Vec<Session>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct SyncStatus {
    pub projects: Vec<ProjectSyncStatus>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct TagCount {
    pub tag: String,
    pub count: i32,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct TaggedEntities {
    pub entities: Vec<TaggedEntity>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct TaggedEntity {
    pub kind: EntityKind,
    pub id: i32,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Tags {
    pub tags: Vec<TagCount>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Timeline {
    pub events: Vec<TimelineEntry>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct TimelineEntry {
    pub id: i32,
    pub order: i32,
    /// Free text; timelines are not bound to a calendar.
    pub date: Option<String>,
    pub content: String,
    pub tags: Vec<String>,
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct WordGoal {
    pub cadence: Cadence,
    pub count: i32,
}

/// A value this kit doesn't know, from a later Hammer, reads as [`WriteMode::Unknown`].
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum WriteMode {
    #[default]
    #[serde(rename = "draft")]
    Draft,
    #[serde(rename = "live")]
    Live,
    /// A value this kit doesn't know. Hammer rejects it as an input.
    #[serde(other)]
    Unknown,
}

/// The optional inputs of [`search`]; leave any out with `..Default::default()`.
#[derive(Debug, Clone, Default, PartialEq, Serialize)]
pub struct SearchOptions {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub filter: Option<SearchFilter>,
}

/// Search a project's scenes, notes, encyclopedia, and timeline. #tag in the query filters by tag.
///
/// Needs `search` or `content:read` in the manifest's `[permissions]`.
pub fn search(project: &str, query: &str, options: SearchOptions) -> crate::Result<SearchResults> {
    #[derive(Serialize)]
    struct Input<'a> {
        project: &'a str,
        query: &'a str,
        #[serde(flatten)]
        options: SearchOptions,
    }
    crate::call("search", &Input { project, query, options })
}

/// The `account` operations.
pub mod account {
    use super::*;

    /// The optional inputs of [`status`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct AccountStatusOptions {
        /// Ask the server whether the stored token is still good; needs the network.
        #[serde(skip_serializing_if = "Option::is_none")]
        pub check: Option<bool>,
    }

    /// The sync server and account this install uses, and optionally whether the server still accepts it.
    ///
    /// Needs `account.status` or `account:read` in the manifest's `[permissions]`.
    pub fn status(options: AccountStatusOptions) -> crate::Result<AccountStatus> {
        #[derive(Serialize)]
        struct Input {
            #[serde(flatten)]
            options: AccountStatusOptions,
        }
        crate::call("account.status", &Input { options })
    }

}

/// The `backup` operations.
pub mod backup {
    use super::*;

    /// Back up a project's saved files.
    ///
    /// Needs `backup.create` or `content:write` in the manifest's `[permissions]`.
    pub fn create(project: &str) -> crate::Result<Backup> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
        }
        crate::call("backup.create", &Input { project })
    }

    /// A project's backups, oldest first.
    ///
    /// Needs `backup.list` or `content:read` in the manifest's `[permissions]`.
    pub fn list(project: &str) -> crate::Result<Backups> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
        }
        crate::call("backup.list", &Input { project })
    }

}

/// The `draft` operations.
pub mod draft {
    use super::*;

    /// Replace a scene's text with one of its drafts, saving the replaced text as a draft first.
    ///
    /// Needs `draft.apply` or `content:prose` in the manifest's `[permissions]`.
    pub fn apply(project: &str, id: i32) -> crate::Result<DraftApplied> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
        }
        crate::call("draft.apply", &Input { project, id })
    }

    /// Save a scene's current text as a named draft.
    ///
    /// Needs `draft.create` or `content:write` in the manifest's `[permissions]`.
    pub fn create(project: &str, scene_id: i32, name: &str) -> crate::Result<DraftInfo> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            #[serde(rename = "sceneId")]
            scene_id: i32,
            name: &'a str,
        }
        crate::call("draft.create", &Input { project, scene_id, name })
    }

    /// Delete a draft for good.
    ///
    /// Needs `draft.delete` (by name) in the manifest's `[permissions]`.
    pub fn delete(project: &str, id: i32) -> crate::Result<DraftInfo> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
        }
        crate::call("draft.delete", &Input { project, id })
    }

    /// A scene's saved drafts, oldest first.
    ///
    /// Needs `draft.list` or `content:read` in the manifest's `[permissions]`.
    pub fn list(project: &str, scene_id: i32) -> crate::Result<Drafts> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            #[serde(rename = "sceneId")]
            scene_id: i32,
        }
        crate::call("draft.list", &Input { project, scene_id })
    }

    /// A draft's markdown.
    ///
    /// Needs `draft.read` or `content:read` in the manifest's `[permissions]`.
    pub fn read(project: &str, id: i32) -> crate::Result<DraftText> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
        }
        crate::call("draft.read", &Input { project, id })
    }

}

/// The `entry` operations.
pub mod entry {
    use super::*;

    /// The optional inputs of [`create`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct EntryCreateOptions {
        #[serde(skip_serializing_if = "Option::is_none")]
        pub tags: Option<Vec<String>>,
        /// Other names the entry goes by, matched in scenes like its name.
        #[serde(skip_serializing_if = "Option::is_none")]
        pub aliases: Option<Vec<String>>,
    }

    /// Add an encyclopedia entry to a project.
    ///
    /// Needs `entry.create` or `content:write` in the manifest's `[permissions]`.
    pub fn create(project: &str, name: &str, r#type: EntryKind, text: &str, options: EntryCreateOptions) -> crate::Result<Entry> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            name: &'a str,
            r#type: EntryKind,
            text: &'a str,
            #[serde(flatten)]
            options: EntryCreateOptions,
        }
        crate::call("entry.create", &Input { project, name, r#type, text, options })
    }

    /// Delete an encyclopedia entry, and its image, for good.
    ///
    /// Needs `entry.delete` (by name) in the manifest's `[permissions]`.
    pub fn delete(project: &str, id: i32) -> crate::Result<EntrySummary> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
        }
        crate::call("entry.delete", &Input { project, id })
    }

    /// The optional inputs of [`list`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct EntryListOptions {
        #[serde(skip_serializing_if = "Option::is_none")]
        pub r#type: Option<EntryKind>,
        #[serde(skip_serializing_if = "Option::is_none")]
        pub tag: Option<String>,
    }

    /// A project's encyclopedia entries, with their aliases, optionally only one type or those with a tag.
    ///
    /// Needs `entry.list` or `content:read` in the manifest's `[permissions]`.
    pub fn list(project: &str, options: EntryListOptions) -> crate::Result<Entries> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            #[serde(flatten)]
            options: EntryListOptions,
        }
        crate::call("entry.list", &Input { project, options })
    }

    /// An encyclopedia entry, with the scenes confirmed to reference it.
    ///
    /// Needs `entry.read` or `content:read` in the manifest's `[permissions]`.
    pub fn read(project: &str, id: i32) -> crate::Result<Entry> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
        }
        crate::call("entry.read", &Input { project, id })
    }

    /// The optional inputs of [`update`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct EntryUpdateOptions {
        #[serde(skip_serializing_if = "Option::is_none")]
        pub name: Option<String>,
        #[serde(skip_serializing_if = "Option::is_none")]
        pub tags: Option<Vec<String>>,
        #[serde(skip_serializing_if = "Option::is_none")]
        pub aliases: Option<Vec<String>>,
    }

    /// Replace an encyclopedia entry's text, and its name, tags, or aliases when given.
    ///
    /// Needs `entry.update` or `content:write` in the manifest's `[permissions]`.
    pub fn update(project: &str, id: i32, text: &str, options: EntryUpdateOptions) -> crate::Result<Entry> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
            text: &'a str,
            #[serde(flatten)]
            options: EntryUpdateOptions,
        }
        crate::call("entry.update", &Input { project, id, text, options })
    }

    /// The `entry.image` operations.
    pub mod image {
        use super::*;

        /// An encyclopedia entry's image.
        ///
        /// Needs `entry.image.get` or `content:read` in the manifest's `[permissions]`.
        pub fn get(project: &str, id: i32) -> crate::Result<EntryImage> {
            #[derive(Serialize)]
            struct Input<'a> {
                project: &'a str,
                id: i32,
            }
            crate::call("entry.image.get", &Input { project, id })
        }

        /// Remove an encyclopedia entry's image.
        ///
        /// Needs `entry.image.remove` or `content:write` in the manifest's `[permissions]`.
        pub fn remove(project: &str, id: i32) -> crate::Result<Entry> {
            #[derive(Serialize)]
            struct Input<'a> {
                project: &'a str,
                id: i32,
            }
            crate::call("entry.image.remove", &Input { project, id })
        }

        /// Set an encyclopedia entry's image, replacing any it has. The extension is jpg, jpeg, png, or webp, and must match the image.
        ///
        /// Needs `entry.image.set` or `content:write` in the manifest's `[permissions]`.
        pub fn set(project: &str, id: i32, extension: &str, content: &[u8]) -> crate::Result<Entry> {
            #[derive(Serialize)]
            struct Input<'a> {
                project: &'a str,
                id: i32,
                extension: &'a str,
                #[serde(serialize_with = "crate::base64::serialize")]
                content: &'a [u8],
            }
            crate::call("entry.image.set", &Input { project, id, extension, content })
        }

    }

}

/// The `export` operations.
pub mod export {
    use super::*;

    /// The formats project.export can produce.
    ///
    /// Needs `export.formats` or `content:read` in the manifest's `[permissions]`.
    pub fn formats() -> crate::Result<ExportFormats> {
        #[derive(Serialize)]
        struct Input {
        }
        crate::call("export.formats", &Input {  })
    }

}

/// The `idea` operations.
pub mod idea {
    use super::*;

    /// Archive a story idea.
    ///
    /// Needs `idea.archive` or `content:write` in the manifest's `[permissions]`.
    pub fn archive(id: &str) -> crate::Result<Idea> {
        #[derive(Serialize)]
        struct Input<'a> {
            id: &'a str,
        }
        crate::call("idea.archive", &Input { id })
    }

    /// The optional inputs of [`create`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct IdeaCreateOptions {
        #[serde(skip_serializing_if = "Option::is_none")]
        pub title: Option<String>,
        #[serde(skip_serializing_if = "Option::is_none")]
        pub tags: Option<Vec<String>>,
    }

    /// Add a story idea.
    ///
    /// Needs `idea.create` or `content:write` in the manifest's `[permissions]`.
    pub fn create(content: &str, options: IdeaCreateOptions) -> crate::Result<Idea> {
        #[derive(Serialize)]
        struct Input<'a> {
            content: &'a str,
            #[serde(flatten)]
            options: IdeaCreateOptions,
        }
        crate::call("idea.create", &Input { content, options })
    }

    /// Delete a story idea for good.
    ///
    /// Needs `idea.delete` (by name) in the manifest's `[permissions]`.
    pub fn delete(id: &str) -> crate::Result<Idea> {
        #[derive(Serialize)]
        struct Input<'a> {
            id: &'a str,
        }
        crate::call("idea.delete", &Input { id })
    }

    /// The optional inputs of [`list`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct IdeaListOptions {
        /// True for only archived ideas, false for only active ones; omitted for both.
        #[serde(skip_serializing_if = "Option::is_none")]
        pub archived: Option<bool>,
    }

    /// Story ideas, which belong to the account rather than a project.
    ///
    /// Needs `idea.list` or `content:read` in the manifest's `[permissions]`.
    pub fn list(options: IdeaListOptions) -> crate::Result<Ideas> {
        #[derive(Serialize)]
        struct Input {
            #[serde(flatten)]
            options: IdeaListOptions,
        }
        crate::call("idea.list", &Input { options })
    }

    /// Return an archived story idea to the active ones.
    ///
    /// Needs `idea.unarchive` or `content:write` in the manifest's `[permissions]`.
    pub fn unarchive(id: &str) -> crate::Result<Idea> {
        #[derive(Serialize)]
        struct Input<'a> {
            id: &'a str,
        }
        crate::call("idea.unarchive", &Input { id })
    }

    /// The optional inputs of [`update`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct IdeaUpdateOptions {
        /// Left out, unchanged; empty, cleared.
        #[serde(skip_serializing_if = "Option::is_none")]
        pub title: Option<String>,
        #[serde(skip_serializing_if = "Option::is_none")]
        pub tags: Option<Vec<String>>,
    }

    /// Replace a story idea's text, and its title or tags when given. An empty title clears it.
    ///
    /// Needs `idea.update` or `content:write` in the manifest's `[permissions]`.
    pub fn update(id: &str, content: &str, options: IdeaUpdateOptions) -> crate::Result<Idea> {
        #[derive(Serialize)]
        struct Input<'a> {
            id: &'a str,
            content: &'a str,
            #[serde(flatten)]
            options: IdeaUpdateOptions,
        }
        crate::call("idea.update", &Input { id, content, options })
    }

}

/// The `note` operations.
pub mod note {
    use super::*;

    /// The optional inputs of [`create`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct NoteCreateOptions {
        #[serde(skip_serializing_if = "Option::is_none")]
        pub tags: Option<Vec<String>>,
    }

    /// Add a note to a project.
    ///
    /// Needs `note.create` or `content:write` in the manifest's `[permissions]`.
    pub fn create(project: &str, content: &str, options: NoteCreateOptions) -> crate::Result<Note> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            content: &'a str,
            #[serde(flatten)]
            options: NoteCreateOptions,
        }
        crate::call("note.create", &Input { project, content, options })
    }

    /// Delete a note for good.
    ///
    /// Needs `note.delete` (by name) in the manifest's `[permissions]`.
    pub fn delete(project: &str, id: i32) -> crate::Result<Note> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
        }
        crate::call("note.delete", &Input { project, id })
    }

    /// The optional inputs of [`list`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct NoteListOptions {
        #[serde(skip_serializing_if = "Option::is_none")]
        pub tag: Option<String>,
    }

    /// A project's notes, optionally only those with a tag.
    ///
    /// Needs `note.list` or `content:read` in the manifest's `[permissions]`.
    pub fn list(project: &str, options: NoteListOptions) -> crate::Result<Notes> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            #[serde(flatten)]
            options: NoteListOptions,
        }
        crate::call("note.list", &Input { project, options })
    }

    /// One note.
    ///
    /// Needs `note.read` or `content:read` in the manifest's `[permissions]`.
    pub fn read(project: &str, id: i32) -> crate::Result<Note> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
        }
        crate::call("note.read", &Input { project, id })
    }

    /// The optional inputs of [`update`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct NoteUpdateOptions {
        /// Left out, the tags stay as they are.
        #[serde(skip_serializing_if = "Option::is_none")]
        pub tags: Option<Vec<String>>,
    }

    /// Replace a note's text, and its tags when given.
    ///
    /// Needs `note.update` or `content:write` in the manifest's `[permissions]`.
    pub fn update(project: &str, id: i32, content: &str, options: NoteUpdateOptions) -> crate::Result<Note> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
            content: &'a str,
            #[serde(flatten)]
            options: NoteUpdateOptions,
        }
        crate::call("note.update", &Input { project, id, content, options })
    }

}

/// The `ops` operations.
pub mod ops {
    use super::*;

    /// Every operation you may call, with JSON schemas for its input and output.
    ///
    /// Needs `ops.list` or `content:read` in the manifest's `[permissions]`.
    pub fn list() -> crate::Result<OperationList> {
        #[derive(Serialize)]
        struct Input {
        }
        crate::call("ops.list", &Input {  })
    }

}

/// The `plugin` operations.
pub mod plugin {
    use super::*;

    /// Install, or replace, a runtime plugin from a .hammerplugin file, granting it every permission its manifest asks for, which the reply lists. Takes effect at once in a running app; otherwise, the next time it starts.
    ///
    /// Needs `plugin.install` (by name) in the manifest's `[permissions]`.
    pub fn install(path: &str) -> crate::Result<PluginSummary> {
        #[derive(Serialize)]
        struct Input<'a> {
            path: &'a str,
        }
        crate::call("plugin.install", &Input { path })
    }

    /// The installed runtime plugins, and what each may use.
    ///
    /// Needs `plugin.list` or `plugins:read` in the manifest's `[permissions]`.
    pub fn list() -> crate::Result<PluginList> {
        #[derive(Serialize)]
        struct Input {
        }
        crate::call("plugin.list", &Input {  })
    }

    /// Uninstall a runtime plugin, forgetting its cache and secrets. Its other settings are kept for a reinstall.
    ///
    /// Needs `plugin.uninstall` (by name) in the manifest's `[permissions]`.
    pub fn uninstall(id: &str) -> crate::Result<PluginRemoved> {
        #[derive(Serialize)]
        struct Input<'a> {
            id: &'a str,
        }
        crate::call("plugin.uninstall", &Input { id })
    }

}

/// The `project` operations.
pub mod project {
    use super::*;

    /// Create an empty project.
    ///
    /// Needs `project.create` or `content:write` in the manifest's `[permissions]`.
    pub fn create(name: &str) -> crate::Result<ProjectName> {
        #[derive(Serialize)]
        struct Input<'a> {
            name: &'a str,
        }
        crate::call("project.create", &Input { name })
    }

    /// Delete a project, and everything in it, for good. It must not be open in Hammer.
    ///
    /// Needs `project.delete` (by name) in the manifest's `[permissions]`.
    pub fn delete(project: &str) -> crate::Result<ProjectName> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
        }
        crate::call("project.delete", &Input { project })
    }

    /// The optional inputs of [`export`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct ProjectExportOptions {
        /// Limits the export to these scenes; omitted exports the whole story.
        #[serde(rename = "sceneIds")]
        #[serde(skip_serializing_if = "Option::is_none")]
        pub scene_ids: Option<Vec<i32>>,
        #[serde(rename = "treatTopLevelAsChapters")]
        #[serde(skip_serializing_if = "Option::is_none")]
        pub treat_top_level_as_chapters: Option<bool>,
    }

    /// Export a project, or some of its scenes, in one of the formats from export.formats.
    ///
    /// Needs `project.export` or `content:read` in the manifest's `[permissions]`.
    pub fn export(project: &str, format: &str, options: ProjectExportOptions) -> crate::Result<ExportedFile> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            format: &'a str,
            #[serde(flatten)]
            options: ProjectExportOptions,
        }
        crate::call("project.export", &Input { project, format, options })
    }

    /// The optional inputs of [`import`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct ProjectImportOptions {
        /// Left out, the title the file gives itself.
        #[serde(skip_serializing_if = "Option::is_none")]
        pub name: Option<String>,
        #[serde(rename = "markdownSplit")]
        #[serde(skip_serializing_if = "Option::is_none")]
        pub markdown_split: Option<MarkdownSplit>,
        #[serde(rename = "rtfSplit")]
        #[serde(skip_serializing_if = "Option::is_none")]
        pub rtf_split: Option<RtfSplit>,
        /// A regular expression for the lines that start a chapter; left out, chapter, part, prologue, and epilogue.
        #[serde(rename = "chapterPattern")]
        #[serde(skip_serializing_if = "Option::is_none")]
        pub chapter_pattern: Option<String>,
        /// Put each chapter's scenes in a group.
        #[serde(rename = "chapterGroups")]
        #[serde(skip_serializing_if = "Option::is_none")]
        pub chapter_groups: Option<bool>,
    }

    /// Create a project from a Markdown or RTF manuscript, split into scenes by its headings or a chapter pattern.
    ///
    /// Needs `project.import` or `content:prose` in the manifest's `[permissions]`.
    pub fn import(format: ImportFileFormat, content: &[u8], options: ProjectImportOptions) -> crate::Result<ProjectImported> {
        #[derive(Serialize)]
        struct Input<'a> {
            format: ImportFileFormat,
            #[serde(serialize_with = "crate::base64::serialize")]
            content: &'a [u8],
            #[serde(flatten)]
            options: ProjectImportOptions,
        }
        crate::call("project.import", &Input { format, content, options })
    }

    /// A project's details: author, language, tags, word count goal, cached totals, and sync linkage.
    ///
    /// Needs `project.info` or `content:read` in the manifest's `[permissions]`.
    pub fn info(project: &str) -> crate::Result<ProjectInfo> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
        }
        crate::call("project.info", &Input { project })
    }

    /// List every project with its word count and when it was last edited.
    ///
    /// Needs `project.list` or `content:read` in the manifest's `[permissions]`.
    pub fn list() -> crate::Result<ProjectList> {
        #[derive(Serialize)]
        struct Input {
        }
        crate::call("project.list", &Input {  })
    }

    /// Rename a project. It must not be open in Hammer.
    ///
    /// Needs `project.rename` or `content:write` in the manifest's `[permissions]`.
    pub fn rename(project: &str, name: &str) -> crate::Result<ProjectName> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            name: &'a str,
        }
        crate::call("project.rename", &Input { project, name })
    }

}

/// The `scene` operations.
pub mod scene {
    use super::*;

    /// Add markdown to the end of a scene's text, as a new paragraph.
    ///
    /// Needs `scene.append` or `content:prose` in the manifest's `[permissions]`.
    pub fn append(project: &str, id: i32, markdown: &str) -> crate::Result<SceneWordCount> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
            markdown: &'a str,
        }
        crate::call("scene.append", &Input { project, id, markdown })
    }

    /// Archive a scene: take it out of the story, keeping its text, drafts, and notes.
    ///
    /// Needs `scene.archive` or `content:write` in the manifest's `[permissions]`.
    pub fn archive(project: &str, id: i32) -> crate::Result<SceneItemRef> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
        }
        crate::call("scene.archive", &Input { project, id })
    }

    /// A project's archived scenes.
    ///
    /// Needs `scene.archived` or `content:read` in the manifest's `[permissions]`.
    pub fn archived(project: &str) -> crate::Result<ArchivedScenes> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
        }
        crate::call("scene.archived", &Input { project })
    }

    /// The optional inputs of [`create`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct SceneCreateOptions {
        #[serde(skip_serializing_if = "Option::is_none")]
        pub kind: Option<SceneKind>,
        /// The group to create it in; left out, the top level.
        #[serde(rename = "parentId")]
        #[serde(skip_serializing_if = "Option::is_none")]
        pub parent_id: Option<i32>,
        /// Its position among its siblings; left out, the end.
        #[serde(skip_serializing_if = "Option::is_none")]
        pub index: Option<i32>,
    }

    /// Create an empty scene or group, at the end of its parent unless an index is given.
    ///
    /// Needs `scene.create` or `content:write` in the manifest's `[permissions]`.
    pub fn create(project: &str, name: &str, options: SceneCreateOptions) -> crate::Result<SceneItemRef> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            name: &'a str,
            #[serde(flatten)]
            options: SceneCreateOptions,
        }
        crate::call("scene.create", &Input { project, name, options })
    }

    /// Delete a scene, archived or not, or an empty group, for good.
    ///
    /// Needs `scene.delete` (by name) in the manifest's `[permissions]`.
    pub fn delete(project: &str, id: i32) -> crate::Result<SceneItemRef> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
        }
        crate::call("scene.delete", &Input { project, id })
    }

    /// The optional inputs of [`r#move`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct SceneMoveOptions {
        /// The group to move into; left out, the top level.
        #[serde(rename = "parentId")]
        #[serde(skip_serializing_if = "Option::is_none")]
        pub parent_id: Option<i32>,
    }

    /// Move a scene or group to a position within a group, or the top level when parentId is left out. index is its position there after the move.
    ///
    /// Needs `scene.move` or `content:write` in the manifest's `[permissions]`.
    pub fn r#move(project: &str, id: i32, index: i32, options: SceneMoveOptions) -> crate::Result<SceneItemRef> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
            index: i32,
            #[serde(flatten)]
            options: SceneMoveOptions,
        }
        crate::call("scene.move", &Input { project, id, index, options })
    }

    /// A scene's markdown, including unsaved edits, with its outline, notes, and tags.
    ///
    /// Needs `scene.read` or `content:read` in the manifest's `[permissions]`.
    pub fn read(project: &str, id: i32) -> crate::Result<SceneText> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
        }
        crate::call("scene.read", &Input { project, id })
    }

    /// Rename a scene or group.
    ///
    /// Needs `scene.rename` or `content:write` in the manifest's `[permissions]`.
    pub fn rename(project: &str, id: i32, name: &str) -> crate::Result<SceneItemRef> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
            name: &'a str,
        }
        crate::call("scene.rename", &Input { project, id, name })
    }

    /// A project's scenes and groups in story order, with word counts.
    ///
    /// Needs `scene.tree` or `content:read` in the manifest's `[permissions]`.
    pub fn tree(project: &str) -> crate::Result<SceneTree> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
        }
        crate::call("scene.tree", &Input { project })
    }

    /// Return an archived scene to the story, at the end of the top level.
    ///
    /// Needs `scene.unarchive` or `content:write` in the manifest's `[permissions]`.
    pub fn unarchive(project: &str, id: i32) -> crate::Result<SceneItemRef> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
        }
        crate::call("scene.unarchive", &Input { project, id })
    }

    /// The optional inputs of [`write`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct SceneWriteOptions {
        /// The new draft's name, in draft mode only.
        #[serde(rename = "draftName")]
        #[serde(skip_serializing_if = "Option::is_none")]
        pub draft_name: Option<String>,
    }

    /// Replace a scene's text with markdown. Mode draft saves it as a new draft of the scene and leaves the scene alone. Mode live replaces the scene's text, saving the old text as a draft first.
    ///
    /// Needs `scene.write` or `content:prose` in the manifest's `[permissions]`.
    pub fn write(project: &str, id: i32, mode: WriteMode, markdown: &str, options: SceneWriteOptions) -> crate::Result<SceneWriteResult> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
            mode: WriteMode,
            markdown: &'a str,
            #[serde(flatten)]
            options: SceneWriteOptions,
        }
        crate::call("scene.write", &Input { project, id, mode, markdown, options })
    }

    /// The `scene.meta` operations.
    pub mod meta {
        use super::*;

        /// A scene's outline, notes, tags, and references: the entries confirmed as in it, and those dismissed.
        ///
        /// Needs `scene.meta.read` or `content:read` in the manifest's `[permissions]`.
        pub fn read(project: &str, id: i32) -> crate::Result<SceneMeta> {
            #[derive(Serialize)]
            struct Input<'a> {
                project: &'a str,
                id: i32,
            }
            crate::call("scene.meta.read", &Input { project, id })
        }

        /// The optional inputs of [`write`]; leave any out with `..Default::default()`.
        #[derive(Debug, Clone, Default, PartialEq, Serialize)]
        pub struct SceneMetaWriteOptions {
            #[serde(skip_serializing_if = "Option::is_none")]
            pub outline: Option<String>,
            #[serde(skip_serializing_if = "Option::is_none")]
            pub notes: Option<String>,
            #[serde(skip_serializing_if = "Option::is_none")]
            pub tags: Option<Vec<String>>,
        }

        /// Change a scene's outline, notes, or tags. Fields left out keep their value.
        ///
        /// Needs `scene.meta.write` or `content:write` in the manifest's `[permissions]`.
        pub fn write(project: &str, id: i32, options: SceneMetaWriteOptions) -> crate::Result<SceneMeta> {
            #[derive(Serialize)]
            struct Input<'a> {
                project: &'a str,
                id: i32,
                #[serde(flatten)]
                options: SceneMetaWriteOptions,
            }
            crate::call("scene.meta.write", &Input { project, id, options })
        }

    }

    /// The `scene.read` operations.
    pub mod read {
        use super::*;

        /// The optional inputs of [`many`]; leave any out with `..Default::default()`.
        #[derive(Debug, Clone, Default, PartialEq, Serialize)]
        pub struct SceneReadManyOptions {
            /// A group whose scenes, at any depth, to read.
            #[serde(skip_serializing_if = "Option::is_none")]
            pub group: Option<i32>,
            #[serde(skip_serializing_if = "Option::is_none")]
            pub ids: Option<Vec<i32>>,
            /// The previous page's next.
            #[serde(skip_serializing_if = "Option::is_none")]
            pub after: Option<i32>,
            #[serde(rename = "maxWords")]
            #[serde(skip_serializing_if = "Option::is_none")]
            pub max_words: Option<i32>,
        }

        /// Scenes in story order with their markdown, metadata, and the groups they sit in, a page of about maxWords, counting outlines and notes, at a time: every scene, a group's, or those in ids. Archived scenes are left out. Pass next as after for the following page; it is null on the last.
        ///
        /// Needs `scene.read.many` or `content:read` in the manifest's `[permissions]`.
        pub fn many(project: &str, options: SceneReadManyOptions) -> crate::Result<ScenePage> {
            #[derive(Serialize)]
            struct Input<'a> {
                project: &'a str,
                #[serde(flatten)]
                options: SceneReadManyOptions,
            }
            crate::call("scene.read.many", &Input { project, options })
        }

    }

    /// The `scene.references` operations.
    pub mod references {
        use super::*;

        /// The optional inputs of [`set`]; leave any out with `..Default::default()`.
        #[derive(Debug, Clone, Default, PartialEq, Serialize)]
        pub struct SceneReferencesSetOptions {
            #[serde(skip_serializing_if = "Option::is_none")]
            pub confirm: Option<Vec<i32>>,
            #[serde(skip_serializing_if = "Option::is_none")]
            pub dismiss: Option<Vec<i32>>,
            #[serde(skip_serializing_if = "Option::is_none")]
            pub clear: Option<Vec<i32>>,
        }

        /// Change which entries a scene references. confirm marks entries as in the scene, and un-dismisses them; dismiss marks entries as only mentioned, so name matching stops adding them; clear forgets entries either way, so a later match can add them again.
        ///
        /// Needs `scene.references.set` or `content:write` in the manifest's `[permissions]`.
        pub fn set(project: &str, id: i32, options: SceneReferencesSetOptions) -> crate::Result<SceneMeta> {
            #[derive(Serialize)]
            struct Input<'a> {
                project: &'a str,
                id: i32,
                #[serde(flatten)]
                options: SceneReferencesSetOptions,
            }
            crate::call("scene.references.set", &Input { project, id, options })
        }

    }

}

/// The `stats` operations.
pub mod stats {
    use super::*;

    /// The optional inputs of [`activity`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct StatsActivityOptions {
        /// First day included, in the device's time zone; omitted for no lower bound. An ISO 8601 date, such as `2026-03-01`.
        #[serde(skip_serializing_if = "Option::is_none")]
        pub from: Option<String>,
        /// Last day included; omitted for no upper bound. An ISO 8601 date, such as `2026-03-01`.
        #[serde(skip_serializing_if = "Option::is_none")]
        pub to: Option<String>,
    }

    /// Words written per day over a date range, plus lifetime words per device.
    ///
    /// Needs `stats.activity` or `content:read` in the manifest's `[permissions]`.
    pub fn activity(project: &str, options: StatsActivityOptions) -> crate::Result<Activity> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            #[serde(flatten)]
            options: StatsActivityOptions,
        }
        crate::call("stats.activity", &Input { project, options })
    }

    /// The optional inputs of [`project`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct StatsProjectOptions {
        /// Recalculate even when the cached statistics are current.
        #[serde(skip_serializing_if = "Option::is_none")]
        pub recalculate: Option<bool>,
    }

    /// A project's statistics, as its Home screen shows them.
    ///
    /// Needs `stats.project` or `content:read` in the manifest's `[permissions]`.
    pub fn project(project: &str, options: StatsProjectOptions) -> crate::Result<ProjectStats> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            #[serde(flatten)]
            options: StatsProjectOptions,
        }
        crate::call("stats.project", &Input { project, options })
    }

    /// The optional inputs of [`sessions`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct StatsSessionsOptions {
        /// First day included, in the device's time zone; omitted for no lower bound. An ISO 8601 date, such as `2026-03-01`.
        #[serde(skip_serializing_if = "Option::is_none")]
        pub from: Option<String>,
        /// Last day included; omitted for no upper bound. An ISO 8601 date, such as `2026-03-01`.
        #[serde(skip_serializing_if = "Option::is_none")]
        pub to: Option<String>,
    }

    /// Writing sessions over a date range, from every device.
    ///
    /// Needs `stats.sessions` or `content:read` in the manifest's `[permissions]`.
    pub fn sessions(project: &str, options: StatsSessionsOptions) -> crate::Result<Sessions> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            #[serde(flatten)]
            options: StatsSessionsOptions,
        }
        crate::call("stats.sessions", &Input { project, options })
    }

}

/// The `sync` operations.
pub mod sync {
    use super::*;

    /// Each project's link to the sync server, its last sync, and how many of its changes are waiting to sync.
    ///
    /// Needs `sync.status` or `account:read` in the manifest's `[permissions]`.
    pub fn status() -> crate::Result<SyncStatus> {
        #[derive(Serialize)]
        struct Input {
        }
        crate::call("sync.status", &Input {  })
    }

}

/// The `tag` operations.
pub mod tag {
    use super::*;

    /// Every scene, note, entry, and timeline event carrying a tag.
    ///
    /// Needs `tag.find` or `content:read` in the manifest's `[permissions]`.
    pub fn find(project: &str, tag: &str) -> crate::Result<TaggedEntities> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            tag: &'a str,
        }
        crate::call("tag.find", &Input { project, tag })
    }

    /// The optional inputs of [`list`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct TagListOptions {
        #[serde(skip_serializing_if = "Option::is_none")]
        pub kind: Option<EntityKind>,
        #[serde(skip_serializing_if = "Option::is_none")]
        pub limit: Option<i32>,
    }

    /// The tags used in a project, most used first, optionally counting one kind of entity.
    ///
    /// Needs `tag.list` or `content:read` in the manifest's `[permissions]`.
    pub fn list(project: &str, options: TagListOptions) -> crate::Result<Tags> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            #[serde(flatten)]
            options: TagListOptions,
        }
        crate::call("tag.list", &Input { project, options })
    }

}

/// The `timeline` operations.
pub mod timeline {
    use super::*;

    /// The optional inputs of [`create`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct TimelineCreateOptions {
        /// Free text, such as "Day 3" or "Spring, 1802".
        #[serde(skip_serializing_if = "Option::is_none")]
        pub date: Option<String>,
        #[serde(skip_serializing_if = "Option::is_none")]
        pub tags: Option<Vec<String>>,
        /// Its position in the timeline; left out, the end.
        #[serde(skip_serializing_if = "Option::is_none")]
        pub index: Option<i32>,
    }

    /// Add an event to a project's timeline, at the end unless an index is given.
    ///
    /// Needs `timeline.create` or `content:write` in the manifest's `[permissions]`.
    pub fn create(project: &str, content: &str, options: TimelineCreateOptions) -> crate::Result<TimelineEntry> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            content: &'a str,
            #[serde(flatten)]
            options: TimelineCreateOptions,
        }
        crate::call("timeline.create", &Input { project, content, options })
    }

    /// Delete a timeline event for good.
    ///
    /// Needs `timeline.delete` (by name) in the manifest's `[permissions]`.
    pub fn delete(project: &str, id: i32) -> crate::Result<TimelineEntry> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
        }
        crate::call("timeline.delete", &Input { project, id })
    }

    /// A project's timeline events in order.
    ///
    /// Needs `timeline.list` or `content:read` in the manifest's `[permissions]`.
    pub fn list(project: &str) -> crate::Result<Timeline> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
        }
        crate::call("timeline.list", &Input { project })
    }

    /// Move a timeline event to a position; index is its position after the move.
    ///
    /// Needs `timeline.move` or `content:write` in the manifest's `[permissions]`.
    pub fn r#move(project: &str, id: i32, index: i32) -> crate::Result<TimelineEntry> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
            index: i32,
        }
        crate::call("timeline.move", &Input { project, id, index })
    }

    /// One timeline event.
    ///
    /// Needs `timeline.read` or `content:read` in the manifest's `[permissions]`.
    pub fn read(project: &str, id: i32) -> crate::Result<TimelineEntry> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
        }
        crate::call("timeline.read", &Input { project, id })
    }

    /// The optional inputs of [`update`]; leave any out with `..Default::default()`.
    #[derive(Debug, Clone, Default, PartialEq, Serialize)]
    pub struct TimelineUpdateOptions {
        /// Left out, unchanged; empty, cleared.
        #[serde(skip_serializing_if = "Option::is_none")]
        pub date: Option<String>,
        #[serde(skip_serializing_if = "Option::is_none")]
        pub tags: Option<Vec<String>>,
    }

    /// Replace a timeline event's text, and its date or tags when given. An empty date clears it.
    ///
    /// Needs `timeline.update` or `content:write` in the manifest's `[permissions]`.
    pub fn update(project: &str, id: i32, content: &str, options: TimelineUpdateOptions) -> crate::Result<TimelineEntry> {
        #[derive(Serialize)]
        struct Input<'a> {
            project: &'a str,
            id: i32,
            content: &'a str,
            #[serde(flatten)]
            options: TimelineUpdateOptions,
        }
        crate::call("timeline.update", &Input { project, id, content, options })
    }

}
