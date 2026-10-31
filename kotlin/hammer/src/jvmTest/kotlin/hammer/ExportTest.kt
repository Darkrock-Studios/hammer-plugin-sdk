package hammer

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class ExportTest {
	@Test
	fun `an export request carries its images and cover`() {
		val request = json.decodeFromString(
			ExportRequest.serializer(),
			"""{"format":"book.epub","projectName":"Storm","language":"en","topLevelAsChapters":true,""" +
				""""chapters":[{"name":"One","scenes":[],"prose":[[{"type":"paragraph","spans":[{"text":"Rain."}]},""" +
				"""{"type":"image","id":5,"name":"Map"}],[{"type":"paragraph","spans":[{"text":"Map"}]}]]}],""" +
				""""images":[{"id":2,"name":"Cover","extension":"jpg","mimeType":"image/jpeg","content":"AP8="},""" +
				"""{"id":5,"name":"Map","extension":"png","mimeType":"image/png","content":"iVBORw=="}],""" +
				""""coverImageId":2,"settings":{},"locale":null}""",
		)
		assertEquals(listOf(2, 5), request.images.map { it.id })
		val map = request.image(5)!!
		assertEquals(listOf("Map", "png", "image/png"), listOf(map.name, map.extension, map.mimeType))
		assertContentEquals(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()), map.content)
		assertNull(request.image(3))
		assertEquals(2, request.coverImageId)
		assertEquals("Cover", request.cover?.name)
		assertContentEquals(byteArrayOf(0, -1), request.cover?.content)

		val prose = request.chapters.single().prose
		assertEquals(2, prose.size)
		assertEquals(listOf(null, 5, null), prose.flatten().map(::proseImageId))
		assertEquals(listOf(5), request.chapters.flatMap { it.prose }.flatten().mapNotNull(::proseImageId))
	}

	@Test
	fun `a request from a host without images has none`() {
		val request = json.decodeFromString(
			ExportRequest.serializer(),
			"""{"format":"hello.txt","projectName":"Storm","chapters":[{"name":"One","scenes":["![image](hammer://image/5)"]}],"settings":{}}""",
		)
		assertTrue(request.images.isEmpty())
		assertNull(request.coverImageId)
		assertNull(request.cover)
		assertNull(request.image(5))
		assertTrue(request.chapters.single().prose.isEmpty())
	}

	@Test
	fun `a cover not among the images is none`() {
		val request = json.decodeFromString(
			ExportRequest.serializer(),
			"""{"format":"book.epub","projectName":"Storm","images":[],"coverImageId":9}""",
		)
		assertNull(request.cover)
	}

	@Test
	fun `only an image block has an image id`() {
		listOf(
			"""{"type":"paragraph","spans":[{"text":"Map"}]}""", """{"type":"image","name":"Map"}""",
			"""{"type":"image","id":"5"}""", """{"id":5}""", """[]""", """"image"""", "null",
		).forEach { assertNull(proseImageId(Json.parseToJsonElement(it)), it) }
		assertEquals(7, proseImageId(Json.parseToJsonElement("""{"name":"Map","id":7,"type":"image"}""")))
	}
}
