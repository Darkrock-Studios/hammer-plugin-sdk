package hammer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlin.io.encoding.Base64

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
	/**
	 * For a format whose manifest entry sets `images = true`, each project image the story refers to
	 * whose file Hammer could read, sorted by id, and the cover with `cover = true`; empty otherwise.
	 */
	val images: List<Image> = emptyList(),
	/** The cover's id among [images], with `cover = true`; null when the project has none or it could not be read. */
	val coverImageId: Int? = null,
) : WithSettings {
	/**
	 * A chapter's scenes: as markdown in [scenes], or, for a format whose manifest entry sets
	 * `input = "prose"`, in [prose] as Hammer's parsed blocks, one list of blocks per scene.
	 *
	 * Markdown shows an image as `![image](hammer://image/<id>)`, which [findImageIds] and
	 * [parseImageReference] read. In prose, with `images = true`, a line holding only an image is a block
	 * `{"type": "image", "id": 5, "name": "Map"}`, which [proseImageId] reads; without it, the image is
	 * a paragraph of its name. An image inside a line, or one whose file Hammer could not read, is its
	 * name as text, never a block.
	 */
	@Serializable
	class Chapter(
		val name: String,
		val scenes: List<String> = emptyList(),
		val prose: List<List<JsonElement>> = emptyList(),
	)

	/** A project image and its file: `image.get`'s [hammer.ops.ImageContent] with [mimeType] added. */
	@Serializable
	class Image(
		val id: Int,
		val name: String,
		val extension: String,
		/** The file's media type; `application/octet-stream` when Hammer does not know [extension]. */
		val mimeType: String,
		/** The file as Hammer sent it, standard padded base64. */
		@SerialName("content")
		val base64: String,
	) {
		/**
		 * The file's bytes, decoded from [base64] when first read, which throws [IllegalArgumentException]
		 * for text that is not base64.
		 */
		val content: ByteArray by lazy { Base64.decode(base64) }
	}

	/** The image [id] among [images], or null when it is not there. */
	fun image(id: Int): Image? = images.firstOrNull { it.id == id }

	/** The cover among [images], or null when there is none. */
	val cover: Image? get() = coverImageId?.let(::image)
}

/** The id of a prose `image` block, `{"type": "image", "id": 5, "name": "Map"}`; null for any other block. */
fun proseImageId(block: JsonElement): Int? {
	val fields = block as? JsonObject ?: return null
	val type = fields["type"] as? JsonPrimitive
	if (type == null || !type.isString || type.content != "image") return null
	return (fields["id"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
}

/** What the `action` export is given: one of the manifest's `[[actions]]`, run from a screen of a project. */
@Serializable
class ActionRequest(
	val action: String,
	val project: String,
	override val settings: JsonObject = JsonObject(emptyMap()),
	/** A value for each field of the page the user was on; on the first call, the action's `[[actions.field]]`s. */
	val input: JsonObject = JsonObject(emptyMap()),
	val context: Context = Context(),
	/** The id of the button pressed; null on the first call, which opens the action or runs the manifest's fields. */
	val button: String? = null,
	/** Reserved for fields that call the action when they change; always null. */
	val changed: String? = null,
	/** The last page's [ActionReply.Page.state], as it was written; null on the first call. See [state]. */
	val state: JsonElement? = null,
	/** Set on the first call after Hammer restored a run the user had left. */
	val resumed: Boolean = false,
	/** The UI's BCP 47 tag, such as `fr-CA`, for replying in the user's language; null when unknown. */
	val locale: String? = null,
) : WithSettings {
	/** The screen it was run from (`project`, `scene`, `note`, `entry`, or `event`) and the id of the item shown there. */
	@Serializable
	class Context(val place: String = "project", val id: Int? = null)
}

/** The action's input read into a `T`, a `@Serializable` class with a property per field of the page. */
inline fun <reified T> ActionRequest.input(): T = json.decodeFromJsonElement(input)

/** The request's state read into a `T`, as [stateOf] wrote it: null when there is none, or it is not a `T`. */
inline fun <reified T> ActionRequest.state(): T? = decodeState(state)

@PublishedApi
internal inline fun <reified T> decodeState(state: JsonElement?): T? {
	if (state == null || state == JsonNull) return null
	return try {
		json.decodeFromJsonElement<T>(state)
	} catch (e: IllegalArgumentException) {
		null
	}
}

/** [value] as JSON, for a page's [ActionReply.Page.state]; [ActionRequest.state] reads it back. */
inline fun <reified T> stateOf(value: T): JsonElement = json.encodeToJsonElement(value)

/**
 * What the `action` export returns; every part is optional, and the default ends the action without a
 * word. A [page] is shown in the action's dialog, where pressing one of its buttons calls the action
 * again with the button's id, the page's values as [ActionRequest.input], and the page's state. A
 * [message] is shown briefly, whatever else the reply does. [errors] keep the user on the page they
 * were on, with what they entered and its state; [open] ends the action and goes to an item. A reply
 * with none of [page], [errors], or [open] ends the action.
 */
@Serializable
class ActionReply(
	val page: Page? = null,
	val message: String? = null,
	/** Messages by field key, shown under each field; a key of `""`, or one that is no field's, is shown for the whole page. */
	val errors: Map<String, String> = emptyMap(),
	val open: Open? = null,
) {
	/** What the dialog shows: [title], [markdown], the [fields] in order, and [buttons]. */
	@Serializable
	class Page(
		val title: String? = null,
		val markdown: String? = null,
		val fields: List<Field> = emptyList(),
		val buttons: List<Button> = emptyList(),
		/**
		 * Handed back untouched as the next call's [ActionRequest.state], usually from [stateOf]. Hammer
		 * never reads it, and fails a call whose state is over 1 MiB as JSON.
		 */
		val state: JsonElement? = null,
	)

	/**
	 * A field of a page, made by [text], [bool], [int], [choice], [scenes], or [entity]. Its value comes
	 * back in the next call's input under [key]: a string for text and choice, a bool, an integer, for
	 * scenes a list of scene ids (one id or null when not [multiple]), and for entity an [EntityRef] or
	 * null (a list of them when [multiple]). Hammer checks [required], an int's bounds, a choice's options,
	 * and that an entity's picks exist and match its filters before calling, unless the button pressed
	 * skips them.
	 */
	@Serializable
	class Field private constructor(
		val key: String,
		/** `text`, `bool`, `int`, `choice`, `scenes`, or `entity`. */
		val type: String,
		val label: String,
		val hint: String? = null,
		/** For text, not blank; for scenes, not empty; for entity, a pick. */
		val required: Boolean = false,
		/** The initial value; null for the type's own: false, [min] or 0, the first option, "", or none. */
		val default: JsonElement? = null,
		val min: Int? = null,
		val max: Int? = null,
		val options: List<Option> = emptyList(),
		val format: TextFormat = TextFormat.Plain,
		val multiline: Boolean = false,
		/** An entity's kinds of item: `scene`, `note`, `entry`, or `event`; all four when empty. */
		val kinds: List<String> = emptyList(),
		/** An entity's types of entry: `person`, `place`, `thing`, `event`, or `idea`; any when empty. */
		@SerialName("entry_types")
		val entryTypes: List<String> = emptyList(),
		/** An entity's tags: an item needs one of them; any item when empty. */
		val tags: List<String> = emptyList(),
		// Null for the type's own, which differ: scenes pick a list, an entity one item.
		@SerialName("multiple")
		private val multipleKey: Boolean? = null,
	) {
		/** Whether the value is a list: by default for scenes, not for an entity. */
		val multiple: Boolean get() = multipleKey ?: (type == "scenes")

		/** One of a choice's options: [value] is what comes back in the input, [label] what is shown. */
		@Serializable
		class Option(val value: String, val label: String)

		companion object {
			/** Text, edited as [format] says; [multiline] is for plain text. */
			fun text(
				key: String,
				label: String,
				hint: String? = null,
				required: Boolean = false,
				default: String? = null,
				format: TextFormat = TextFormat.Plain,
				multiline: Boolean = false,
			) = Field(key, "text", label, hint, required, default?.let(::JsonPrimitive), format = format, multiline = multiline)

			fun bool(key: String, label: String, hint: String? = null, default: Boolean? = null) =
				Field(key, "bool", label, hint, default = default?.let(::JsonPrimitive))

			fun int(key: String, label: String, hint: String? = null, default: Int? = null, min: Int? = null, max: Int? = null) =
				Field(key, "int", label, hint, default = default?.let(::JsonPrimitive), min = min, max = max)

			/** A choice of [options], at least one; [default] is an option's value. */
			fun choice(key: String, label: String, options: List<Option>, hint: String? = null, default: String? = null) =
				Field(key, "choice", label, hint, default = default?.let(::JsonPrimitive), options = options)

			/**
			 * Scenes picked from the project's tree: a list of ids, or with [multiple] false, one id or null.
			 * [default] is the ids picked at first, at most one when not [multiple].
			 */
			fun scenes(
				key: String,
				label: String,
				hint: String? = null,
				required: Boolean = false,
				multiple: Boolean = true,
				default: List<Int>? = null,
			): Field {
				val initial = default?.let { ids ->
					if (multiple) JsonArray(ids.map(::JsonPrimitive)) else ids.singleOrNull()?.let(::JsonPrimitive) ?: JsonNull
				}
				return Field(key, "scenes", label, hint, required, initial, multipleKey = false.takeUnless { multiple })
			}

			/**
			 * An item of the project picked by name: one [EntityRef] or null, or with [multiple] a list. The
			 * picker offers [kinds] of item (`scene`, `note`, `entry`, `event`; all four when empty); of
			 * entries, those of [entryTypes] (`person`, `place`, `thing`, `event`, `idea`; any when empty),
			 * which needs `entry` among the kinds; and with [tags], only items tagged with one of them.
			 * [default] is picked at first; Hammer shows a default that is gone or does not match as empty.
			 */
			fun entity(
				key: String,
				label: String,
				hint: String? = null,
				required: Boolean = false,
				kinds: List<String> = emptyList(),
				entryTypes: List<String> = emptyList(),
				tags: List<String> = emptyList(),
				multiple: Boolean = false,
				default: EntityRef? = null,
			): Field {
				val initial = default?.let { if (multiple) entityJson(listOf(it)) else entityJson(it) }
				return Field(
					key, "entity", label, hint, required, initial,
					kinds = kinds, entryTypes = entryTypes, tags = tags, multipleKey = true.takeIf { multiple },
				)
			}

			/**
			 * An [entity] field starting with the items in [default]: a list, or with [multiple] false, one
			 * item from a [default] of at most one.
			 */
			fun entity(
				key: String,
				label: String,
				hint: String? = null,
				required: Boolean = false,
				kinds: List<String> = emptyList(),
				entryTypes: List<String> = emptyList(),
				tags: List<String> = emptyList(),
				multiple: Boolean = true,
				default: List<EntityRef>,
			): Field {
				require(multiple || default.size <= 1) { "A field of one item starts with one at most, not ${default.size}" }
				if (!multiple) return entity(key, label, hint, required, kinds, entryTypes, tags, default = default.singleOrNull())
				return Field(
					key, "entity", label, hint, required, entityJson(default),
					kinds = kinds, entryTypes = entryTypes, tags = tags, multipleKey = true,
				)
			}

			private fun entityJson(ref: EntityRef): JsonElement = json.encodeToJsonElement(EntityRef.serializer(), ref)

			private fun entityJson(refs: List<EntityRef>): JsonElement = JsonArray(refs.map(::entityJson))
		}
	}

	/**
	 * A button of a page: pressing it calls the action again with [id] as [ActionRequest.button]. It sits
	 * in the dialog's footer, or when [inline] under the markdown, for acting on something in it. With
	 * [validate] false, Hammer calls without checking the fields, as it should for Back or Cancel.
	 */
	@Serializable
	class Button(
		val id: String,
		val label: String,
		val primary: Boolean = false,
		val inline: Boolean = false,
		val validate: Boolean = true,
	)

	/**
	 * An item for Hammer to go to: [place] is `project`, `scene`, `note`, `entry`, or `event`, and [id] the
	 * item's (null for `project`). An id that does not exist fails.
	 */
	@Serializable
	class Open(val place: String, val id: Int? = null)

	/** What the `action` export returns for this reply, without the parts left unset. */
	fun toJson(): String = json.encodeToString(serializer(), this)

	companion object {
		/** Ends the action, showing [message] briefly. */
		fun message(message: String) = ActionReply(message = message)

		/** Shows [markdown] alone, a report the user can copy or save as a note. */
		fun document(markdown: String, title: String? = null) = ActionReply(Page(title = title, markdown = markdown))
	}
}

/**
 * An item of the project, as an entity field's value: [kind] is `scene`, `note`, `entry`, or `event`, and
 * [id] the item's. A plugin's input class reads one field as `EntityRef?` and one with `multiple` as
 * `List<EntityRef>`; `ActionReply.Open(ref.kind, ref.id)` goes to it.
 */
@Serializable
data class EntityRef(val kind: String, val id: Int)

/** What edits a text field: an ordinary text field, or the scene editor's, whose value is markdown. */
@Serializable
enum class TextFormat {
	@SerialName("plain")
	Plain,

	/** The scene editor with its format bar. */
	@SerialName("rich")
	Rich,

	/** The scene editor with the extended format bar: highlight, task lists, and tables. */
	@SerialName("rich-extended")
	RichExtended,
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
