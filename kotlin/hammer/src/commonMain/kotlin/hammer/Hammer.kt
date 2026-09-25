// Hammer's plugin kit for Kotlin/Wasm. See hammer-editor's docs/CLIENT-PLUGINS.md for the exports a
// plugin provides and the JSON each one is given; the request classes here are that JSON.
package hammer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
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
fun callJson(operation: String, input: JsonElement): JsonElement {
	val request = buildJsonObject {
		put("operation", operation)
		put("input", input)
	}
	val reply = json.parseToJsonElement(host.dispatch(request.toString())).jsonObject
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

object Log {
	fun info(message: String) = host.log(LogLevel.Info, message)

	fun warn(message: String) = host.log(LogLevel.Warn, message)

	fun error(message: String) = host.log(LogLevel.Error, message)
}
