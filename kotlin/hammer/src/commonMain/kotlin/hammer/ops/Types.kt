// Generated from api/operations.json by kotlin/codegen: regenerate rather than edit.
@file:Suppress("RedundantVisibilityModifier", "unused")

package hammer.ops

import hammer.Base64Bytes
import hammer.OperationEnumSerializer
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A value this kit doesn't know, from a later Hammer, reads as [Unknown].
 */
@Serializable(with = Access.Serializer::class)
public enum class Access(
	/**
	 * What Hammer calls it; empty for [Unknown].
	 */
	public val `value`: String,
) {
	Read("read"),
	Write("write"),
	Destructive("destructive"),
	Unknown(""),
	;

	internal object Serializer : OperationEnumSerializer<Access>("hammer.ops.Access", Access.entries, Access.Unknown, { it.value })
}

@Serializable
public data class AccountStatus(
	public val server: String? = null,
	public val ssl: Boolean? = null,
	public val email: String? = null,
	public val loggedIn: Boolean = false,
	/**
	 * Null unless checked.
	 */
	public val tokenValid: Boolean? = null,
)

@Serializable
public data class Activity(
	public val days: List<DayWords> = emptyList(),
	/**
	 * Lifetime totals, not limited to the range.
	 */
	public val devices: List<DeviceWords> = emptyList(),
)

@Serializable
public data class Appearance(
	public val entryId: Int = 0,
	public val name: String = "",
	public val type: EntryKind = EntryKind.Person,
	public val sceneCount: Int = 0,
)

@Serializable
public data class ArchivedScene(
	public val id: Int = 0,
	public val name: String = "",
)

@Serializable
public data class ArchivedScenes(
	public val scenes: List<ArchivedScene> = emptyList(),
)

@Serializable
public data class Backup(
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val date: String = "",
)

@Serializable
public data class Backups(
	public val backups: List<Backup> = emptyList(),
)

/**
 * A value this kit doesn't know, from a later Hammer, reads as [Unknown].
 */
@Serializable(with = Cadence.Serializer::class)
public enum class Cadence(
	/**
	 * What Hammer calls it; empty for [Unknown].
	 */
	public val `value`: String,
) {
	Day("day"),
	Week("week"),
	Unknown(""),
	;

	internal object Serializer : OperationEnumSerializer<Cadence>("hammer.ops.Cadence", Cadence.entries, Cadence.Unknown, { it.value })
}

@Serializable
public data class ChapterWords(
	public val id: Int = 0,
	public val name: String = "",
	public val words: Int = 0,
)

/**
 * A value this kit doesn't know, from a later Hammer, reads as [Unknown].
 */
@Serializable(with = ConflictChoice.Serializer::class)
public enum class ConflictChoice(
	/**
	 * What Hammer calls it; empty for [Unknown].
	 */
	public val `value`: String,
) {
	Abort("abort"),
	Local("local"),
	Server("server"),
	Unknown(""),
	;

	internal object Serializer : OperationEnumSerializer<ConflictChoice>("hammer.ops.ConflictChoice", ConflictChoice.entries, ConflictChoice.Unknown, { it.value })
}

@Serializable
public data class DayWords(
	/**
	 * An ISO 8601 date, such as `2026-03-01`.
	 */
	public val date: String = "",
	public val words: Int = 0,
)

@Serializable
public data class DeviceWords(
	public val device: String = "",
	public val words: Int = 0,
)

@Serializable
public data class DraftApplied(
	public val sceneId: Int = 0,
	/**
	 * The draft holding the text it replaced; null when the text was unchanged.
	 */
	public val previousDraftId: Int? = null,
	public val wordCount: Int = 0,
)

@Serializable
public data class DraftInfo(
	public val id: Int = 0,
	public val name: String = "",
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val created: String = "",
)

@Serializable
public data class DraftText(
	public val id: Int = 0,
	public val sceneId: Int = 0,
	public val name: String = "",
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val created: String = "",
	public val markdown: String = "",
)

@Serializable
public data class Drafts(
	public val drafts: List<DraftInfo> = emptyList(),
)

/**
 * A value this kit doesn't know, from a later Hammer, reads as [Unknown].
 */
@Serializable(with = EntityKind.Serializer::class)
public enum class EntityKind(
	/**
	 * What Hammer calls it; empty for [Unknown].
	 */
	public val `value`: String,
) {
	Scene("scene"),
	Note("note"),
	Entry("entry"),
	Timeline("timeline"),
	Unknown(""),
	;

	internal object Serializer : OperationEnumSerializer<EntityKind>("hammer.ops.EntityKind", EntityKind.entries, EntityKind.Unknown, { it.value })
}

@Serializable
public data class Entries(
	public val entries: List<EntrySummary> = emptyList(),
)

@Serializable
public data class Entry(
	public val id: Int = 0,
	public val name: String = "",
	public val type: EntryKind = EntryKind.Person,
	public val text: String = "",
	public val tags: List<String> = emptyList(),
	public val aliases: List<String> = emptyList(),
	public val hasImage: Boolean = false,
	/**
	 * Scene ids whose references to this entry have been confirmed.
	 */
	public val referencedBy: List<Int> = emptyList(),
)

@Serializable
public data class EntryCount(
	public val type: EntryKind = EntryKind.Person,
	public val count: Int = 0,
)

@Serializable
public data class EntryImage(
	/**
	 * jpg, jpeg, png, or webp.
	 */
	public val extension: String = "",
	@Serializable(with = Base64Bytes::class)
	public val content: ByteArray = byteArrayOf(),
)

/**
 * A value this kit doesn't know, from a later Hammer, reads as [Unknown].
 */
@Serializable(with = EntryKind.Serializer::class)
public enum class EntryKind(
	/**
	 * What Hammer calls it; empty for [Unknown].
	 */
	public val `value`: String,
) {
	Person("person"),
	Place("place"),
	Thing("thing"),
	Event("event"),
	Idea("idea"),
	Unknown(""),
	;

	internal object Serializer : OperationEnumSerializer<EntryKind>("hammer.ops.EntryKind", EntryKind.entries, EntryKind.Unknown, { it.value })
}

@Serializable
public data class EntrySummary(
	public val id: Int = 0,
	public val name: String = "",
	public val type: EntryKind = EntryKind.Person,
	/**
	 * Other names the entry goes by.
	 */
	public val aliases: List<String>? = null,
)

@Serializable
public data class ExportFormat(
	public val id: String = "",
	public val fileExtension: String = "",
	public val mimeType: String = "",
)

@Serializable
public data class ExportFormats(
	public val formats: List<ExportFormat> = emptyList(),
)

@Serializable
public data class ExportedFile(
	public val fileName: String = "",
	public val mimeType: String = "",
	@Serializable(with = Base64Bytes::class)
	public val content: ByteArray = byteArrayOf(),
)

@Serializable
public data class Idea(
	public val id: String = "",
	public val title: String? = null,
	public val content: String = "",
	public val tags: List<String> = emptyList(),
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val created: String = "",
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val updated: String = "",
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val promoted: String? = null,
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val archived: String? = null,
)

@Serializable
public data class Ideas(
	public val ideas: List<Idea> = emptyList(),
)

/**
 * A value this kit doesn't know, from a later Hammer, reads as [Unknown].
 */
@Serializable(with = ImportFileFormat.Serializer::class)
public enum class ImportFileFormat(
	/**
	 * What Hammer calls it; empty for [Unknown].
	 */
	public val `value`: String,
) {
	Markdown("markdown"),
	Rtf("rtf"),
	Unknown(""),
	;

	internal object Serializer : OperationEnumSerializer<ImportFileFormat>("hammer.ops.ImportFileFormat", ImportFileFormat.entries, ImportFileFormat.Unknown, { it.value })
}

/**
 * A value this kit doesn't know, from a later Hammer, reads as [Unknown].
 */
@Serializable(with = MarkdownSplit.Serializer::class)
public enum class MarkdownSplit(
	/**
	 * What Hammer calls it; empty for [Unknown].
	 */
	public val `value`: String,
) {
	Auto("auto"),
	H1("h1"),
	H2("h2"),
	Pattern("pattern"),
	Unknown(""),
	;

	internal object Serializer : OperationEnumSerializer<MarkdownSplit>("hammer.ops.MarkdownSplit", MarkdownSplit.entries, MarkdownSplit.Unknown, { it.value })
}

@Serializable
public data class Note(
	public val id: Int = 0,
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val created: String = "",
	public val tags: List<String> = emptyList(),
	public val content: String = "",
)

@Serializable
public data class Notes(
	public val notes: List<Note> = emptyList(),
)

@Serializable
public data class OperationDescriptor(
	public val name: String = "",
	public val description: String = "",
	public val access: Access = Access.Read,
	public val scope: OperationScope = OperationScope.Content,
	public val input: Map<String, JsonElement> = emptyMap(),
	public val output: Map<String, JsonElement> = emptyMap(),
)

@Serializable
public data class OperationList(
	public val operations: List<OperationDescriptor> = emptyList(),
)

/**
 * A value this kit doesn't know, from a later Hammer, reads as [Unknown].
 */
@Serializable(with = OperationScope.Serializer::class)
public enum class OperationScope(
	/**
	 * What Hammer calls it; empty for [Unknown].
	 */
	public val `value`: String,
) {
	Content("content"),
	Account("account"),
	Unknown(""),
	;

	internal object Serializer : OperationEnumSerializer<OperationScope>("hammer.ops.OperationScope", OperationScope.entries, OperationScope.Unknown, { it.value })
}

@Serializable
public data class PagedScene(
	public val id: Int = 0,
	public val name: String = "",
	/**
	 * Names of the groups containing the scene, outermost first.
	 */
	public val path: List<String> = emptyList(),
	public val markdown: String = "",
	public val wordCount: Int = 0,
	public val meta: SceneMeta = SceneMeta(),
)

@Serializable
public data class ProjectImported(
	public val name: String = "",
	public val scenes: Int = 0,
)

@Serializable
public data class ProjectInfo(
	public val name: String = "",
	public val projectId: String = "",
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val created: String? = null,
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val lastAccessed: String? = null,
	public val authorName: String? = null,
	public val language: String? = null,
	public val tags: List<String> = emptyList(),
	public val wordCountGoal: WordGoal? = null,
	public val wordCount: Int? = null,
	public val sceneCount: Int? = null,
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val lastEdited: String? = null,
)

@Serializable
public data class ProjectList(
	public val projects: List<ProjectSummary> = emptyList(),
)

@Serializable
public data class ProjectName(
	public val name: String = "",
)

@Serializable
public data class ProjectStats(
	public val totalWords: Int = 0,
	public val sceneCount: Int = 0,
	/**
	 * Top-level scenes and groups, in story order.
	 */
	public val chapters: List<ChapterWords> = emptyList(),
	public val longestScene: SceneWords? = null,
	public val shortestSceneWords: Int = 0,
	public val medianSceneWords: Int = 0,
	public val noteCount: Int = 0,
	public val timelineEventCount: Int = 0,
	public val entryCounts: List<EntryCount> = emptyList(),
	/**
	 * People appearing in the most scenes.
	 */
	public val topAppearances: List<Appearance> = emptyList(),
	public val wordCountGoal: WordGoal? = null,
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val lastEdited: String? = null,
	public val lastEditedScene: SceneRef? = null,
)

@Serializable
public data class ProjectSummary(
	public val name: String = "",
	public val projectId: String = "",
	/**
	 * From the statistics cache; null until statistics are first calculated.
	 */
	public val wordCount: Int? = null,
	/**
	 * Null when the project's metadata can't be read. An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val created: String? = null,
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val lastAccessed: String? = null,
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val lastEdited: String? = null,
)

@Serializable
public data class ProjectSyncResult(
	public val name: String = "",
	public val outcome: SyncOutcome = SyncOutcome.Success,
)

@Serializable
public data class ProjectSyncStatus(
	public val name: String = "",
	/**
	 * True once the project has synced.
	 */
	public val linked: Boolean = false,
	/**
	 * Null when never synced. An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val lastSync: String? = null,
	public val pendingChanges: Int = 0,
)

/**
 * A value this kit doesn't know, from a later Hammer, reads as [Unknown].
 */
@Serializable(with = RtfSplit.Serializer::class)
public enum class RtfSplit(
	/**
	 * What Hammer calls it; empty for [Unknown].
	 */
	public val `value`: String,
) {
	Formatting("formatting"),
	Pattern("pattern"),
	Single("single"),
	Unknown(""),
	;

	internal object Serializer : OperationEnumSerializer<RtfSplit>("hammer.ops.RtfSplit", RtfSplit.entries, RtfSplit.Unknown, { it.value })
}

@Serializable
public data class SceneItemRef(
	public val id: Int = 0,
	public val name: String = "",
	public val kind: SceneKind = SceneKind.Scene,
)

/**
 * A value this kit doesn't know, from a later Hammer, reads as [Unknown].
 */
@Serializable(with = SceneKind.Serializer::class)
public enum class SceneKind(
	/**
	 * What Hammer calls it; empty for [Unknown].
	 */
	public val `value`: String,
) {
	Scene("scene"),
	Group("group"),
	Unknown(""),
	;

	internal object Serializer : OperationEnumSerializer<SceneKind>("hammer.ops.SceneKind", SceneKind.entries, SceneKind.Unknown, { it.value })
}

@Serializable
public data class SceneMeta(
	public val outline: String = "",
	public val notes: String = "",
	public val tags: List<String> = emptyList(),
	public val currentDraft: String = "",
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val created: String? = null,
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val lastEdited: String? = null,
	/**
	 * Entry ids confirmed as in the scene.
	 */
	public val references: List<Int> = emptyList(),
	/**
	 * Entry ids the writer dismissed, which name matching no longer adds.
	 */
	public val dismissedReferences: List<Int> = emptyList(),
)

@Serializable
public data class SceneNode(
	public val id: Int = 0,
	public val name: String = "",
	public val kind: SceneKind = SceneKind.Scene,
	public val wordCount: Int = 0,
	public val children: List<SceneNode> = emptyList(),
)

@Serializable
public data class ScenePage(
	public val scenes: List<PagedScene> = emptyList(),
	/**
	 * The last scene's id when more follow, to pass as after; null on the last page.
	 */
	public val next: Int? = null,
)

@Serializable
public data class SceneRef(
	public val id: Int = 0,
	public val name: String = "",
)

@Serializable
public data class SceneText(
	public val id: Int = 0,
	public val name: String = "",
	public val archived: Boolean = false,
	public val markdown: String = "",
	public val wordCount: Int = 0,
	public val meta: SceneMeta = SceneMeta(),
)

@Serializable
public data class SceneTree(
	public val nodes: List<SceneNode> = emptyList(),
)

@Serializable
public data class SceneWordCount(
	public val wordCount: Int = 0,
)

@Serializable
public data class SceneWords(
	public val id: Int = 0,
	public val name: String = "",
	public val words: Int = 0,
)

@Serializable
public data class SceneWriteResult(
	/**
	 * The new draft, in draft mode.
	 */
	public val draftId: Int? = null,
	/**
	 * In live mode, the draft holding the text it replaced; null when the text was unchanged.
	 */
	public val previousDraftId: Int? = null,
	public val wordCount: Int = 0,
)

/**
 * A value this kit doesn't know, from a later Hammer, reads as [Unknown].
 */
@Serializable(with = SearchFilter.Serializer::class)
public enum class SearchFilter(
	/**
	 * What Hammer calls it; empty for [Unknown].
	 */
	public val `value`: String,
) {
	All("all"),
	Scenes("scenes"),
	Notes("notes"),
	Encyclopedia("encyclopedia"),
	Timeline("timeline"),
	Unknown(""),
	;

	internal object Serializer : OperationEnumSerializer<SearchFilter>("hammer.ops.SearchFilter", SearchFilter.entries, SearchFilter.Unknown, { it.value })
}

@Serializable
public data class SearchHit(
	public val kind: EntityKind = EntityKind.Scene,
	public val id: Int = 0,
	public val title: String = "",
	public val snippet: String = "",
	/**
	 * The match's range within [snippet].
	 */
	public val matchStart: Int = 0,
	public val matchEnd: Int = 0,
)

@Serializable
public data class SearchResults(
	public val results: List<SearchHit> = emptyList(),
)

@Serializable
public data class Session(
	public val device: String = "",
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val startedAt: String = "",
	/**
	 * An ISO 8601 instant, such as `2026-03-01T09:30:00Z`.
	 */
	public val endedAt: String = "",
	public val words: Int = 0,
)

@Serializable
public data class Sessions(
	public val sessions: List<Session> = emptyList(),
)

/**
 * A value this kit doesn't know, from a later Hammer, reads as [Unknown].
 */
@Serializable(with = SyncOutcome.Serializer::class)
public enum class SyncOutcome(
	/**
	 * What Hammer calls it; empty for [Unknown].
	 */
	public val `value`: String,
) {
	Success("success"),
	Unchanged("unchanged"),
	NeedsResolution("needs_resolution"),
	Failed("failed"),
	Skipped("skipped"),
	Unknown(""),
	;

	internal object Serializer : OperationEnumSerializer<SyncOutcome>("hammer.ops.SyncOutcome", SyncOutcome.entries, SyncOutcome.Unknown, { it.value })
}

@Serializable
public data class SyncRunResult(
	public val accountSynced: Boolean = false,
	public val ideasSynced: Boolean = false,
	public val projects: List<ProjectSyncResult> = emptyList(),
	public val log: List<String> = emptyList(),
)

@Serializable
public data class SyncStatus(
	public val projects: List<ProjectSyncStatus> = emptyList(),
)

@Serializable
public data class TagCount(
	public val tag: String = "",
	public val count: Int = 0,
)

@Serializable
public data class TaggedEntities(
	public val entities: List<TaggedEntity> = emptyList(),
)

@Serializable
public data class TaggedEntity(
	public val kind: EntityKind = EntityKind.Scene,
	public val id: Int = 0,
)

@Serializable
public data class Tags(
	public val tags: List<TagCount> = emptyList(),
)

@Serializable
public data class Timeline(
	public val events: List<TimelineEntry> = emptyList(),
)

@Serializable
public data class TimelineEntry(
	public val id: Int = 0,
	public val order: Int = 0,
	/**
	 * Free text; timelines are not bound to a calendar.
	 */
	public val date: String? = null,
	public val content: String = "",
	public val tags: List<String> = emptyList(),
)

@Serializable
public data class WordGoal(
	public val cadence: Cadence = Cadence.Day,
	public val count: Int = 0,
)

/**
 * A value this kit doesn't know, from a later Hammer, reads as [Unknown].
 */
@Serializable(with = WriteMode.Serializer::class)
public enum class WriteMode(
	/**
	 * What Hammer calls it; empty for [Unknown].
	 */
	public val `value`: String,
) {
	Draft("draft"),
	Live("live"),
	Unknown(""),
	;

	internal object Serializer : OperationEnumSerializer<WriteMode>("hammer.ops.WriteMode", WriteMode.entries, WriteMode.Unknown, { it.value })
}
