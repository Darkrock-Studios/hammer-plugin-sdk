package hammer.codegen

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A value's type, as each language's generator maps it. */
sealed interface Type {
	data class Text(val format: String?) : Type
	data object Integer : Type
	data object Long : Type
	data object Number : Type
	data object Bool : Type
	data object Bytes : Type
	data object AnyJson : Type
	data class ListOf(val item: Type) : Type
	data class MapOf(val value: Type) : Type
	data class Named(val name: String) : Type
	data class Nullable(val inner: Type) : Type
}

data class Field(val name: String, val type: Type, val required: Boolean, val doc: String?)

sealed interface Decl {
	val name: String
}

data class ObjectDecl(override val name: String, val fields: List<Field>) : Decl

data class EnumDecl(override val name: String, val values: List<String>) : Decl {
	/** The extra value an unrecognised one reads as: `Unknown`, unless Hammer has a value by that name. */
	fun unknownName(): String = if (values.any { pascalCase(it) == "Unknown" }) "Unrecognized" else "Unknown"
}

data class Operation(
	val name: String,
	val description: String,
	val access: String,
	val scope: String,
	/** The input object's fields, which the generated function takes as parameters. */
	val params: List<Field>,
	/** Null for an operation whose output is `{}`. */
	val output: Type?,
) {
	val segments: List<String> = name.split('.')

	/** What a manifest's `[permissions]` needs for it. */
	val permission: String =
		if (access == "destructive") "`$name` (by name)" else "`$name` or `$scope:$access`"
}

class Api(val operations: List<Operation>, val decls: Map<String, Decl>)

/** Reads the operations snapshot (`ops.list`'s output, with each type's host name in `x-name`). */
fun readApi(text: String): Api {
	val root = Json.parseToJsonElement(text).jsonObject
	val decls = sortedMapOf<String, Decl>()
	val enumNames = mutableMapOf<List<String>, String>()
	fun collectEnums(element: JsonElement) {
		when (element) {
			is JsonObject -> {
				val name = element.stringOrNull(NAME)
				val values = element["enum"]
				if (name != null && values != null) enumNames[values.jsonArray.map { it.jsonPrimitive.content }] = name
				element.values.forEach(::collectEnums)
			}
			is kotlinx.serialization.json.JsonArray -> element.forEach(::collectEnums)
			else -> Unit
		}
	}
	collectEnums(root)
	val operations = root.getValue("operations").jsonArray.map { element ->
		val op = element.jsonObject
		val reader = SchemaReader(decls, enumNames)
		val input = op.getValue("input").jsonObject
		val output = op.getValue("output").jsonObject
		reader.defs = (input["\$defs"]?.jsonObject.orEmpty() + output["\$defs"]?.jsonObject.orEmpty())
		Operation(
			name = op.string("name"),
			description = op.string("description"),
			access = op.string("access"),
			scope = op.string("scope"),
			params = reader.fields(input),
			output = if (output.isEmptyObject()) null else reader.type(output),
		)
	}
	return Api(operations, decls)
}

private class SchemaReader(val decls: MutableMap<String, Decl>, val enumNames: Map<List<String>, String>) {
	var defs: Map<String, JsonElement> = emptyMap()

	fun fields(schema: JsonObject): List<Field> {
		val required = schema["required"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty().toSet()
		return schema["properties"]?.jsonObject.orEmpty().map { (name, element) ->
			val prop = element.jsonObject
			Field(name, type(prop), name in required, prop.stringOrNull("description"))
		}
	}

	fun type(schema: JsonObject): Type {
		schema["anyOf"]?.let { any ->
			val others = any.jsonArray.map { it.jsonObject }.filter { it.stringOrNull("type") != "null" }
			require(others.size == 1) { "Only a nullable anyOf is supported: $schema" }
			return Type.Nullable(type(others.single()))
		}
		schema["\$ref"]?.let { ref ->
			val key = ref.jsonPrimitive.content.substringAfterLast('/')
			val def = defs[key]?.jsonObject ?: error("No \$defs entry $key")
			val name = def.stringOrNull(NAME) ?: key.substringAfterLast('.')
			if (name !in decls) {
				decls[name] = ObjectDecl(name, emptyList())
				decls[name] = ObjectDecl(name, fields(def))
			}
			return Type.Named(name)
		}
		schema["enum"]?.let { element ->
			val values = element.jsonArray.map { it.jsonPrimitive.content }
			// The dumper doesn't name types inside `$defs`; such an enum is one named elsewhere.
			val name = schema.stringOrNull(NAME) ?: enumNames[values] ?: error("An enum without $NAME: $schema")
			register(EnumDecl(name, values))
			return Type.Named(name)
		}
		return when (schema.stringOrNull("type")) {
			"string" -> if (schema.stringOrNull("contentEncoding") == "base64") Type.Bytes else Type.Text(schema.stringOrNull("format"))
			"integer" -> if (schema.stringOrNull("format") == "int64") Type.Long else Type.Integer
			"number" -> Type.Number
			"boolean" -> Type.Bool
			"array" -> Type.ListOf(type(schema.getValue("items").jsonObject))
			"object" -> {
				val name = schema.stringOrNull(NAME)
				val values = schema["additionalProperties"]
				when {
					name != null -> {
						register(ObjectDecl(name, fields(schema)))
						Type.Named(name)
					}
					values is JsonObject -> Type.MapOf(type(values))
					else -> Type.AnyJson
				}
			}
			else -> Type.AnyJson
		}
	}

	private fun register(decl: Decl) {
		val existing = decls[decl.name]
		require(existing == null || existing == decl) { "Two different types named ${decl.name}" }
		decls[decl.name] = decl
	}
}

/** The host's name for an object or enum type, added by the snapshot's dumper. */
private const val NAME = "x-name"

/** `{}`, as an operation with nothing to return describes its output; anything else is typed. */
private fun JsonObject.isEmptyObject() =
	stringOrNull("type") == "object" && this["properties"]?.jsonObject.isNullOrEmpty() && this["additionalProperties"] !is JsonObject

private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
private fun JsonObject.stringOrNull(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull

/** `wordCount` → `word_count`. */
fun snakeCase(name: String): String = name.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").replace('-', '_').lowercase()

/** `rich-extended` or `wordCount` → `RichExtended` or `WordCount`. */
fun pascalCase(name: String): String =
	name.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }.joinToString("") { it.replaceFirstChar(Char::uppercase) }

/** A type is referenced only from operations' inputs when it never reaches an output. */
fun Api.reachable(): List<Decl> {
	val seen = linkedSetOf<String>()
	fun visit(type: Type?) {
		when (type) {
			is Type.Named -> if (seen.add(type.name)) (decls[type.name] as? ObjectDecl)?.fields?.forEach { visit(it.type) }
			is Type.Nullable -> visit(type.inner)
			is Type.ListOf -> visit(type.item)
			is Type.MapOf -> visit(type.value)
			else -> Unit
		}
	}
	operations.forEach { op ->
		visit(op.output)
		op.params.forEach { visit(it.type) }
	}
	return seen.sorted().map { decls.getValue(it) }
}
