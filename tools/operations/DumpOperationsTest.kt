package operations

import com.darkrockstudios.apps.hammer.operations.core.OperationList
import com.darkrockstudios.apps.hammer.operations.core.coreOperations
import com.darkrockstudios.apps.hammer.operations.core.descriptor
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File
import kotlin.test.Test

class DumpOperationsTest {
	private val docs = mutableMapOf<String, MutableMap<String, String>>()

	private companion object {
		/** The host's name for an object or enum type. */
		const val NAME_KEY = "x-name"
	}

	@Test
	fun dump() {
		File("src/commonMain/kotlin").walkTopDown().filter { it.extension == "kt" }.forEach { readDocs(it.readText()) }
		val ops = coreOperations().sortedBy { it.name }.map { op ->
			val d = op.descriptor()
			d.copy(input = annotate(op.input.descriptor, d.input), output = annotate(op.output.descriptor, d.output))
		}
		File("build/operations.json").writeText(Json { prettyPrint = true }.encodeToString(OperationList.serializer(), OperationList(ops)))
	}

	private fun readDocs(source: String) {
		val header = Regex("""class\s+(\w+)\s*\(""")
		val field = Regex("""/\*\*(.*?)\*/\s*(?:@[^\n]*\n\s*)*va[lr]\s+(\w+)""", RegexOption.DOT_MATCHES_ALL)
		header.findAll(source).forEach { match ->
			var depth = 1
			var i = match.range.last + 1
			while (i < source.length && depth > 0) {
				when (source[i]) { '(' -> depth++; ')' -> depth-- }
				i++
			}
			val params = source.substring(match.range.last + 1, i - 1)
			field.findAll(params).forEach { f ->
				val text = f.groupValues[1].lines().joinToString(" ") { it.trim().removePrefix("*").trim() }.trim()
				docs.getOrPut(match.groupValues[1]) { mutableMapOf() }[f.groupValues[2]] = text
			}
		}
	}

	@OptIn(ExperimentalSerializationApi::class)
	private fun annotate(desc: SerialDescriptor, schema: JsonObject): JsonObject {
		schema["anyOf"]?.let { any ->
			return JsonObject(schema + ("anyOf" to JsonArray(any.jsonArray.map {
				val o = it.jsonObject
				if (o["type"] == JsonPrimitive("null")) o else annotate(desc, o)
			})))
		}
		val owner = desc.serialName.removeSuffix("?").substringAfterLast('.')
		return when (desc.kind) {
			// Generated kits name their types after the host's.
			SerialKind.ENUM -> JsonObject(schema + (NAME_KEY to JsonPrimitive(owner)))
			PrimitiveKind.STRING -> when (owner) {
				"Instant" -> JsonObject(schema + ("format" to JsonPrimitive("date-time")))
				"LocalDate" -> JsonObject(schema + ("format" to JsonPrimitive("date")))
				else -> schema
			}
			PrimitiveKind.LONG -> JsonObject(schema + ("format" to JsonPrimitive("int64")))
			StructureKind.CLASS, StructureKind.OBJECT -> {
				val props = schema["properties"]?.jsonObject ?: return JsonObject(schema + (NAME_KEY to JsonPrimitive(owner)))
				val annotated = props.mapValues { (name, prop) ->
					val index = desc.getElementIndex(name)
					var p = if (index >= 0) annotate(desc.getElementDescriptor(index), prop.jsonObject) else prop.jsonObject
					docs[owner]?.get(name)?.let { p = JsonObject(p + ("description" to JsonPrimitive(it))) }
					p
				}
				JsonObject(schema + ("properties" to JsonObject(annotated)) + (NAME_KEY to JsonPrimitive(owner)))
			}
			StructureKind.LIST -> schema["items"]?.let {
				JsonObject(schema + ("items" to annotate(desc.getElementDescriptor(0), it.jsonObject)))
			} ?: schema
			StructureKind.MAP -> schema["additionalProperties"]?.let {
				if (it is JsonObject) JsonObject(schema + ("additionalProperties" to annotate(desc.getElementDescriptor(1), it))) else schema
			} ?: schema
			else -> schema
		}
	}
}
