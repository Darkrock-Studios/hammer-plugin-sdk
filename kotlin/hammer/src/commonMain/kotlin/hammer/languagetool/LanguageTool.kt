package hammer.languagetool

import hammer.ServerErrorKind
import hammer.ServerException
import hammer.ServerResponse
import hammer.formEncode
import hammer.json
import hammer.serverRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.DeserializationStrategy

/**
 * LanguageTool on the user's sync server: the `languagetool` service, which forwards to the
 * LanguageTool server its admin set up. The manifest names it under `permissions.hammerServer`:
 *
 *     [permissions]
 *     hammerServer = ["languagetool"]
 *
 * A failure throws [ServerException].
 *
 *     val checked = LanguageTool.check(CheckRequest(text, language = "en-US"))
 *     checked.matches.forEach { Log.info("${it.textIn(text)}: ${it.message}") }
 */
object LanguageTool {
	const val SERVICE = "languagetool"
	const val CHECK_PATH = "/v2/check"
	const val LANGUAGES_PATH = "/v2/languages"

	/**
	 * The most a check's encoded form may be on a sync server left at its defaults; an admin may allow
	 * more. Encoding can triple text that isn't ASCII, so keep a check's text to about 30,000 bytes.
	 */
	const val DEFAULT_MAX_REQUEST_BYTES = 100_000

	/** Checks [request]'s text on the sync server's LanguageTool. */
	fun check(request: CheckRequest): CheckResponse {
		val headers = mapOf("Content-Type" to "application/x-www-form-urlencoded", "Accept" to "application/json")
		return parse(serverRequest(SERVICE, "POST", CHECK_PATH, headers, request.form().encodeToByteArray()), CheckResponse.serializer())
	}

	/** The languages the sync server's LanguageTool checks. */
	fun languages(): List<Language> = parse(
		serverRequest(SERVICE, "GET", LANGUAGES_PATH, mapOf("Accept" to "application/json")),
		ListSerializer(Language.serializer()),
	)

	internal fun <T> parse(response: ServerResponse, deserializer: DeserializationStrategy<T>): T {
		response.error()?.let { throw it }
		return try {
			json.decodeFromString(deserializer, response.body)
		} catch (e: SerializationException) {
			throw ServerException(ServerErrorKind.Invalid, "LanguageTool's reply couldn't be read: ${e.message}")
		}
	}
}

enum class Level {
	Default,

	/** More checks of style and typography. */
	Picky,
}

/** A check of [text]. Rules and categories are LanguageTool's ids, such as `EN_A_VS_AN` or `TYPOS`. */
data class CheckRequest(
	val text: String,
	/** A language code such as `en-US`, or `auto` for LanguageTool to detect it. */
	val language: String = "auto",
	/** The writer's first language, for rules about false friends. */
	val motherTongue: String? = null,
	/** With `auto`, the variants to prefer, such as `en-GB` and `de-AT`. */
	val preferredVariants: List<String> = emptyList(),
	val enabledRules: List<String> = emptyList(),
	val disabledRules: List<String> = emptyList(),
	val enabledCategories: List<String> = emptyList(),
	val disabledCategories: List<String> = emptyList(),
	/** Only the enabled rules and categories, none of the rest. */
	val enabledOnly: Boolean = false,
	val level: Level = Level.Default,
) {
	/** The form LanguageTool reads, URL-encoded. */
	fun form(): String {
		val fields = mutableListOf("text" to text, "language" to language.trim().ifEmpty { "auto" })
		motherTongue?.let { fields += "motherTongue" to it }
		listOf(
			"preferredVariants" to preferredVariants,
			"enabledRules" to enabledRules,
			"disabledRules" to disabledRules,
			"enabledCategories" to enabledCategories,
			"disabledCategories" to disabledCategories,
		).forEach { (name, values) -> if (values.isNotEmpty()) fields += name to values.joinToString(",") }
		if (enabledOnly) fields += "enabledOnly" to "true"
		if (level == Level.Picky) fields += "level" to "picky"
		return formEncode(fields)
	}
}

@Serializable
data class CheckResponse(
	val language: CheckedLanguage? = null,
	val matches: List<Match> = emptyList(),
	val warnings: Warnings = Warnings(),
)

@Serializable
data class Warnings(
	/** LanguageTool stopped before the end of the text, as when a check takes too long. */
	val incompleteResults: Boolean = false,
)

/** The language the text was checked in, and the one LanguageTool detected in it. */
@Serializable
data class CheckedLanguage(
	val name: String = "",
	val code: String = "",
	val detectedLanguage: DetectedLanguage? = null,
)

@Serializable
data class DetectedLanguage(
	val name: String = "",
	val code: String = "",
	val confidence: Double? = null,
)

/**
 * A problem LanguageTool found. [offset] and [length] count UTF-16 units of the text checked, as a
 * Kotlin String does, so they index it directly.
 */
@Serializable
data class Match(
	val message: String = "",
	val shortMessage: String = "",
	val offset: Int,
	val length: Int,
	val replacements: List<Replacement> = emptyList(),
	val context: Context = Context(),
	val sentence: String = "",
	val rule: Rule = Rule(),
) {
	val end: Int get() = offset + length

	/** The matched part of [text], the text that was checked; null when the match runs past its end. */
	fun textIn(text: String): String? = if (offset >= 0 && length >= 0 && end <= text.length) text.substring(offset, end) else null
}

@Serializable
data class Replacement(
	val value: String,
	val shortDescription: String? = null,
)

/** Some of the text around a match, with the match's place in it. */
@Serializable
data class Context(
	val text: String = "",
	val offset: Int = 0,
	val length: Int = 0,
)

@Serializable
data class Rule(
	val id: String = "",
	val subId: String? = null,
	val description: String = "",
	/** Such as `misspelling`, `grammar`, `style`, `typographical`, or `register`. */
	val issueType: String = "",
	val category: Category = Category(),
	val urls: List<RuleUrl> = emptyList(),
)

@Serializable
data class Category(
	/** Such as `TYPOS`, `GRAMMAR`, `STYLE`, or `REDUNDANCY`. */
	val id: String = "",
	val name: String = "",
)

/** A page explaining the rule. */
@Serializable
data class RuleUrl(val value: String)

/** A language the server checks. */
@Serializable
data class Language(
	val name: String,
	val code: String,
	/** The code with its variant, such as `en-US`, which a check takes as its language. */
	val longCode: String,
)
