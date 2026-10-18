import hammer.ActionDriver
import hammer.ActionReply
import hammer.ActionRequest
import hammer.CommandRequest
import hammer.DiagnoseRequest
import hammer.ExportRequest
import hammer.FakeHammerHost
import hammer.diagnoseReply
import hammer.host
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

	private fun action(name: String) = ActionRequest(name, "Storm", settings)

	private fun driver(name: String, handle: (ActionRequest) -> ActionReply?, place: String = "project", id: Int? = null, locale: String? = null) =
		ActionDriver(name, handle, "Storm", settings, ActionRequest.Context(place, id), locale)

	@Test
	fun `exports, messages, and commands use the greeting setting`() {
		assertEquals("Ahoy, Storm!\nOne: 2 scenes\n", helloExport(ExportRequest("hello.txt", "Storm", chapters = listOf(ExportRequest.Chapter("One", listOf("a", "b"))), settings = settings)))
		assertEquals("Ahoy, Storm!", greet(action("greet")).message)
		assertEquals("Ahoy, Alice!", helloCommand(CommandRequest("hello", line = "Alice", settings = settings)))
	}

	@Test
	fun `the outline follows the scene tree`() {
		assertEquals("# Storm\n- Part One\n  - Storm\n  - Calm\n", outline(action("outline")).page?.markdown)
	}

	@Test
	fun `words are counted in the chosen scenes, or the scene it was run from, with progress`() {
		val chosen = driver("count", ::countWords).open().fill("scenes", listOf(2)).run()
		assertEquals("# Word counts\n- Storm: 3\n", chosen.page?.markdown)
		assertEquals("# Word counts\n- Calm: 1\n", driver("count", ::countWords, place = "scene", id = 3).open().run().page?.markdown)
		assertEquals("# Word counts\n- Storm: 3\n- Calm: 1\n", driver("count", ::countWords).open().run().page?.markdown)
		assertEquals(listOf("Counting Storm", "Counting Calm"), fake.progress.takeLast(2).map { it["message"]!!.jsonPrimitive.content })
	}

	@Test
	fun `a tip comes from the packaged tips`() {
		assert(tip().message in java.io.File("resources/tips.txt").readLines())
	}

	@Test
	fun `waving asks a name, refuses one with digits, and carries it to the next page`() {
		val wave = driver("wave", ::wave).open()
		wave.press("next")
		assertEquals(setOf("name"), wave.errors.keys)
		wave.fill("name", "R2D2").press("next")
		assertEquals(mapOf("name" to "A name has no digits"), wave.errors)

		wave.fill("name", " Mara ").press("next")
		assertEquals("Mara", wave.state<Wave>()?.name)
		assertEquals(listOf("back", "wave"), wave.page?.buttons?.map { it.id })
		wave.fill("times", 9).press("wave")
		assertEquals(setOf("times"), wave.errors.keys)

		wave.press("back")
		assertEquals(JsonPrimitive("Mara"), wave.values["name"])
		wave.press("next").fill("times", 2).press("wave")
		assertTrue(wave.ended)
		assertNull(wave.opened)
		assertEquals(listOf("Mara waved 2 times"), wave.messages)
	}

	@Test
	fun `waving is in French for a French UI`() {
		val wave = driver("wave", ::wave, locale = "fr-CA").open()
		assertEquals("Suivant", wave.page?.buttons?.single()?.label)
		wave.fill("name", "Mara").press("next").press("wave")
		assertEquals(listOf("Mara a salué"), wave.messages)
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
