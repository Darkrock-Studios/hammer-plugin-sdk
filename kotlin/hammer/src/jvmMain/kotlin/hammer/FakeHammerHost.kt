package hammer

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * A stand-in for Hammer in JVM tests: [operations] answer calls by name, and [cache] is the plugin's
 * cache. An operation with no handler fails as NotFound; a handler throws [OperationException] to fail.
 *
 *     hammer.host = FakeHammerHost(mapOf("scene.read" to { input -> buildJsonObject { put("markdown", "Rain.") } }))
 */
class FakeHammerHost(
	private val operations: Map<String, (JsonObject) -> JsonElement> = emptyMap(),
) : HammerHost {
	val cache = mutableMapOf<String, ByteArray>()
	val logged = mutableListOf<Pair<LogLevel, String>>()
	val progress = mutableListOf<JsonObject>()

	override fun dispatch(request: String): String {
		val parsed = json.parseToJsonElement(request).jsonObject
		val name = parsed.getValue("operation").jsonPrimitive.content
		val input = parsed["input"] as? JsonObject ?: JsonObject(emptyMap())
		val reply = try {
			val handler = operations[name] ?: throw OperationException("NotFound", "No operation $name")
			buildJsonObject { put("output", handler(input)) }
		} catch (e: OperationException) {
			buildJsonObject {
				putJsonObject("error") {
					put("kind", e.kind)
					put("message", e.message)
				}
			}
		}
		return reply.toString()
	}

	override fun cacheGet(key: ByteArray): ByteArray? = cache[key.decodeToString()]

	override fun cacheSet(key: ByteArray, value: ByteArray) {
		if (value.isEmpty()) cache.remove(key.decodeToString()) else cache[key.decodeToString()] = value
	}

	override fun log(level: LogLevel, message: String) {
		logged += level to message
	}

	override fun progress(report: String) {
		progress += json.parseToJsonElement(report).jsonObject
	}
}
