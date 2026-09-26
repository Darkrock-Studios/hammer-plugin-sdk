// Hammer's plugin kit for Kotlin/Wasm. See hammer-editor's docs/CLIENT-PLUGINS.md for the exports a
// plugin provides and the JSON each one is given; the request classes here are that JSON.
package hammer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

const val CACHE_MAX_KEY = 1024
const val CACHE_MAX_VALUE = 1024 * 1024

@PublishedApi
internal val json = Json { ignoreUnknownKeys = true }

/** A failed operation: [kind] is NotFound, InvalidInput, PermissionDenied, or Failed. */
class OperationException(val kind: String, message: String) : Exception(message)

/** Runs a Hammer operation the manifest has permission for; its output, or [OperationException]. */
fun callJson(operation: String, input: JsonElement): JsonElement = parseReply(dispatch(operation, input))

/**
 * [callJson]'s output as JSON text, unparsed: for passing a large output on as it is, which parsing
 * and printing again would make slow.
 */
fun callJsonText(operation: String, input: JsonElement): String {
	val reply = dispatch(operation, input)
	return outputText(reply) ?: parseReply(reply).toString()
}

private fun dispatch(operation: String, input: JsonElement): String = host.dispatch(
	buildJsonObject {
		put("operation", operation)
		put("input", input)
	}.toString(),
)

/**
 * The output in [reply] when it succeeded, without parsing it. A reply has one key, so the output runs
 * from `"output":` to the object's closing brace.
 */
private fun outputText(reply: String): String? {
	var at = skipSpace(reply, 0)
	if (at == reply.length || reply[at] != '{') return null
	at = skipSpace(reply, at + 1)
	if (!reply.startsWith(OUTPUT_KEY, at)) return null
	at = skipSpace(reply, at + OUTPUT_KEY.length)
	if (at == reply.length || reply[at] != ':') return null
	val start = skipSpace(reply, at + 1)
	var end = reply.lastIndexOf('}')
	while (end > start && reply[end - 1].isWhitespace()) end--
	return if (end > start) reply.substring(start, end) else null
}

private fun skipSpace(text: String, from: Int): Int {
	var at = from
	while (at < text.length && text[at].isWhitespace()) at++
	return at
}

private const val OUTPUT_KEY = "\"output\""

private fun parseReply(text: String): JsonElement {
	val reply = json.parseToJsonElement(text).jsonObject
	reply["error"]?.jsonObject?.let { error ->
		throw OperationException(
			error["kind"]?.jsonPrimitive?.contentOrNull ?: "Failed",
			error["message"]?.jsonPrimitive?.contentOrNull ?: "Failed",
		)
	}
	return reply["output"] ?: JsonNull
}

/**
 * Runs a Hammer operation and reads its output into a `T`, such as
 * `call<Tree>("scene.tree", buildJsonObject { put("project", name) })`.
 */
inline fun <reified T> call(operation: String, input: JsonElement): T =
	json.decodeFromJsonElement(callJson(operation, input))

// The plugin's own cache, kept by Hammer between calls and runs. Hammer may drop any value at any
// time, and clears them all when the plugin is installed again or uninstalled. A call still running on
// the old version during an install may write after the clear, so put a version in keys whose values'
// format may change.

/** The value cached under [key], or null when there is none. */
fun cacheGet(key: String): ByteArray? {
	val keyBytes = key.encodeToByteArray()
	return if (keyBytes.size > CACHE_MAX_KEY) null else host.cacheGet(keyBytes)
}

/** Caches [value] under [key]; an empty value removes it. False, storing nothing, when either is over its limit. */
fun cacheSet(key: String, value: ByteArray): Boolean {
	val keyBytes = key.encodeToByteArray()
	if (keyBytes.size > CACHE_MAX_KEY || value.size > CACHE_MAX_VALUE) return false
	host.cacheSet(keyBytes, value)
	return true
}

/**
 * Reads a file from the package's `resources/` by its path there, such as `words/en.txt`: null when the
 * package has none by that name, or it is empty. Hammer reads it from the package on every call, so keep
 * one you use often.
 */
fun resource(name: String): ByteArray? = host.resource(name)

/** [resource] as UTF-8 text. */
fun resourceText(name: String): String? = resource(name)?.decodeToString()

/**
 * Reports how far an action has got: [fraction] from 0 to 1, or null when not known, and a line
 * saying what it is doing. Hammer shows it while the action runs. When the user has stopped the run,
 * the call does not return: the action ends there.
 */
fun progress(fraction: Double? = null, message: String? = null) {
	host.progress(
		buildJsonObject {
			fraction?.let { put("fraction", it.coerceIn(0.0, 1.0)) }
			message?.let { put("message", it) }
		}.toString(),
	)
}

/** The `diagnose` export's reply for [diagnostics] in [paragraphs], at UTF-8 offsets. */
fun diagnoseReply(paragraphs: List<String>, diagnostics: List<Diagnostic>): String = buildJsonObject {
	put("diagnostics", buildJsonArray {
		diagnostics.forEach { issue ->
			val paragraph = paragraphs.getOrNull(issue.paragraph) ?: return@forEach
			add(buildJsonObject {
				put("paragraph", issue.paragraph)
				put("start", utf8Offset(paragraph, issue.start))
				put("end", utf8Offset(paragraph, issue.end))
				put("message", issue.message)
				put("fixes", buildJsonArray {
					issue.fixes.forEach { fix ->
						if (fix.label == null) {
							add(fix.replacement)
						} else {
							add(buildJsonObject {
								put("replacement", fix.replacement)
								put("label", fix.label)
							})
						}
					}
				})
			})
		}
	})
}.toString()

private fun utf8Offset(text: String, index: Int): Int {
	var bytes = 0
	var i = 0
	val end = index.coerceIn(0, text.length)
	while (i < end) {
		val c = text[i]
		bytes += when {
			c.code < 0x80 -> 1
			c.code < 0x800 -> 2
			c.isHighSurrogate() && i + 1 < text.length -> { i++; 4 }
			else -> 3
		}
		i++
	}
	return bytes
}

object Log {
	fun info(message: String) = host.log(LogLevel.Info, message)

	fun warn(message: String) = host.log(LogLevel.Warn, message)

	fun error(message: String) = host.log(LogLevel.Error, message)
}
