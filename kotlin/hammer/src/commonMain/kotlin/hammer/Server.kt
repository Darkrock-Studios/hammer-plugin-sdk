package hammer

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

// Requests to a service on the user's own Hammer sync server, such as `languagetool`. Hammer sends
// them as the signed-in user, so the plugin never holds their credentials, and only to the services
// the manifest names under `permissions.hammerServer`. hammer.languagetool has typed functions for
// the `languagetool` service.

class ServerResponse(val status: Int, val body: String) {
	val isSuccess: Boolean get() = status in 200..299

	/**
	 * What a status other than success means, with the message the sync server gave for the user when
	 * it gave one; null on success.
	 */
	fun error(): ServerException? {
		if (isSuccess) return null
		val kind = when (status) {
			429 -> ServerErrorKind.RateLimited
			in 400..499 -> ServerErrorKind.Rejected
			else -> ServerErrorKind.ServiceFailed
		}
		val detail = body.take(200).trim()
		val message = displayMessage(body)
			?: if (detail.isEmpty()) "The service replied $status" else "The service replied $status: $detail"
		return ServerException(kind, message)
	}
}

/** Why a request failed: the first five are from Hammer, before any reply; the rest are what [ServerResponse.error] makes of a status. */
enum class ServerErrorKind(internal val wire: String?) {
	/** No sync account in this workspace, or its session has ended. */
	NotSignedIn("not_signed_in"),

	/** The sync server doesn't offer the service. */
	Unavailable("unavailable"),

	/** The sync server couldn't be reached. */
	Network("network"),

	/** The manifest doesn't name the service. */
	NotPermitted("not_permitted"),

	/** A refused method or path, or a reply that isn't text, or isn't what the service should send. */
	Invalid("invalid"),

	/** Too many requests: 429. The sync server allows more after a minute. */
	RateLimited(null),

	/** The service refused the request, such as one too large or in a language it doesn't check: 4xx. */
	Rejected(null),

	/** The service failed, or the sync server couldn't reach it: 5xx. */
	ServiceFailed(null),
}

class ServerException(val kind: ServerErrorKind, message: String) : Exception(message)

/**
 * Sends a request to [service], at [path] under it (such as `/v2/check`), and waits for its response.
 * [method] is `GET` or `POST`; only `Content-Type` and `Accept` headers are sent. A response of any
 * status is returned; [ServerResponse.error] says what one other than success means. Throws
 * [ServerException] when there is no response.
 */
fun serverRequest(
	service: String,
	method: String,
	path: String,
	headers: Map<String, String> = emptyMap(),
	body: ByteArray? = null,
): ServerResponse {
	val request = buildJsonObject {
		put("service", service)
		put("method", method)
		put("path", path)
		putJsonObject("headers") { headers.forEach { (name, value) -> put(name, value) } }
	}.toString()
	val reply = host.serverRequest(request, body?.takeIf { it.isNotEmpty() })
		?: throw ServerException(ServerErrorKind.Invalid, "The sync server gave no reply")
	return parseServerReply(reply)
}

internal fun parseServerReply(reply: String): ServerResponse {
	val parsed = try {
		json.parseToJsonElement(reply).jsonObject
	} catch (e: IllegalArgumentException) {
		throw ServerException(ServerErrorKind.Invalid, e.message ?: "The sync server's reply couldn't be read")
	}
	parsed["status"]?.jsonPrimitive?.intOrNull?.let { status ->
		return ServerResponse(status, parsed["body"]?.jsonPrimitive?.contentOrNull.orEmpty())
	}
	val error = parsed["error"]?.jsonPrimitive?.contentOrNull
	val kind = ServerErrorKind.entries.firstOrNull { it.wire != null && it.wire == error } ?: ServerErrorKind.Invalid
	throw ServerException(kind, parsed["message"]?.jsonPrimitive?.contentOrNull ?: "The sync server gave no reply")
}

private fun displayMessage(body: String): String? = try {
	json.parseToJsonElement(body).jsonObject["displayMessage"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }
} catch (_: IllegalArgumentException) {
	null
}

/** [fields] as `application/x-www-form-urlencoded`. */
internal fun formEncode(fields: List<Pair<String, String>>): String =
	fields.joinToString("&") { (name, value) -> "${formEncode(name)}=${formEncode(value)}" }

private fun formEncode(text: String): String = buildString {
	for (byte in text.encodeToByteArray()) {
		val code = byte.toInt() and 0xFF
		val char = code.toChar()
		when {
			char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in "-._*" -> append(char)
			char == ' ' -> append('+')
			else -> append('%').append(HEX[code shr 4]).append(HEX[code and 0xF])
		}
	}
}

private const val HEX = "0123456789ABCDEF"
