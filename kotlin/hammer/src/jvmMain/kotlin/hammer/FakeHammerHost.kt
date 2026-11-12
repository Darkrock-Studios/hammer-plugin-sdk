package hammer

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File

/**
 * A stand-in for Hammer in JVM tests: [operations] answer calls by name, and [cache] is the plugin's
 * cache. An operation with no handler fails as NotFound; a handler throws [OperationException] to fail.
 * Resources are read from [resourceDirectory], by default the plugin's own `resources/`, since Gradle
 * runs tests in the project's directory. [services] answer requests to the sync server's services by
 * name; one with no handler is not permitted, and a handler throws [ServerException] to fail. Requests
 * Hammer would refuse fail as it fails them, and headers it would drop are dropped.
 *
 *     hammer.host = FakeHammerHost(mapOf("scene.read" to { input -> buildJsonObject { put("markdown", "Rain.") } }))
 */
class FakeHammerHost(
	private val operations: Map<String, (JsonObject) -> JsonElement> = emptyMap(),
	private val resourceDirectory: File = File("resources"),
	private val services: Map<String, (ServiceCall) -> ServerResponse> = emptyMap(),
) : HammerHost {
	val cache = mutableMapOf<String, ByteArray>()
	val logged = mutableListOf<Pair<LogLevel, String>>()
	val progress = mutableListOf<JsonObject>()
	val serviceCalls = mutableListOf<ServiceCall>()

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

	// Only paths inside the directory, as Hammer only has the files under resources/.
	override fun resource(name: String): ByteArray? {
		if (name.split('/').any { it.isEmpty() || it == "." || it == ".." || '\\' in it }) return null
		return File(resourceDirectory, name).takeIf { it.isFile }?.readBytes()?.takeIf { it.isNotEmpty() }
	}

	override fun serverRequest(request: String, body: ByteArray?): String {
		val parsed = json.parseToJsonElement(request).jsonObject
		val call = ServiceCall(
			service = parsed.getValue("service").jsonPrimitive.content,
			method = parsed.getValue("method").jsonPrimitive.content.uppercase(),
			path = parsed.getValue("path").jsonPrimitive.content,
			headers = parsed["headers"]?.jsonObject.orEmpty()
				.filterKeys { name -> SERVER_HEADERS.any { it.equals(name, ignoreCase = true) } }
				.mapValues { it.value.jsonPrimitive.content },
			body = body,
		)
		serviceCalls += call
		val reply = try {
			val handler = services[call.service]
				?: throw ServerException(ServerErrorKind.NotPermitted, "The manifest doesn't name ${call.service}")
			val dotSegment = call.path.split('/').any { it == "." || it == ".." }
			if (call.method !in SERVER_METHODS || !SERVER_PATH.matches(call.path) || dotSegment) {
				throw ServerException(ServerErrorKind.Invalid, "Refused ${call.method} ${call.path}")
			}
			val response = handler(call)
			buildJsonObject {
				put("status", response.status)
				put("body", response.body)
			}
		} catch (e: ServerException) {
			buildJsonObject {
				put("error", e.kind.wire ?: ServerErrorKind.Invalid.wire)
				put("message", e.message)
			}
		}
		return reply.toString()
	}

	override fun log(level: LogLevel, message: String) {
		logged += level to message
	}

	override fun progress(report: String) {
		progress += json.parseToJsonElement(report).jsonObject
	}
}

// What Hammer allows a request to a service: the same checks as its own.
private val SERVER_METHODS = setOf("GET", "POST")
private val SERVER_PATH = Regex("(/[A-Za-z0-9._-]+)+")
private val SERVER_HEADERS = listOf("Content-Type", "Accept")

/** A request a plugin sent to a service on the sync server, as [FakeHammerHost] received it. */
class ServiceCall(
	val service: String,
	val method: String,
	val path: String,
	val headers: Map<String, String>,
	val body: ByteArray?,
) {
	val bodyText: String? get() = body?.decodeToString()
}
