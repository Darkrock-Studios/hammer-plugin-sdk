package hammer.codegen

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

private const val PACKAGE = "hammer.ops"
private val SERIALIZABLE = ClassName("kotlinx.serialization", "Serializable")
private val JSON_ELEMENT = ClassName("kotlinx.serialization.json", "JsonElement")
private val JSON_NULL = ClassName("kotlinx.serialization.json", "JsonNull")
private val BUILD_JSON_OBJECT = MemberName("kotlinx.serialization.json", "buildJsonObject")
private val BASE64_BYTES = ClassName("hammer", "Base64Bytes")
private val ENUM_SERIALIZER = ClassName("hammer", "OperationEnumSerializer")
private val OPERATION = MemberName("hammer", "operation")
private val OPERATION_VALUE = MemberName("hammer", "operationValue")
private const val HEADER = "Generated from api/operations.json by kotlin/codegen: regenerate rather than edit."

/** Writes `hammer.ops`: a data class or enum for each type, and a function for each operation under [ROOT]. */
class KotlinGenerator(private val api: Api) {
	fun write(dir: File) {
		dir.deleteRecursively()
		types().writeTo(dir.parentFile.parentFile)
		operations().writeTo(dir.parentFile.parentFile)
	}

	private fun file(name: String) = FileSpec.builder(PACKAGE, name)
		.addFileComment(HEADER)
		.addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S, %S", "RedundantVisibilityModifier", "unused").build())
		.indent("\t")

	private fun types(): FileSpec {
		val file = file("Types")
		api.reachable().forEach { decl ->
			file.addType(
				when (decl) {
					is ObjectDecl -> dataClass(decl)
					is EnumDecl -> enumClass(decl)
				}
			)
		}
		return file.build()
	}

	private fun dataClass(decl: ObjectDecl): TypeSpec {
		val constructor = FunSpec.constructorBuilder()
		val type = TypeSpec.classBuilder(decl.name).addModifiers(KModifier.PUBLIC, KModifier.DATA).addAnnotation(SERIALIZABLE)
		decl.fields.forEach { field ->
			val typeName = field.type.kotlin(field.required)
			// Every field has a default: Hammer sends them all, but a test's fake or an older host may not.
			constructor.addParameter(
				ParameterSpec.builder(field.name, typeName)
					.defaultValue(if (field.required) field.type.default() else CodeBlock.of("null"))
					.build()
			)
			type.addProperty(
				PropertySpec.builder(field.name, typeName, KModifier.PUBLIC)
					.initializer(field.name)
					.apply { field.kdoc()?.let { addKdoc("%L", it) } }
					.apply { if (field.type.unwrapped() == Type.Bytes) addAnnotation(serializableWith()) }
					.build()
			)
		}
		return type.primaryConstructor(constructor.build()).build()
	}

	private fun enumClass(decl: EnumDecl): TypeSpec {
		val type = ClassName(PACKAGE, decl.name)
		val unknown = decl.unknownName()
		val serializer = TypeSpec.objectBuilder("Serializer")
			.addModifiers(KModifier.INTERNAL)
			.superclass(ENUM_SERIALIZER.parameterizedBy(type))
			.addSuperclassConstructorParameter("%S, %T.entries, %T.%L, { it.value }", "$PACKAGE.${decl.name}", type, type, unknown)
			.build()
		val builder = TypeSpec.enumBuilder(decl.name)
			.addModifiers(KModifier.PUBLIC)
			.addAnnotation(AnnotationSpec.builder(SERIALIZABLE).addMember("with = %T.Serializer::class", type).build())
			.addKdoc("A value this kit doesn't know, from a later Hammer, reads as [%L].\n", unknown)
			.primaryConstructor(FunSpec.constructorBuilder().addParameter("value", STRING).build())
			.addProperty(
				PropertySpec.builder("value", STRING, KModifier.PUBLIC).initializer("value")
					.addKdoc("What Hammer calls it; empty for [%L].", unknown).build()
			)
			.addType(serializer)
		decl.values.forEach { value ->
			builder.addEnumConstant(pascalCase(value), TypeSpec.anonymousClassBuilder().addSuperclassConstructorParameter("%S", value).build())
		}
		builder.addEnumConstant(unknown, TypeSpec.anonymousClassBuilder().addSuperclassConstructorParameter("%S", "").build())
		return builder.build()
	}

	private fun operations(): FileSpec {
		val file = file("Operations")
		val groups = linkedMapOf<List<String>, TypeSpec.Builder>()
		fun group(path: List<String>): TypeSpec.Builder = groups.getOrPut(path) {
			val name = if (path.isEmpty()) ROOT else path.joinToString("") { pascalCase(it) } + "Operations"
			val builder = TypeSpec.objectBuilder(name).addModifiers(KModifier.PUBLIC)
			if (path.isEmpty()) {
				builder.addKdoc(
					"Hammer's operations, typed. Each function runs one operation, as [hammer.call] would, and throws\n" +
						"[hammer.OperationException] when it fails. A plugin may call only those its manifest's\n" +
						"`[permissions]` ask for: each function's documentation names what it needs.\n"
				)
			} else {
				group(path.dropLast(1)).addProperty(
					PropertySpec.builder(path.last(), ClassName(PACKAGE, name), KModifier.PUBLIC)
						.initializer("%T", ClassName(PACKAGE, name))
						.addKdoc("The `%L` operations.", path.joinToString("."))
						.build()
				)
			}
			builder
		}
		group(emptyList())
		api.operations.forEach { op -> group(op.segments.dropLast(1)).addFunction(function(op)) }
		groups.values.forEach { file.addType(it.build()) }
		return file.build()
	}

	private fun function(op: Operation): FunSpec {
		val params = op.params.sortedBy { !it.required }
		val fn = FunSpec.builder(camelCase(op.segments.last())).addModifiers(KModifier.PUBLIC)
		fn.addKdoc("%L\n\nNeeds %L in the manifest's `[permissions]`.\n", op.description.trimEnd(), op.permission)
		params.forEach { p ->
			fn.addParameter(
				ParameterSpec.builder(p.name, p.type.kotlin(p.required)).apply { if (!p.required) defaultValue("null") }.build()
			)
			p.kdoc()?.let { fn.addKdoc("@param %L %L\n", p.name, it) }
		}
		val input = CodeBlock.builder().add("%M {\n", BUILD_JSON_OBJECT).indent()
		params.forEach { p ->
			val value = if (p.type.unwrapped() == Type.Bytes) {
				CodeBlock.of("%M(%N, %T)", OPERATION_VALUE, p.name, BASE64_BYTES)
			} else {
				CodeBlock.of("%M(%N)", OPERATION_VALUE, p.name)
			}
			if (p.required) {
				input.addStatement("put(%S, %L)", p.name, value)
			} else {
				input.addStatement("if (%N != null) put(%S, %L)", p.name, p.name, value)
			}
		}
		input.unindent().add("}")
		val output = op.output
		if (output == null) {
			fn.addStatement("%M<%T>(%S, %L)", OPERATION, JSON_ELEMENT, op.name, input.build())
		} else {
			fn.returns(output.kotlin(true))
			fn.addStatement("return %M(%S, %L)", OPERATION, op.name, input.build())
		}
		return fn.build()
	}

	private fun serializableWith() = AnnotationSpec.builder(SERIALIZABLE).addMember("with = %T::class", BASE64_BYTES).build()

	private fun Type.kotlin(required: Boolean): TypeName {
		val name: TypeName = when (this) {
			is Type.Text -> STRING
			Type.Integer -> INT
			Type.Long -> LONG
			Type.Number -> DOUBLE
			Type.Bool -> BOOLEAN
			Type.Bytes -> BYTE_ARRAY
			Type.AnyJson -> JSON_ELEMENT
			is Type.ListOf -> LIST.parameterizedBy(item.kotlin(true))
			is Type.MapOf -> MAP.parameterizedBy(STRING, value.kotlin(true))
			is Type.Named -> ClassName(PACKAGE, this.name)
			is Type.Nullable -> return inner.kotlin(true).copy(nullable = true)
		}
		return if (required) name else name.copy(nullable = true)
	}

	private fun Type.default(): CodeBlock = when (this) {
		is Type.Text -> CodeBlock.of("%S", "")
		Type.Integer -> CodeBlock.of("0")
		Type.Long -> CodeBlock.of("0L")
		Type.Number -> CodeBlock.of("0.0")
		Type.Bool -> CodeBlock.of("false")
		Type.Bytes -> CodeBlock.of("byteArrayOf()")
		Type.AnyJson -> CodeBlock.of("%T", JSON_NULL)
		is Type.ListOf -> CodeBlock.of("emptyList()")
		is Type.MapOf -> CodeBlock.of("emptyMap()")
		is Type.Nullable -> CodeBlock.of("null")
		is Type.Named -> when (val decl = api.decls.getValue(name)) {
			is EnumDecl -> CodeBlock.of("%T.%L", ClassName(PACKAGE, name), pascalCase(decl.values.first()))
			is ObjectDecl -> CodeBlock.of("%T()", ClassName(PACKAGE, name))
		}
	}

	private fun Field.kdoc(): String? {
		val format = when ((type.unwrapped() as? Type.Text)?.format) {
			"date-time" -> "An ISO 8601 instant, such as `2026-03-01T09:30:00Z`."
			"date" -> "An ISO 8601 date, such as `2026-03-01`."
			else -> null
		}
		return listOfNotNull(doc, format).joinToString(" ").ifEmpty { null }
	}

	companion object {
		const val ROOT = "Hammer"
	}
}

fun Type.unwrapped(): Type = if (this is Type.Nullable) inner.unwrapped() else this

/** `read`, or `meta-write` → `metaWrite`. */
fun camelCase(name: String): String = pascalCase(name).replaceFirstChar(Char::lowercase)
