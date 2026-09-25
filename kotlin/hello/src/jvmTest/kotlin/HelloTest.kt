import hammer.ActionRequest
import hammer.CommandRequest
import hammer.DiagnoseRequest
import hammer.ExportRequest
import hammer.FakeHammerHost
import hammer.diagnoseReply
import hammer.host
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class HelloTest {
	private val settings = buildJsonObject { put("greeting", "Ahoy") }

	private val fake = FakeHammerHost(
		mapOf(
			"scene.tree" to { _ ->
				buildJsonObject {
					putJsonArray("nodes") {
						add(buildJsonObject {
							put("id", 1); put("name", "Part One"); put("kind", "group")
							putJsonArray("children") {
								add(buildJsonObject { put("id", 2); put("name", "Storm"); put("kind", "scene") })
								add(buildJsonObject { put("id", 3); put("name", "Calm"); put("kind", "scene") })
							}
						})
					}
				}
			},
			"scene.read" to { input ->
				buildJsonObject { put("markdown", if (input["id"]!!.jsonPrimitive.int == 2) "Rain fell hard." else "Quiet.") }
			},
		),
	).also { host = it }

	private fun action(name: String, input: Map<String, JsonArray> = emptyMap(), button: String? = null, place: String = "project", id: Int? = null) =
		ActionRequest(name, "Storm", settings, kotlinx.serialization.json.JsonObject(input), ActionRequest.Context(place, id), button)

	@Test
	fun `exports, messages, and commands use the greeting setting`() {
		assertEquals("Ahoy, Storm!\nOne: 2 scenes\n", helloExport(ExportRequest("hello.txt", "Storm", chapters = listOf(ExportRequest.Chapter("One", listOf("a", "b"))), settings = settings)))
		assertEquals("Ahoy, Storm!", greet(action("greet")))
		assertEquals("Ahoy, Alice!", helloCommand(CommandRequest("hello", line = "Alice", settings = settings)))
	}

	@Test
	fun `the outline follows the scene tree`() {
		assertEquals("# Storm\n- Part One\n  - Storm\n  - Calm\n", outline(action("outline")))
	}

	@Test
	fun `words are counted in the chosen scenes, or the scene it was run from, with progress`() {
		assertEquals("# Word counts\n- Storm: 3\n", countWords(action("count", mapOf("scenes" to JsonArray(listOf(JsonPrimitive(2)))))))
		assertEquals("# Word counts\n- Calm: 1\n", countWords(action("count", place = "scene", id = 3)))
		assertEquals("# Word counts\n- Storm: 3\n- Calm: 1\n", countWords(action("count")))
		assertEquals(listOf("Counting Storm", "Counting Calm"), fake.progress.takeLast(2).map { it["message"]!!.jsonPrimitive.content })
	}

	@Test
	fun `waving shows buttons, and a button's id comes back`() {
		assertEquals(listOf("once", "twice"), wave(action("wave")).buttons.map { it.id })
		assertEquals("You waved twice", wave(action("wave", button = "twice")).message)
	}

	@Test
	fun `each very is found, at UTF-8 offsets in the reply`() {
		val request = DiagnoseRequest("very", listOf("Café very cold. Everyone was VERY."))
		val found = findVery(request)
		assertEquals(listOf("very ", "VERY"), found.map { request.paragraphs[0].substring(it.start, it.end) })
		assertEquals(
			"""{"diagnostics":[{"paragraph":0,"start":6,"end":11,"message":"“Very” rarely helps","fixes":[{"replacement":"","label":"Remove “very”"}]},""" +
				"""{"paragraph":0,"start":30,"end":34,"message":"“Very” rarely helps","fixes":[{"replacement":"","label":"Remove “very”"}]}]}""",
			diagnoseReply(request.paragraphs, found),
		)
	}
}
