package hammer.codegen

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class GeneratorTest {
	private fun api(input: String, output: String) = readApi(
		"""{"operations": [{"name": "thing.get", "description": "Gets a thing.", "access": "read", "scope": "content",
			"input": $input, "output": $output}]}"""
	)

	private val noInput = """{"type": "object", "properties": {}, "x-name": "NoInput"}"""

	@Test
	fun `only an empty object output is no output`() {
		assertEquals(null, api(noInput, """{"type": "object", "properties": {}}""").operations.single().output)
		assertEquals(Type.ListOf(Type.Integer), api(noInput, """{"type": "array", "items": {"type": "integer"}}""").operations.single().output)
		assertEquals(
			Type.Nullable(Type.Text(null)),
			api(noInput, """{"anyOf": [{"type": "string"}, {"type": "null"}]}""").operations.single().output,
		)
	}

	@Test
	fun `a required nullable text input borrows with a lifetime`() {
		val api = api(
			"""{"type": "object", "properties": {"group": {"anyOf": [{"type": "string"}, {"type": "null"}]}}, "required": ["group"]}""",
			"""{"type": "object", "properties": {"id": {"type": "integer"}}, "required": ["id"], "x-name": "Thing"}""",
		)
		val file = File(createTempDirectory().toFile(), "ops.rs")
		RustGenerator(api).write(file)
		val rust = file.readText()
		assertContains(rust, "pub fn get(group: Option<&str>) -> crate::Result<Thing>")
		assertContains(rust, "struct Input<'a> {")
		assertContains(rust, "group: Option<&'a str>,")
	}
}
