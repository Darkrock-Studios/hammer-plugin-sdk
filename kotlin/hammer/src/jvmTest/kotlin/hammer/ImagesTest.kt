package hammer

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ImagesTest {
	@Test
	fun `a reference names its image id`() {
		assertEquals(3, parseImageReference("hammer://image/3"))
		assertEquals(3, parseImageReference("<hammer://image/3>"))
		assertEquals(7, parseImageReference("hammer://image/007"))
		assertEquals(42, parseImageReference(imageReference(42)))
		assertEquals("hammer://image/42", imageReference(42))
		assertEquals(Int.MAX_VALUE, parseImageReference("hammer://image/2147483647"))
	}

	@Test
	fun `anything else names no image`() {
		listOf(
			"https://example.com/3.png", "hammer://scene/3", "hammer://image/", "hammer://image/3x",
			"hammer://image/-3", "hammer://image/+3", "hammer://image/3 ", " hammer://image/3",
			"hammer://image/99999999999", "hammer://image/2147483648", "<hammer://image/3", "hammer://image/3>",
			"<<hammer://image/3>>", "<>", "", "hammer://image/３",
		).forEach { assertEquals(null, parseImageReference(it), it) }
	}

	@Test
	fun `finds every referenced id`() {
		val text = "![a](hammer://image/1)\n\nSee ![b](<hammer://image/12> \"t\") and [c]: hammer://image/7"
		assertEquals(listOf(1, 12, 7), findImageIds(text).toList())
		assertEquals(listOf(9, 2), findImageIds("hammer://image/9 hammer://image/2 hammer://image/9").toList())
		assertEquals(setOf(5), findImageIds("```\n![x](hammer://image/5)\n```\n![y](hammer://image/5)"))
		assertEquals(setOf(3), findImageIds("A [link](hammer://image/3)."))
		assertEquals(setOf(4), findImageIds("éhammer://image/4é"))
		assertEquals(emptySet(), findImageIds("No images here."))
	}

	@Test
	fun `an id runs to its last digit and the scheme stands alone`() {
		assertEquals(setOf(12), findImageIds("![a](hammer://image/12)"))
		assertEquals(setOf(12), findImageIds("hammer://image/12"))
		assertEquals(setOf(12), findImageIds("hammer://image/12.png"))
		listOf(
			"hammer://image/12x", "hammer://image/12_", "xhammer://image/3", "_hammer://image/3", "+hammer://image/3",
			".hammer://image/3", "-hammer://image/3", "9hammer://image/3", "hammer://image/", "hammer://image/x",
		).forEach { assertEquals(emptySet(), findImageIds(it), it) }
	}

	@Test
	fun `an id too large for an Int names no image`() {
		assertEquals(setOf(3), findImageIds("![a](hammer://image/3) ![b](hammer://image/99999999999)"))
	}

	@Test
	fun `getImage reads the image a reference names`() {
		val inputs = mutableListOf<JsonObject>()
		host = FakeHammerHost(mapOf("image.get" to { input ->
			inputs += input
			buildJsonObject { put("id", 3); put("name", "Map"); put("extension", "png"); put("content", "AP8=") }
		}))
		val image = getImage("Novel", "<hammer://image/3>")
		assertEquals(buildJsonObject { put("project", "Novel"); put("id", 3) }, inputs.single())
		assertEquals("Map" to "png", image.name to image.extension)
		assertContentEquals(byteArrayOf(0, -1), image.content)

		val failure = assertFailsWith<OperationException> { getImage("Novel", "https://example.com/3.png") }
		assertEquals("InvalidInput", failure.kind)
		assertEquals(1, inputs.size)
	}
}
