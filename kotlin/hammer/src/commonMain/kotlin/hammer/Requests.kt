package hammer

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/** A request carrying the plugin's settings: each declared setting's current value. */
interface WithSettings {
	val settings: JsonObject
}

/** The settings read into a `T`, a `@Serializable` class with a property per setting. */
inline fun <reified T> WithSettings.settings(): T = json.decodeFromJsonElement(settings)

/** What the `export` export is given: one of the manifest's export formats, and the story. */
@Serializable
class ExportRequest(
	val format: String,
	val projectName: String,
	val language: String = "en",
	val topLevelAsChapters: Boolean = true,
	val chapters: List<Chapter> = emptyList(),
	override val settings: JsonObject = JsonObject(emptyMap()),
	/** The UI's BCP 47 tag, such as `fr-CA`, for replying in the user's language; null when unknown. */
	val locale: String? = null,
) : WithSettings {
	/**
	 * A chapter's scenes: as markdown in [scenes], or, for a format whose manifest entry sets
	 * `input = "prose"`, as Hammer's parsed blocks in [prose].
	 */
	@Serializable
	class Chapter(
		val name: String,
		val scenes: List<String> = emptyList(),
		val prose: List<JsonElement> = emptyList(),
	)
}

/** What the `action` export is given: one of the manifest's `[[actions]]`, run from a screen of a project. */
@Serializable
class ActionRequest(
	val action: String,
	val project: String,
	override val settings: JsonObject = JsonObject(emptyMap()),
	/** A value for each of the action's `[[actions.field]]`s. */
	val input: JsonObject = JsonObject(emptyMap()),
	val context: Context = Context(),
	/** The id of the button pressed, for an interactive action's later calls; null on the first. */
	val button: String? = null,
	/** The UI's BCP 47 tag, such as `fr-CA`, for replying in the user's language; null when unknown. */
	val locale: String? = null,
) : WithSettings {
	/** The screen it was run from (`project`, `scene`, `note`, `entry`, or `event`) and the id of the item shown there. */
	@Serializable
	class Context(val place: String = "project", val id: Int? = null)
}

/** The action's input read into a `T`, a `@Serializable` class with a property per field. */
inline fun <reified T> ActionRequest.input(): T = json.decodeFromJsonElement(input)

/**
 * What an `interactive` action returns: [markdown] shown with [buttons] under it, each calling the
 * action again with its id as [ActionRequest.button], and a [message] shown briefly. No markdown
 * closes the dialog.
 */
@Serializable
class ActionReply(
	val markdown: String? = null,
	val buttons: List<Button> = emptyList(),
	val message: String? = null,
) {
	/** A button; one with [footer] sits in the dialog's footer, for acting on the whole reply, such as a fresh list. */
	@Serializable
	class Button(val id: String, val label: String, val footer: Boolean = false)

	/** What an `interactive` action's `action` export returns. */
	fun toJson(): String = json.encodeToString(serializer(), this)
}

/** What the `diagnose` export is given: paragraphs of plain text to check for one of the manifest's `[[diagnostics]]`. */
@Serializable
class DiagnoseRequest(
	val diagnostics: String,
	val paragraphs: List<String>,
	/** The project's BCP 47 tag, or null when it has none. */
	val language: String? = null,
	override val settings: JsonObject = JsonObject(emptyMap()),
	/** The UI's BCP 47 tag, such as `fr-CA`, for replying in the user's language; null when unknown. */
	val locale: String? = null,
) : WithSettings

/**
 * An issue in paragraph [paragraph] from [start] to [end], indexes into that paragraph's String. The
 * kit turns them into the UTF-8 offsets Hammer reads.
 */
class Diagnostic(
	val paragraph: Int,
	val start: Int,
	val end: Int,
	val message: String,
	val fixes: List<Fix> = emptyList(),
)

/** Text to put in place of a [Diagnostic]'s range, shown as [label] where the text alone would not say what it does. */
class Fix(val replacement: String, val label: String? = null)

/** What the `command` export is given: a line of standard input to one of the manifest's `[[commands]]`. */
@Serializable
class CommandRequest(
	val command: String,
	val args: List<String> = emptyList(),
	val line: String,
	override val settings: JsonObject = JsonObject(emptyMap()),
	/** The UI's BCP 47 tag, such as `fr-CA`, for replying in the user's language; null when unknown. */
	val locale: String? = null,
) : WithSettings
