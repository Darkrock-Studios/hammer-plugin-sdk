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

/** What the `action` export is given: one of the manifest's `[[actions]]`, run on a project. */
@Serializable
class ActionRequest(
	val action: String,
	val project: String,
	override val settings: JsonObject = JsonObject(emptyMap()),
) : WithSettings

/** What the `command` export is given: a line of standard input to one of the manifest's `[[commands]]`. */
@Serializable
class CommandRequest(
	val command: String,
	val args: List<String> = emptyList(),
	val line: String,
	override val settings: JsonObject = JsonObject(emptyMap()),
) : WithSettings
