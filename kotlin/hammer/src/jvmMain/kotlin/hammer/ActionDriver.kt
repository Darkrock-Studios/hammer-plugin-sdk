package hammer

import java.io.File
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.longOrNull
import net.peanuuutz.tomlkt.Toml
import net.peanuuutz.tomlkt.TomlArray
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.TomlNull
import net.peanuuutz.tomlkt.TomlTable

/**
 * Runs an action's handler in a JVM test as Hammer runs it. [open] shows the manifest's
 * `[[actions.field]]`s, read from the plugin's `manifest.toml` unless [fields] are given, or calls at once
 * when there are none. [fill] sets a field, [run] presses Run on the manifest's page, and [press] presses
 * a page's button. Before calling, Hammer's own checks run (required, an int's bounds, a choice's
 * options) unless the button has `validate = false`; what they find is in [errors], and the handler is not
 * called. Each call carries the page's values and its state, and its reply is applied as Hammer would:
 * errors keep the page, its values, and its state; a page replaces the one showing; and `open`, or a
 * reply with none of these, ends the action. A reply Hammer would refuse, such as one with state over
 * 1 MiB, throws.
 *
 *     val wave = ActionDriver("wave", ::wave).open()
 *     wave.fill("name", "Mara").press("next")
 *     assertEquals("Mara", wave.state<Wave>()?.name)
 */
class ActionDriver(
	private val action: String,
	private val handle: (ActionRequest) -> ActionReply?,
	private val project: String = "Project",
	private val settings: JsonObject = JsonObject(emptyMap()),
	private val context: ActionRequest.Context = ActionRequest.Context(),
	private val locale: String? = null,
	private val fields: List<ActionReply.Field> = manifestFields(action),
) {
	/** The page showing, or null before [open] and once the action has ended. */
	var page: ActionReply.Page? = null
		private set

	private val _values = mutableMapOf<String, JsonElement>()

	/** The page's values by field key, as the next call's input carries them. */
	val values: Map<String, JsonElement> get() = _values

	/** Messages shown under the page's fields by key, or for the whole page under `""`: Hammer's checks' or the plugin's. */
	var errors: Map<String, String> = emptyMap()
		private set

	private val _messages = mutableListOf<String>()

	/** Every reply's message, in order. */
	val messages: List<String> get() = _messages

	/** What the action ended by opening, if it did. */
	var opened: ActionReply.Open? = null
		private set

	var ended = false
		private set

	private val _requests = mutableListOf<ActionRequest>()

	/** Every request the handler was called with, in order. */
	val requests: List<ActionRequest> get() = _requests

	private var onManifestPage = false
	private var resumed = false

	/** Opens the action, as from its menu item: a fresh run, whatever came before. */
	fun open(): ActionDriver {
		page = null
		errors = emptyMap()
		opened = null
		ended = false
		resumed = false
		refuse(fields)?.let { error("Hammer would refuse the manifest's fields: $it") }
		onManifestPage = fields.isNotEmpty()
		if (onManifestPage) show(ActionReply.Page(fields = fields)) else call(null)
		return this
	}

	fun fill(key: String, value: String) = fill(key, JsonPrimitive(value))

	fun fill(key: String, value: Boolean) = fill(key, JsonPrimitive(value))

	fun fill(key: String, value: Int) = fill(key, JsonPrimitive(value))

	/** Sets a scenes field to these scene ids. */
	fun fill(key: String, value: List<Int>) = fill(key, JsonArray(value.map(::JsonPrimitive)))

	fun fill(key: String, value: JsonElement): ActionDriver {
		val page = showing()
		require(page.fields.any { it.key == key }) { "No field $key on the page; it has ${page.fields.map { it.key }}" }
		_values[key] = value
		return this
	}

	/** Presses Run on the manifest's page, calling the action with no button. */
	fun run(): ActionDriver {
		showing()
		check(onManifestPage) { "Run is only on the manifest's page; press one of this page's buttons" }
		submit(null, validate = true)
		return this
	}

	fun press(id: String): ActionDriver {
		val page = showing()
		val button = page.buttons.find { it.id == id }
			?: error("No button $id on the page; it has ${page.buttons.map { it.id }}")
		submit(id, button.validate)
		return this
	}

	/**
	 * Closes the dialog and opens the action again from the same place, as Hammer does with an action that
	 * keeps its runs: the page and its values come back, and the next call carries `resumed`.
	 */
	fun resume(): ActionDriver {
		showing()
		errors = emptyMap()
		resumed = true
		return this
	}

	/** The page's state read into a `T`, as [ActionRequest.state] reads it. */
	inline fun <reified T> state(): T? = decodeState(page?.state)

	private fun showing(): ActionReply.Page = page ?: error(if (ended) "The action has ended" else "The action is not open")

	private fun submit(button: String?, validate: Boolean) {
		if (validate) {
			val problems = showing().fields.mapNotNull { field -> problem(field)?.let { field.key to it } }.toMap()
			if (problems.isNotEmpty()) {
				errors = problems
				return
			}
		}
		call(button)
	}

	private fun call(button: String?) {
		val request = ActionRequest(
			action, project, settings, JsonObject(_values.toMap()), context, button,
			changed = null, state = page?.state, resumed = resumed, locale = locale,
		)
		resumed = false
		_requests += request
		// Through JSON and back, so the driver sees what Hammer would.
		val reply = json.decodeFromString(ActionReply.serializer(), (handle(request) ?: ActionReply()).toJson())
		refuse(reply)?.let { throw IllegalStateException("Hammer would fail this call: $it") }
		reply.message?.let { _messages += it }
		errors = emptyMap()
		when {
			reply.errors.isNotEmpty() -> errors = reply.errors
			reply.open != null -> end(reply.open)
			reply.page != null -> {
				onManifestPage = false
				show(reply.page)
			}
			else -> end(null)
		}
	}

	private fun show(page: ActionReply.Page) {
		this.page = page
		_values.clear()
		page.fields.forEach { _values[it.key] = it.default ?: emptyValue(it) }
	}

	private fun end(open: ActionReply.Open?) {
		page = null
		_values.clear()
		opened = open
		ended = true
	}

	/** Hammer's check of one field's value before a call: what is wrong, or null. */
	private fun problem(field: ActionReply.Field): String? {
		val value = _values[field.key]
		val text = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
		return when (field.type) {
			"text" -> "Required".takeIf { field.required && text.isNullOrBlank() }
			"scenes" -> "Required".takeIf { field.required && (value == null || value == JsonNull || (value as? JsonArray)?.isEmpty() == true) }
			"int" -> {
				val number = (value as? JsonPrimitive)?.takeUnless { it.isString }?.longOrNull
				when {
					number == null -> "Not a number"
					field.min != null && number < field.min -> "At least ${field.min}"
					field.max != null && number > field.max -> "At most ${field.max}"
					else -> null
				}
			}
			"choice" -> "Not one of the options".takeIf { field.options.none { it.value == text } }
			else -> null
		}
	}

	companion object {
		private const val STATE_MAX = 1024 * 1024
		private val PLACES = setOf("project", "scene", "note", "entry", "event")

		/**
		 * The `[[actions.field]]`s of [action] in [manifest], by default the plugin's own, since Gradle runs
		 * tests in the project's directory. None when there is no such file.
		 */
		fun manifestFields(action: String, manifest: File = File("manifest.toml")): List<ActionReply.Field> {
			if (!manifest.isFile) return emptyList()
			val actions = Toml.parseToTomlTable(manifest.readText())["actions"] as? TomlArray
			val entry = actions?.filterIsInstance<TomlTable>()?.find { (it["name"] as? TomlLiteral)?.content == action }
				?: error("$manifest has no action $action")
			return (entry["field"] as? TomlArray)?.map { json.decodeFromJsonElement<ActionReply.Field>(it.toJson()) }.orEmpty()
		}

		private fun TomlElement.toJson(): JsonElement = when (this) {
			is TomlTable -> JsonObject(content.mapValues { it.value.toJson() })
			is TomlArray -> JsonArray(content.map { it.toJson() })
			is TomlLiteral -> when (type) {
				TomlLiteral.Type.Boolean -> JsonPrimitive(content.toBoolean())
				TomlLiteral.Type.Integer -> JsonPrimitive(content.toLong())
				TomlLiteral.Type.Float -> JsonPrimitive(content.toDouble())
				else -> JsonPrimitive(content)
			}
			TomlNull -> JsonNull
		}

		private fun emptyValue(field: ActionReply.Field): JsonElement = when (field.type) {
			"bool" -> JsonPrimitive(false)
			"int" -> JsonPrimitive(field.min ?: 0)
			"choice" -> JsonPrimitive(field.options.first().value)
			"scenes" -> if (field.multiple) JsonArray(emptyList()) else JsonNull
			else -> JsonPrimitive("")
		}

		private fun refuse(fields: List<ActionReply.Field>): String? = fields.firstNotNullOfOrNull { field ->
			when {
				field.type !in setOf("text", "bool", "int", "choice", "scenes") -> "field ${field.key} has the unknown type ${field.type}"
				field.type == "choice" && field.options.isEmpty() -> "choice ${field.key} has no options"
				else -> null
			}
		}

		private fun refuse(reply: ActionReply): String? {
			reply.page?.let { page ->
				refuse(page.fields)?.let { return it }
				val size = page.state?.toString()?.encodeToByteArray()?.size ?: 0
				if (size > STATE_MAX) return "its state is $size bytes, over 1 MiB"
			}
			reply.open?.let { if (it.place !in PLACES) return "it opens the unknown place ${it.place}" }
			return null
		}
	}
}
