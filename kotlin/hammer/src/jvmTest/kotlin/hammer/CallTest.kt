package hammer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CallTest {
	private fun replying(reply: String) = object : HammerHost {
		override fun dispatch(request: String) = reply
		override fun cacheGet(key: ByteArray): ByteArray? = null
		override fun cacheSet(key: ByteArray, value: ByteArray) = Unit
		override fun log(level: LogLevel, message: String) = Unit
	}

	private fun text(reply: String): String {
		host = replying(reply)
		return callJsonText("scene.read", JsonObject(emptyMap()))
	}

	@Test
	fun `the output's text is passed on as it is`() {
		host = FakeHammerHost(mapOf("scene.read" to { _ -> buildJsonObject { put("markdown", "“Rain,” she said. {}") } }))
		assertEquals("""{"markdown":"“Rain,” she said. {}"}""", callJsonText("scene.read", JsonObject(emptyMap())))
		assertEquals(""""a \"quoted\" }"""", text(""" { "output" : "a \"quoted\" }" } """))
		assertEquals("[1,2]", text("""{"output":[1,2]}"""))
	}

	@Test
	fun `no output is null, and a failure throws`() {
		assertEquals("null", text("{}"))
		val failure = assertFailsWith<OperationException> { text("""{"error":{"kind":"NotFound","message":"No scene 3"}}""") }
		assertEquals("NotFound" to "No scene 3", failure.kind to failure.message)
	}
}
