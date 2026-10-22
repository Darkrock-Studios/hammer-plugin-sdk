package hammer.codegen

import java.io.File

/**
 * Writes the Rust kit's `ops` module: a struct or enum for each type, and a function for each operation in a
 * module per name segment (`scene.meta.read` is `ops::scene::meta::read`). Required inputs are parameters;
 * an operation with optional inputs takes them last, in an `…Options` struct that has `Default`.
 */
class RustGenerator(private val api: Api) {
	private val out = StringBuilder()

	fun write(file: File) {
		out.clear()
		line("//! Hammer's operations, typed. Each function runs one operation, as [`crate::call`] would. A plugin may")
		line("//! call only those its manifest's `[permissions]` ask for: each function's documentation names what it needs.")
		line("//!")
		line("//! Generated from api/operations.json by kotlin/codegen: regenerate rather than edit.")
		line("#![allow(clippy::all, unused_imports)]")
		line()
		line("use serde::{Deserialize, Serialize};")
		line("use std::collections::BTreeMap;")
		line()
		api.reachable().forEach { decl ->
			when (decl) {
				// Every field has a default: Hammer sends them all, but a test's fake or an older host may not.
				is ObjectDecl -> struct(decl.name, decl.fields, derives = "Debug, Clone, Default, PartialEq, Serialize, Deserialize", container = "#[serde(default)]")
				is EnumDecl -> enum(decl)
			}
			line()
		}
		module(emptyList(), 0)
		file.writeText(out.toString().trimEnd() + "\n")
	}

	private fun module(path: List<String>, depth: Int) {
		val here = api.operations.filter { it.segments.dropLast(1) == path }
		here.forEach { function(it, depth) }
		val children = api.operations.filter { it.segments.size > path.size + 1 && it.segments.take(path.size) == path }
			.map { it.segments[path.size] }.distinct()
		children.forEach { child ->
			val indent = "    ".repeat(depth)
			line("${indent}/// The `${(path + child).joinToString(".")}` operations.")
			line("${indent}pub mod ${ident(snakeCase(child))} {")
			line("$indent    use super::*;")
			line()
			module(path + child, depth + 1)
			line("$indent}")
			line()
		}
	}

	private fun function(op: Operation, depth: Int) {
		val indent = "    ".repeat(depth)
		val required = op.params.filter { it.required }
		val optional = op.params.filterNot { it.required }
		val options = pascalCase(op.name) + "Options"
		if (optional.isNotEmpty()) {
			doc(indent, "The optional inputs of [`${ident(snakeCase(op.segments.last()))}`]; leave any out with `..Default::default()`.")
			struct(options, optional, derives = "Debug, Clone, Default, PartialEq, Serialize", indent = indent)
			line()
		}
		doc(indent, op.description.trimEnd())
		line("$indent///")
		doc(indent, "Needs ${op.permission} in the manifest's `[permissions]`.")
		val params = required.map { "${ident(snakeCase(it.name))}: ${it.type.borrowed()}" } +
			(if (optional.isNotEmpty()) listOf("options: $options") else emptyList())
		val output = op.output?.owned() ?: "()"
		val signature = "pub fn ${ident(snakeCase(op.segments.last()))}(${params.joinToString(", ")}) -> crate::Result<$output>"
		line("$indent$signature {")
		val inner = "$indent    "
		val lifetime = if (required.any { '&' in it.type.borrowed() }) "<'a>" else ""
		line("$inner#[derive(Serialize)]")
		line("${inner}struct Input$lifetime {")
		required.forEach { p ->
			rename(inner + "    ", p.name)
			if (p.type.unwrapped() == Type.Bytes) line("$inner    #[serde(serialize_with = \"crate::base64::serialize\")]")
			line("$inner    ${ident(snakeCase(p.name))}: ${p.type.borrowed().replace("&", "&'a ")},")
		}
		if (optional.isNotEmpty()) {
			line("$inner    #[serde(flatten)]")
			line("$inner    options: $options,")
		}
		line("$inner}")
		val fields = required.map { ident(snakeCase(it.name)) } + (if (optional.isNotEmpty()) listOf("options") else emptyList())
		val call = "crate::call(\"${op.name}\", &Input { ${fields.joinToString(", ")} })"
		if (op.output == null) {
			line("$inner$call.map(|_: serde_json::Value| ())")
		} else {
			line("$inner$call")
		}
		line("$indent}")
		line()
	}

	private fun struct(name: String, fields: List<Field>, derives: String, indent: String = "", container: String? = null) {
		line("$indent#[derive($derives)]")
		container?.let { line("$indent$it") }
		line("${indent}pub struct $name {")
		fields.forEach { f ->
			f.doc()?.let { doc("$indent    ", it) }
			rename("$indent    ", f.name)
			val optionalInput = !f.required
			if (optionalInput) line("$indent    #[serde(skip_serializing_if = \"Option::is_none\"${if ("Deserialize" in derives) ", default" else ""})]")
			if (f.type.unwrapped() == Type.Bytes) {
				line("$indent    #[serde(with = \"crate::base64::${if (f.type is Type.Nullable || optionalInput) "option" else "bytes"}\")]")
			}
			val type = if (optionalInput) "Option<${f.type.unwrapped().owned()}>" else f.type.owned()
			line("$indent    pub ${ident(snakeCase(f.name))}: $type,")
		}
		line("$indent}")
	}

	private fun enum(decl: EnumDecl) {
		line("/// A value this kit doesn't know, from a later Hammer, reads as [`${decl.name}::${decl.unknownName()}`].")
		line("#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]")
		line("pub enum ${decl.name} {")
		decl.values.forEachIndexed { i, value ->
			if (i == 0) line("    #[default]")
			line("    #[serde(rename = \"$value\")]")
			line("    ${pascalCase(value)},")
		}
		line("    /// A value this kit doesn't know. Hammer rejects it as an input.")
		line("    #[serde(other)]")
		line("    ${decl.unknownName()},")
		line("}")
	}

	private fun rename(indent: String, name: String) {
		if (snakeCase(name) != name) line("$indent#[serde(rename = \"$name\")]")
	}

	private fun Type.owned(): String = when (this) {
		is Type.Text -> "String"
		Type.Integer -> "i32"
		Type.Long -> "i64"
		Type.Number -> "f64"
		Type.Bool -> "bool"
		Type.Bytes -> "Vec<u8>"
		Type.AnyJson -> "serde_json::Value"
		is Type.ListOf -> "Vec<${item.owned()}>"
		is Type.MapOf -> "BTreeMap<String, ${value.owned()}>"
		is Type.Named -> name
		is Type.Nullable -> "Option<${inner.owned()}>"
	}

	/** A required parameter's type: borrowed where that saves the caller a clone. */
	private fun Type.borrowed(): String = when (this) {
		is Type.Text -> "&str"
		Type.Bytes -> "&[u8]"
		is Type.ListOf -> "&[${if (item is Type.Text) "&str" else item.owned()}]"
		is Type.MapOf -> "&BTreeMap<String, ${value.owned()}>"
		is Type.Nullable -> "Option<${inner.borrowed()}>"
		else -> owned()
	}

	private fun Field.doc(): String? {
		val format = when ((type.unwrapped() as? Type.Text)?.format) {
			"date-time" -> "An ISO 8601 instant, such as `2026-03-01T09:30:00Z`."
			"date" -> "An ISO 8601 date, such as `2026-03-01`."
			else -> null
		}
		return listOfNotNull(doc, format).joinToString(" ").ifEmpty { null }
	}

	private fun doc(indent: String, text: String) = text.lines().forEach { line("$indent/// $it".trimEnd()) }

	private fun line(text: String = "") {
		out.append(text).append('\n')
	}

	private fun ident(name: String) = if (name in KEYWORDS) "r#$name" else name

	private companion object {
		val KEYWORDS = setOf(
			"as", "break", "const", "continue", "crate", "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in",
			"let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return", "self", "static", "struct", "super",
			"trait", "true", "type", "unsafe", "use", "where", "while", "async", "await", "dyn", "abstract", "become",
			"box", "do", "final", "macro", "override", "priv", "typeof", "unsized", "virtual", "yield", "try", "gen",
		)
	}
}
