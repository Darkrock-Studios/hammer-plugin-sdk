package hammer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.io.encoding.Base64

// The typed operations in hammer.ops are generated; these are what their bodies call.

internal inline fun <reified O> operation(name: String, input: JsonObject): O =
	json.decodeFromJsonElement(callJson(name, input))

internal inline fun <reified T> operationValue(value: T): JsonElement = json.encodeToJsonElement(value)

internal fun <T> operationValue(value: T, serializer: SerializationStrategy<T>): JsonElement =
	json.encodeToJsonElement(serializer, value)

/**
 * A generated enum's serializer. A value this kit doesn't know, such as an entry kind added in a later
 * Hammer, reads as [unknown] rather than failing the whole output; [unknown] can't be sent.
 */
abstract class OperationEnumSerializer<E : Enum<E>>(
	serialName: String,
	private val entries: List<E>,
	private val unknown: E,
	private val value: (E) -> String,
) : KSerializer<E> {
	override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

	override fun serialize(encoder: Encoder, value: E) {
		require(value != unknown) { "${descriptor.serialName}.${unknown.name} can't be sent to Hammer" }
		encoder.encodeString(value(value))
	}

	override fun deserialize(decoder: Decoder): E {
		val text = decoder.decodeString()
		return entries.firstOrNull { it != unknown && value(it) == text } ?: unknown
	}
}

/** Binary content, such as an exported file or an entry's image, which operations carry as base64 text. */
object Base64Bytes : KSerializer<ByteArray> {
	override val descriptor = PrimitiveSerialDescriptor("hammer.Base64Bytes", PrimitiveKind.STRING)

	override fun serialize(encoder: Encoder, value: ByteArray) = encoder.encodeString(Base64.encode(value))

	override fun deserialize(decoder: Decoder): ByteArray = Base64.decode(decoder.decodeString())
}
