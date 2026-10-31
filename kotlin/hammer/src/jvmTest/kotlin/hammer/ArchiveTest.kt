package hammer

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ArchiveTest {
	@Test
	fun `an archive lists its files in order, each with text or content`() {
		val archive = ExportArchive()
			.text("story.md", "# One\n\"Rain.\" é")
			.bytes("images/1.png", byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()))
			.base64("cover.jpg", "AP8=")
		assertEquals(
			"""{"files":[{"path":"story.md","text":"# One\n\"Rain.\" é"},""" +
				"""{"path":"images/1.png","content":"iVBORw=="},{"path":"cover.jpg","content":"AP8="}]}""",
			archive.toJson(),
		)
	}

	@Test
	fun `bytes are standard base64 with padding`() {
		val archive = ExportArchive()
			.bytes("1", "f".encodeToByteArray())
			.bytes("2", "fo".encodeToByteArray())
			.bytes("3", "foo".encodeToByteArray())
			.bytes("4", byteArrayOf(-5, -1, -65))
			.bytes("5", byteArrayOf())
		assertEquals(
			"""{"files":[{"path":"1","content":"Zg=="},{"path":"2","content":"Zm8="},{"path":"3","content":"Zm9v"},""" +
				"""{"path":"4","content":"+/+/"},{"path":"5","content":""}]}""",
			archive.toJson(),
		)
	}

	@Test
	fun `base64 is passed on as it is, never decoded and encoded again`() {
		// Decoding and encoding again would give Zg== and Zm8=.
		val archive = ExportArchive().base64("a", "Zh==").base64("b", "Zm8")
		assertEquals("""{"files":[{"path":"a","content":"Zh=="},{"path":"b","content":"Zm8"}]}""", archive.toJson())
	}

	@Test
	fun `an image's content is forwarded as Hammer sent it`() {
		val request = json.decodeFromString(
			ExportRequest.serializer(),
			"""{"format":"book.zip","projectName":"Storm","images":[""" +
				"""{"id":2,"name":"Cover","extension":"jpg","mimeType":"image/jpeg","content":"AP8="},""" +
				"""{"id":5,"name":"Map","extension":"png","mimeType":"image/png","content":"Zh=="}],"coverImageId":2}""",
		)
		val map = request.image(5)!!
		val archive = ExportArchive().image("images/5.png", map).image("cover.jpg", request.cover!!)
		assertEquals(
			"""{"files":[{"path":"images/5.png","content":"Zh=="},{"path":"cover.jpg","content":"AP8="}]}""",
			archive.toJson(),
		)
		assertEquals("Zh==", map.base64)
		assertFailsWith<IllegalArgumentException> { map.content }
		assertContentEquals(byteArrayOf(0, -1), request.cover!!.content)
	}

	@Test
	fun `each path rule refuses a file, naming its path`() {
		listOf(
			"" to "is empty",
			"/story.md" to "starts with /",
			"images\\1.png" to "has a backslash",
			"images//1.png" to "has an empty segment",
			"images/" to "has an empty segment",
			"./story.md" to "has a . segment",
			"images/../story.md" to "has a .. segment",
			"images/." to "has a . segment",
			"." to "has a . segment",
			".." to "has a .. segment",
		).forEach { (path, rule) ->
			val failure = assertFailsWith<IllegalArgumentException>(path) { ExportArchive().text(path, "x") }
			assertEquals("Archive path \"$path\" $rule", failure.message)
		}
	}

	@Test
	fun `a path already in the archive is refused`() {
		val archive = ExportArchive().text("story.md", "a")
		val failure = assertFailsWith<IllegalArgumentException> { archive.bytes("story.md", byteArrayOf(1)) }
		assertEquals("Archive path \"story.md\" is already in the archive", failure.message)
		archive.text("Story.md", "b").text("a", "c").text("ab", "d")
		assertEquals(
			"Archive path \"a\" is already in the archive",
			assertFailsWith<IllegalArgumentException> { archive.text("a", "e") }.message,
		)
		assertEquals(
			"""{"files":[{"path":"story.md","text":"a"},{"path":"Story.md","text":"b"},""" +
				"""{"path":"a","text":"c"},{"path":"ab","text":"d"}]}""",
			archive.toJson(),
		)
	}

	@Test
	fun `nested paths and dots inside a segment are fine`() {
		val archive = ExportArchive()
			.bytes("images/2026/a b.png", byteArrayOf(1))
			.text(".hidden/...", "")
			.text("a..b/c.d", "")
			.text("images/carte été.png", "")
		assertEquals(
			"""{"files":[{"path":"images/2026/a b.png","content":"AQ=="},{"path":".hidden/...","text":""},""" +
				"""{"path":"a..b/c.d","text":""},{"path":"images/carte été.png","text":""}]}""",
			archive.toJson(),
		)
	}

	@Test
	fun `an archive without files has no reply`() {
		val failure = assertFailsWith<IllegalArgumentException> { ExportArchive().toJson() }
		assertEquals("An archive needs at least one file", failure.message)
	}
}
