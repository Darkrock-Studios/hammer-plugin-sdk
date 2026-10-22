// Generated from api/operations.json by kotlin/codegen: regenerate rather than edit.
@file:Suppress("RedundantVisibilityModifier", "unused")

package hammer.ops

import hammer.Base64Bytes
import hammer.operation
import hammer.operationValue
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlinx.serialization.json.buildJsonObject

/**
 * Hammer's operations, typed. Each function runs one operation, as [hammer.call] would, and throws
 * [hammer.OperationException] when it fails. A plugin may call only those its manifest's
 * `[permissions]` ask for: each function's documentation names what it needs.
 */
public object Hammer {
	/**
	 * The `account` operations.
	 */
	public val account: AccountOperations = AccountOperations

	/**
	 * The `backup` operations.
	 */
	public val backup: BackupOperations = BackupOperations

	/**
	 * The `draft` operations.
	 */
	public val draft: DraftOperations = DraftOperations

	/**
	 * The `entry` operations.
	 */
	public val entry: EntryOperations = EntryOperations

	/**
	 * The `export` operations.
	 */
	public val export: ExportOperations = ExportOperations

	/**
	 * The `idea` operations.
	 */
	public val idea: IdeaOperations = IdeaOperations

	/**
	 * The `note` operations.
	 */
	public val note: NoteOperations = NoteOperations

	/**
	 * The `ops` operations.
	 */
	public val ops: OpsOperations = OpsOperations

	/**
	 * The `project` operations.
	 */
	public val project: ProjectOperations = ProjectOperations

	/**
	 * The `scene` operations.
	 */
	public val scene: SceneOperations = SceneOperations

	/**
	 * The `stats` operations.
	 */
	public val stats: StatsOperations = StatsOperations

	/**
	 * The `sync` operations.
	 */
	public val sync: SyncOperations = SyncOperations

	/**
	 * The `tag` operations.
	 */
	public val tag: TagOperations = TagOperations

	/**
	 * The `timeline` operations.
	 */
	public val timeline: TimelineOperations = TimelineOperations

	/**
	 * Search a project's scenes, notes, encyclopedia, and timeline. #tag in the query filters by tag.
	 *
	 * Needs `search` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun search(
		project: String,
		query: String,
		filter: SearchFilter? = null,
	): SearchResults = operation("search", buildJsonObject {
		put("project", operationValue(project))
		put("query", operationValue(query))
		if (filter != null) put("filter", operationValue(filter))
	})
}

public object AccountOperations {
	/**
	 * Log in to a sync server with an existing account. Accounts are created in the app.
	 *
	 * Needs `account.login` or `account:write` in the manifest's `[permissions]`.
	 * @param url The server's address, with `https://` or `http://` optional.
	 */
	public fun login(
		url: String,
		email: String,
		password: String,
	): AccountStatus = operation("account.login", buildJsonObject {
		put("url", operationValue(url))
		put("email", operationValue(email))
		put("password", operationValue(password))
	})

	/**
	 * Forget the sync server and every project's sync state for it, as removing the server in Settings does.
	 *
	 * Needs `account.logout` or `account:write` in the manifest's `[permissions]`.
	 */
	public fun logout(): AccountStatus = operation("account.logout", buildJsonObject {
	})

	/**
	 * The sync server and account this install uses, and optionally whether the server still accepts it.
	 *
	 * Needs `account.status` or `account:read` in the manifest's `[permissions]`.
	 * @param check Ask the server whether the stored token is still good; needs the network.
	 */
	public fun status(check: Boolean? = null): AccountStatus = operation("account.status", buildJsonObject {
		if (check != null) put("check", operationValue(check))
	})
}

public object BackupOperations {
	/**
	 * Back up a project's saved files.
	 *
	 * Needs `backup.create` or `content:write` in the manifest's `[permissions]`.
	 * @param project The project's name, or its server project id.
	 */
	public fun create(project: String): Backup = operation("backup.create", buildJsonObject {
		put("project", operationValue(project))
	})

	/**
	 * A project's backups, oldest first.
	 *
	 * Needs `backup.list` or `content:read` in the manifest's `[permissions]`.
	 * @param project The project's name, or its server project id.
	 */
	public fun list(project: String): Backups = operation("backup.list", buildJsonObject {
		put("project", operationValue(project))
	})
}

public object DraftOperations {
	/**
	 * Replace a scene's text with one of its drafts, saving the replaced text as a draft first.
	 *
	 * Needs `draft.apply` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun apply(project: String, id: Int): DraftApplied = operation("draft.apply", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})

	/**
	 * Save a scene's current text as a named draft.
	 *
	 * Needs `draft.create` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun create(
		project: String,
		sceneId: Int,
		name: String,
	): DraftInfo = operation("draft.create", buildJsonObject {
		put("project", operationValue(project))
		put("sceneId", operationValue(sceneId))
		put("name", operationValue(name))
	})

	/**
	 * Delete a draft for good.
	 *
	 * Needs `draft.delete` (by name) in the manifest's `[permissions]`.
	 */
	public fun delete(project: String, id: Int): DraftInfo = operation("draft.delete", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})

	/**
	 * A scene's saved drafts, oldest first.
	 *
	 * Needs `draft.list` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun list(project: String, sceneId: Int): Drafts = operation("draft.list", buildJsonObject {
		put("project", operationValue(project))
		put("sceneId", operationValue(sceneId))
	})

	/**
	 * A draft's markdown.
	 *
	 * Needs `draft.read` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun read(project: String, id: Int): DraftText = operation("draft.read", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})
}

public object EntryOperations {
	/**
	 * The `entry.image` operations.
	 */
	public val image: EntryImageOperations = EntryImageOperations

	/**
	 * Add an encyclopedia entry to a project.
	 *
	 * Needs `entry.create` or `content:write` in the manifest's `[permissions]`.
	 * @param aliases Other names the entry goes by, matched in scenes like its name.
	 */
	public fun create(
		project: String,
		name: String,
		type: EntryKind,
		text: String,
		tags: List<String>? = null,
		aliases: List<String>? = null,
	): Entry = operation("entry.create", buildJsonObject {
		put("project", operationValue(project))
		put("name", operationValue(name))
		put("type", operationValue(type))
		put("text", operationValue(text))
		if (tags != null) put("tags", operationValue(tags))
		if (aliases != null) put("aliases", operationValue(aliases))
	})

	/**
	 * Delete an encyclopedia entry, and its image, for good.
	 *
	 * Needs `entry.delete` (by name) in the manifest's `[permissions]`.
	 */
	public fun delete(project: String, id: Int): EntrySummary = operation("entry.delete", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})

	/**
	 * A project's encyclopedia entries, with their aliases, optionally only one type or those with a tag.
	 *
	 * Needs `entry.list` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun list(
		project: String,
		type: EntryKind? = null,
		tag: String? = null,
	): Entries = operation("entry.list", buildJsonObject {
		put("project", operationValue(project))
		if (type != null) put("type", operationValue(type))
		if (tag != null) put("tag", operationValue(tag))
	})

	/**
	 * An encyclopedia entry, with the scenes confirmed to reference it.
	 *
	 * Needs `entry.read` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun read(project: String, id: Int): Entry = operation("entry.read", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})

	/**
	 * Replace an encyclopedia entry's text, and its name, tags, or aliases when given.
	 *
	 * Needs `entry.update` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun update(
		project: String,
		id: Int,
		text: String,
		name: String? = null,
		tags: List<String>? = null,
		aliases: List<String>? = null,
	): Entry = operation("entry.update", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
		put("text", operationValue(text))
		if (name != null) put("name", operationValue(name))
		if (tags != null) put("tags", operationValue(tags))
		if (aliases != null) put("aliases", operationValue(aliases))
	})
}

public object EntryImageOperations {
	/**
	 * An encyclopedia entry's image.
	 *
	 * Needs `entry.image.get` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun `get`(project: String, id: Int): EntryImage = operation("entry.image.get", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})

	/**
	 * Remove an encyclopedia entry's image.
	 *
	 * Needs `entry.image.remove` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun remove(project: String, id: Int): Entry = operation("entry.image.remove", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})

	/**
	 * Set an encyclopedia entry's image, replacing any it has. The extension is jpg, jpeg, png, or webp, and must match the image.
	 *
	 * Needs `entry.image.set` or `content:write` in the manifest's `[permissions]`.
	 * @param extension jpg, jpeg, png, or webp.
	 */
	public fun `set`(
		project: String,
		id: Int,
		extension: String,
		content: ByteArray,
	): Entry = operation("entry.image.set", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
		put("extension", operationValue(extension))
		put("content", operationValue(content, Base64Bytes))
	})
}

public object ExportOperations {
	/**
	 * The formats project.export can produce.
	 *
	 * Needs `export.formats` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun formats(): ExportFormats = operation("export.formats", buildJsonObject {
	})
}

public object IdeaOperations {
	/**
	 * Archive a story idea.
	 *
	 * Needs `idea.archive` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun archive(id: String): Idea = operation("idea.archive", buildJsonObject {
		put("id", operationValue(id))
	})

	/**
	 * Add a story idea.
	 *
	 * Needs `idea.create` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun create(
		content: String,
		title: String? = null,
		tags: List<String>? = null,
	): Idea = operation("idea.create", buildJsonObject {
		put("content", operationValue(content))
		if (title != null) put("title", operationValue(title))
		if (tags != null) put("tags", operationValue(tags))
	})

	/**
	 * Delete a story idea for good.
	 *
	 * Needs `idea.delete` (by name) in the manifest's `[permissions]`.
	 */
	public fun delete(id: String): Idea = operation("idea.delete", buildJsonObject {
		put("id", operationValue(id))
	})

	/**
	 * Story ideas, which belong to the account rather than a project.
	 *
	 * Needs `idea.list` or `content:read` in the manifest's `[permissions]`.
	 * @param archived True for only archived ideas, false for only active ones; omitted for both.
	 */
	public fun list(archived: Boolean? = null): Ideas = operation("idea.list", buildJsonObject {
		if (archived != null) put("archived", operationValue(archived))
	})

	/**
	 * Return an archived story idea to the active ones.
	 *
	 * Needs `idea.unarchive` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun unarchive(id: String): Idea = operation("idea.unarchive", buildJsonObject {
		put("id", operationValue(id))
	})

	/**
	 * Replace a story idea's text, and its title or tags when given. An empty title clears it.
	 *
	 * Needs `idea.update` or `content:write` in the manifest's `[permissions]`.
	 * @param title Left out, unchanged; empty, cleared.
	 */
	public fun update(
		id: String,
		content: String,
		title: String? = null,
		tags: List<String>? = null,
	): Idea = operation("idea.update", buildJsonObject {
		put("id", operationValue(id))
		put("content", operationValue(content))
		if (title != null) put("title", operationValue(title))
		if (tags != null) put("tags", operationValue(tags))
	})
}

public object NoteOperations {
	/**
	 * Add a note to a project.
	 *
	 * Needs `note.create` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun create(
		project: String,
		content: String,
		tags: List<String>? = null,
	): Note = operation("note.create", buildJsonObject {
		put("project", operationValue(project))
		put("content", operationValue(content))
		if (tags != null) put("tags", operationValue(tags))
	})

	/**
	 * Delete a note for good.
	 *
	 * Needs `note.delete` (by name) in the manifest's `[permissions]`.
	 */
	public fun delete(project: String, id: Int): Note = operation("note.delete", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})

	/**
	 * A project's notes, optionally only those with a tag.
	 *
	 * Needs `note.list` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun list(project: String, tag: String? = null): Notes = operation("note.list", buildJsonObject {
		put("project", operationValue(project))
		if (tag != null) put("tag", operationValue(tag))
	})

	/**
	 * One note.
	 *
	 * Needs `note.read` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun read(project: String, id: Int): Note = operation("note.read", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})

	/**
	 * Replace a note's text, and its tags when given.
	 *
	 * Needs `note.update` or `content:write` in the manifest's `[permissions]`.
	 * @param tags Left out, the tags stay as they are.
	 */
	public fun update(
		project: String,
		id: Int,
		content: String,
		tags: List<String>? = null,
	): Note = operation("note.update", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
		put("content", operationValue(content))
		if (tags != null) put("tags", operationValue(tags))
	})
}

public object OpsOperations {
	/**
	 * Every operation you may call, with JSON schemas for its input and output.
	 *
	 * Needs `ops.list` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun list(): OperationList = operation("ops.list", buildJsonObject {
	})
}

public object ProjectOperations {
	/**
	 * Create an empty project.
	 *
	 * Needs `project.create` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun create(name: String): ProjectName = operation("project.create", buildJsonObject {
		put("name", operationValue(name))
	})

	/**
	 * Delete a project, and everything in it, for good. It must not be open in Hammer.
	 *
	 * Needs `project.delete` (by name) in the manifest's `[permissions]`.
	 * @param project The project's name, or its server project id.
	 */
	public fun delete(project: String): ProjectName = operation("project.delete", buildJsonObject {
		put("project", operationValue(project))
	})

	/**
	 * Export a project, or some of its scenes, in one of the formats from export.formats.
	 *
	 * Needs `project.export` or `content:read` in the manifest's `[permissions]`.
	 * @param format An id from export.formats.
	 * @param sceneIds Limits the export to these scenes; omitted exports the whole story.
	 */
	public fun export(
		project: String,
		format: String,
		sceneIds: List<Int>? = null,
		treatTopLevelAsChapters: Boolean? = null,
	): ExportedFile = operation("project.export", buildJsonObject {
		put("project", operationValue(project))
		put("format", operationValue(format))
		if (sceneIds != null) put("sceneIds", operationValue(sceneIds))
		if (treatTopLevelAsChapters != null) put("treatTopLevelAsChapters", operationValue(treatTopLevelAsChapters))
	})

	/**
	 * Create a project from a Markdown or RTF manuscript, split into scenes by its headings or a chapter pattern.
	 *
	 * Needs `project.import` or `content:write` in the manifest's `[permissions]`.
	 * @param name Left out, the title the file gives itself.
	 * @param chapterPattern A regular expression for the lines that start a chapter; left out, chapter, part, prologue, and epilogue.
	 * @param chapterGroups Put each chapter's scenes in a group.
	 */
	public fun `import`(
		format: ImportFileFormat,
		content: ByteArray,
		name: String? = null,
		markdownSplit: MarkdownSplit? = null,
		rtfSplit: RtfSplit? = null,
		chapterPattern: String? = null,
		chapterGroups: Boolean? = null,
	): ProjectImported = operation("project.import", buildJsonObject {
		put("format", operationValue(format))
		put("content", operationValue(content, Base64Bytes))
		if (name != null) put("name", operationValue(name))
		if (markdownSplit != null) put("markdownSplit", operationValue(markdownSplit))
		if (rtfSplit != null) put("rtfSplit", operationValue(rtfSplit))
		if (chapterPattern != null) put("chapterPattern", operationValue(chapterPattern))
		if (chapterGroups != null) put("chapterGroups", operationValue(chapterGroups))
	})

	/**
	 * A project's details: author, language, tags, word count goal, cached totals, and sync linkage.
	 *
	 * Needs `project.info` or `content:read` in the manifest's `[permissions]`.
	 * @param project The project's name, or its server project id.
	 */
	public fun info(project: String): ProjectInfo = operation("project.info", buildJsonObject {
		put("project", operationValue(project))
	})

	/**
	 * List every project with its word count and when it was last edited.
	 *
	 * Needs `project.list` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun list(): ProjectList = operation("project.list", buildJsonObject {
	})

	/**
	 * Rename a project. It must not be open in Hammer.
	 *
	 * Needs `project.rename` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun rename(project: String, name: String): ProjectName = operation("project.rename", buildJsonObject {
		put("project", operationValue(project))
		put("name", operationValue(name))
	})
}

public object SceneOperations {
	/**
	 * The `scene.meta` operations.
	 */
	public val meta: SceneMetaOperations = SceneMetaOperations

	/**
	 * The `scene.read` operations.
	 */
	public val read: SceneReadOperations = SceneReadOperations

	/**
	 * The `scene.references` operations.
	 */
	public val references: SceneReferencesOperations = SceneReferencesOperations

	/**
	 * Add markdown to the end of a scene's text, as a new paragraph.
	 *
	 * Needs `scene.append` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun append(
		project: String,
		id: Int,
		markdown: String,
	): SceneWordCount = operation("scene.append", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
		put("markdown", operationValue(markdown))
	})

	/**
	 * Archive a scene: take it out of the story, keeping its text, drafts, and notes.
	 *
	 * Needs `scene.archive` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun archive(project: String, id: Int): SceneItemRef = operation("scene.archive", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})

	/**
	 * A project's archived scenes.
	 *
	 * Needs `scene.archived` or `content:read` in the manifest's `[permissions]`.
	 * @param project The project's name, or its server project id.
	 */
	public fun archived(project: String): ArchivedScenes = operation("scene.archived", buildJsonObject {
		put("project", operationValue(project))
	})

	/**
	 * Create an empty scene or group, at the end of its parent unless an index is given.
	 *
	 * Needs `scene.create` or `content:write` in the manifest's `[permissions]`.
	 * @param parentId The group to create it in; left out, the top level.
	 * @param index Its position among its siblings; left out, the end.
	 */
	public fun create(
		project: String,
		name: String,
		kind: SceneKind? = null,
		parentId: Int? = null,
		index: Int? = null,
	): SceneItemRef = operation("scene.create", buildJsonObject {
		put("project", operationValue(project))
		put("name", operationValue(name))
		if (kind != null) put("kind", operationValue(kind))
		if (parentId != null) put("parentId", operationValue(parentId))
		if (index != null) put("index", operationValue(index))
	})

	/**
	 * Delete a scene, archived or not, or an empty group, for good.
	 *
	 * Needs `scene.delete` (by name) in the manifest's `[permissions]`.
	 */
	public fun delete(project: String, id: Int): SceneItemRef = operation("scene.delete", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})

	/**
	 * Move a scene or group to a position within a group, or the top level when parentId is left out. index is its position there after the move.
	 *
	 * Needs `scene.move` or `content:write` in the manifest's `[permissions]`.
	 * @param parentId The group to move into; left out, the top level.
	 */
	public fun move(
		project: String,
		id: Int,
		index: Int,
		parentId: Int? = null,
	): SceneItemRef = operation("scene.move", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
		put("index", operationValue(index))
		if (parentId != null) put("parentId", operationValue(parentId))
	})

	/**
	 * A scene's markdown, including unsaved edits, with its outline, notes, and tags.
	 *
	 * Needs `scene.read` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun read(project: String, id: Int): SceneText = operation("scene.read", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})

	/**
	 * Rename a scene or group.
	 *
	 * Needs `scene.rename` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun rename(
		project: String,
		id: Int,
		name: String,
	): SceneItemRef = operation("scene.rename", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
		put("name", operationValue(name))
	})

	/**
	 * A project's scenes and groups in story order, with word counts.
	 *
	 * Needs `scene.tree` or `content:read` in the manifest's `[permissions]`.
	 * @param project The project's name, or its server project id.
	 */
	public fun tree(project: String): SceneTree = operation("scene.tree", buildJsonObject {
		put("project", operationValue(project))
	})

	/**
	 * Return an archived scene to the story, at the end of the top level.
	 *
	 * Needs `scene.unarchive` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun unarchive(project: String, id: Int): SceneItemRef = operation("scene.unarchive", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})

	/**
	 * Replace a scene's text with markdown. Mode draft saves it as a new draft of the scene and leaves the scene alone. Mode live replaces the scene's text, saving the old text as a draft first.
	 *
	 * Needs `scene.write` or `content:write` in the manifest's `[permissions]`.
	 * @param draftName The new draft's name, in draft mode only.
	 */
	public fun write(
		project: String,
		id: Int,
		mode: WriteMode,
		markdown: String,
		draftName: String? = null,
	): SceneWriteResult = operation("scene.write", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
		put("mode", operationValue(mode))
		put("markdown", operationValue(markdown))
		if (draftName != null) put("draftName", operationValue(draftName))
	})
}

public object SceneMetaOperations {
	/**
	 * A scene's outline, notes, tags, and references: the entries confirmed as in it, and those dismissed.
	 *
	 * Needs `scene.meta.read` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun read(project: String, id: Int): SceneMeta = operation("scene.meta.read", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})

	/**
	 * Change a scene's outline, notes, or tags. Fields left out keep their value.
	 *
	 * Needs `scene.meta.write` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun write(
		project: String,
		id: Int,
		outline: String? = null,
		notes: String? = null,
		tags: List<String>? = null,
	): SceneMeta = operation("scene.meta.write", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
		if (outline != null) put("outline", operationValue(outline))
		if (notes != null) put("notes", operationValue(notes))
		if (tags != null) put("tags", operationValue(tags))
	})
}

public object SceneReadOperations {
	/**
	 * Scenes in story order with their markdown, metadata, and the groups they sit in, a page of about maxWords, counting outlines and notes, at a time: every scene, a group's, or those in ids. Archived scenes are left out. Pass next as after for the following page; it is null on the last.
	 *
	 * Needs `scene.read.many` or `content:read` in the manifest's `[permissions]`.
	 * @param group A group whose scenes, at any depth, to read.
	 * @param after The previous page's next.
	 */
	public fun many(
		project: String,
		group: Int? = null,
		ids: List<Int>? = null,
		after: Int? = null,
		maxWords: Int? = null,
	): ScenePage = operation("scene.read.many", buildJsonObject {
		put("project", operationValue(project))
		if (group != null) put("group", operationValue(group))
		if (ids != null) put("ids", operationValue(ids))
		if (after != null) put("after", operationValue(after))
		if (maxWords != null) put("maxWords", operationValue(maxWords))
	})
}

public object SceneReferencesOperations {
	/**
	 * Change which entries a scene references. confirm marks entries as in the scene, and un-dismisses them; dismiss marks entries as only mentioned, so name matching stops adding them; clear forgets entries either way, so a later match can add them again.
	 *
	 * Needs `scene.references.set` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun `set`(
		project: String,
		id: Int,
		confirm: List<Int>? = null,
		dismiss: List<Int>? = null,
		clear: List<Int>? = null,
	): SceneMeta = operation("scene.references.set", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
		if (confirm != null) put("confirm", operationValue(confirm))
		if (dismiss != null) put("dismiss", operationValue(dismiss))
		if (clear != null) put("clear", operationValue(clear))
	})
}

public object StatsOperations {
	/**
	 * Words written per day over a date range, plus lifetime words per device.
	 *
	 * Needs `stats.activity` or `content:read` in the manifest's `[permissions]`.
	 * @param from First day included, in the device's time zone; omitted for no lower bound. An ISO 8601 date, such as `2026-03-01`.
	 * @param to Last day included; omitted for no upper bound. An ISO 8601 date, such as `2026-03-01`.
	 */
	public fun activity(
		project: String,
		from: String? = null,
		to: String? = null,
	): Activity = operation("stats.activity", buildJsonObject {
		put("project", operationValue(project))
		if (from != null) put("from", operationValue(from))
		if (to != null) put("to", operationValue(to))
	})

	/**
	 * A project's statistics, as its Home screen shows them.
	 *
	 * Needs `stats.project` or `content:read` in the manifest's `[permissions]`.
	 * @param recalculate Recalculate even when the cached statistics are current.
	 */
	public fun project(project: String, recalculate: Boolean? = null): ProjectStats = operation("stats.project", buildJsonObject {
		put("project", operationValue(project))
		if (recalculate != null) put("recalculate", operationValue(recalculate))
	})

	/**
	 * Writing sessions over a date range, from every device.
	 *
	 * Needs `stats.sessions` or `content:read` in the manifest's `[permissions]`.
	 * @param from First day included, in the device's time zone; omitted for no lower bound. An ISO 8601 date, such as `2026-03-01`.
	 * @param to Last day included; omitted for no upper bound. An ISO 8601 date, such as `2026-03-01`.
	 */
	public fun sessions(
		project: String,
		from: String? = null,
		to: String? = null,
	): Sessions = operation("stats.sessions", buildJsonObject {
		put("project", operationValue(project))
		if (from != null) put("from", operationValue(from))
		if (to != null) put("to", operationValue(to))
	})
}

public object SyncOperations {
	/**
	 * Sync the account and every linked project with the server, as Sync all does. A project with a conflict is left for the app.
	 *
	 * Needs `sync.run` or `account:write` in the manifest's `[permissions]`.
	 * @param project Sync only this project; account changes and ideas still sync.
	 */
	public fun run(project: String? = null, onConflict: ConflictChoice? = null): SyncRunResult = operation("sync.run", buildJsonObject {
		if (project != null) put("project", operationValue(project))
		if (onConflict != null) put("onConflict", operationValue(onConflict))
	})

	/**
	 * Each project's link to the sync server, its last sync, and how many of its changes are waiting to sync.
	 *
	 * Needs `sync.status` or `account:read` in the manifest's `[permissions]`.
	 */
	public fun status(): SyncStatus = operation("sync.status", buildJsonObject {
	})
}

public object TagOperations {
	/**
	 * Every scene, note, entry, and timeline event carrying a tag.
	 *
	 * Needs `tag.find` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun find(project: String, tag: String): TaggedEntities = operation("tag.find", buildJsonObject {
		put("project", operationValue(project))
		put("tag", operationValue(tag))
	})

	/**
	 * The tags used in a project, most used first, optionally counting one kind of entity.
	 *
	 * Needs `tag.list` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun list(
		project: String,
		kind: EntityKind? = null,
		limit: Int? = null,
	): Tags = operation("tag.list", buildJsonObject {
		put("project", operationValue(project))
		if (kind != null) put("kind", operationValue(kind))
		if (limit != null) put("limit", operationValue(limit))
	})
}

public object TimelineOperations {
	/**
	 * Add an event to a project's timeline, at the end unless an index is given.
	 *
	 * Needs `timeline.create` or `content:write` in the manifest's `[permissions]`.
	 * @param date Free text, such as "Day 3" or "Spring, 1802".
	 * @param index Its position in the timeline; left out, the end.
	 */
	public fun create(
		project: String,
		content: String,
		date: String? = null,
		tags: List<String>? = null,
		index: Int? = null,
	): TimelineEntry = operation("timeline.create", buildJsonObject {
		put("project", operationValue(project))
		put("content", operationValue(content))
		if (date != null) put("date", operationValue(date))
		if (tags != null) put("tags", operationValue(tags))
		if (index != null) put("index", operationValue(index))
	})

	/**
	 * Delete a timeline event for good.
	 *
	 * Needs `timeline.delete` (by name) in the manifest's `[permissions]`.
	 */
	public fun delete(project: String, id: Int): TimelineEntry = operation("timeline.delete", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})

	/**
	 * A project's timeline events in order.
	 *
	 * Needs `timeline.list` or `content:read` in the manifest's `[permissions]`.
	 * @param project The project's name, or its server project id.
	 */
	public fun list(project: String): Timeline = operation("timeline.list", buildJsonObject {
		put("project", operationValue(project))
	})

	/**
	 * Move a timeline event to a position; index is its position after the move.
	 *
	 * Needs `timeline.move` or `content:write` in the manifest's `[permissions]`.
	 */
	public fun move(
		project: String,
		id: Int,
		index: Int,
	): TimelineEntry = operation("timeline.move", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
		put("index", operationValue(index))
	})

	/**
	 * One timeline event.
	 *
	 * Needs `timeline.read` or `content:read` in the manifest's `[permissions]`.
	 */
	public fun read(project: String, id: Int): TimelineEntry = operation("timeline.read", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
	})

	/**
	 * Replace a timeline event's text, and its date or tags when given. An empty date clears it.
	 *
	 * Needs `timeline.update` or `content:write` in the manifest's `[permissions]`.
	 * @param date Left out, unchanged; empty, cleared.
	 */
	public fun update(
		project: String,
		id: Int,
		content: String,
		date: String? = null,
		tags: List<String>? = null,
	): TimelineEntry = operation("timeline.update", buildJsonObject {
		put("project", operationValue(project))
		put("id", operationValue(id))
		put("content", operationValue(content))
		if (date != null) put("date", operationValue(date))
		if (tags != null) put("tags", operationValue(tags))
	})
}
