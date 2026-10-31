package hammer

import hammer.ops.EntryKind
import hammer.ops.Hammer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OperationsTest {
	private val inputs = mutableListOf<Pair<String, JsonObject>>()

	private fun answering(name: String, output: JsonObject) {
		host = FakeHammerHost(mapOf(name to { input -> inputs += name to input; output }))
	}

	@Test
	fun `a typed call sends its parameters and reads its output`() {
		answering("scene.read", buildJsonObject {
			put("id", 7)
			put("name", "Storm")
			put("archived", false)
			put("markdown", "Rain fell.")
			put("wordCount", 2)
			put("extra", "ignored")
			putJsonObject("meta") {
				put("outline", "")
				put("notes", "")
				putJsonArray("tags") { add("draft") }
				put("currentDraft", "")
				put("created", JsonPrimitive(null as String?))
				put("lastEdited", "2026-03-01T09:30:00Z")
				putJsonArray("references") { add(3) }
				putJsonArray("dismissedReferences") {}
			}
		})
		val scene = Hammer.scene.read("Novel", id = 7)
		assertEquals(buildJsonObject { put("project", "Novel"); put("id", 7) }, inputs.single().second)
		assertEquals("Rain fell." to listOf(3), scene.markdown to scene.meta.references)
		assertEquals(null, scene.meta.created)
	}

	@Test
	fun `optional parameters are left out unless given, and enums go by their names`() {
		answering("entry.list", buildJsonObject { putJsonArray("entries") {} })
		Hammer.entry.list("Novel")
		Hammer.entry.list("Novel", type = EntryKind.Person)
		assertEquals(
			listOf(buildJsonObject { put("project", "Novel") }, buildJsonObject { put("project", "Novel"); put("type", "person") }),
			inputs.map { it.second },
		)
	}

	@Test
	fun `bytes travel as base64 both ways`() {
		answering("project.export", buildJsonObject {
			put("fileName", "novel.txt")
			put("mimeType", "text/plain")
			put("content", "Zm9vYmFy")
		})
		assertContentEquals("foobar".encodeToByteArray(), Hammer.project.export("Novel", "plaintext.txt").content)

		answering("image.get", buildJsonObject { put("id", 3); put("name", "Map"); put("extension", "png"); put("content", "AP8=") })
		assertContentEquals(byteArrayOf(0, -1), Hammer.image.get("Novel", 3).content)

		answering("image.add", buildJsonObject { put("id", 3); put("name", "Map"); put("extension", "png"); put("inUse", false) })
		assertEquals(3, Hammer.image.add("Novel", "Map", "png", byteArrayOf(0, -1)).id)
		assertEquals(JsonPrimitive("AP8="), inputs.last().second["content"])
	}

	@Test
	fun `a value this kit doesn't know reads as Unknown`() {
		answering("entry.list", buildJsonObject {
			putJsonArray("entries") {
				add(buildJsonObject { put("id", 1); put("name", "Mara"); put("type", "person") })
				add(buildJsonObject { put("id", 2); put("name", "The Guild"); put("type", "organization") })
			}
		})
		assertEquals(listOf(EntryKind.Person, EntryKind.Unknown), Hammer.entry.list("Novel").entries.map { it.type })
		assertFailsWith<IllegalArgumentException> { Hammer.entry.list("Novel", type = EntryKind.Unknown) }
	}

	@Test
	fun `a failed operation throws`() {
		host = FakeHammerHost()
		val failure = assertFailsWith<OperationException> { Hammer.project.list() }
		assertEquals("NotFound", failure.kind)
	}
}
