package hammer

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class ActionDriverTest {
	@Serializable
	class Visit(val name: String, val want: String = "")

	@Serializable
	class Names(val name: String)

	@Serializable
	class Wants(val want: String)

	@Serializable
	class Meeting(val a: EntityRef? = null, val b: EntityRef? = null, val cast: List<EntityRef> = emptyList())

	private val nameField = ActionReply.Field.text("name", "Name", required = true)

	// Asks for a name on the manifest's page, then what they want, with Back; Next adds them and opens the entry.
	private fun visit(request: ActionRequest): ActionReply? {
		val visit = request.state<Visit>()
		return when (request.button) {
			null, "name" -> {
				val name = request.input<Names>().name.trim()
				if (name == "Nobody") return ActionReply(errors = mapOf("name" to "Pick someone"))
				wantPage(Visit(name, visit?.want.orEmpty()))
			}
			"back" -> ActionReply(
				ActionReply.Page(
					fields = listOf(ActionReply.Field.text("name", "Name", required = true, default = visit!!.name)),
					buttons = listOf(ActionReply.Button("name", "Next", primary = true)),
					state = stateOf(visit),
				),
			)
			"next" -> {
				val want = request.input<Wants>().want
				if (want == "nothing") return ActionReply(message = "Really?", errors = mapOf("" to "Everyone wants something"))
				ActionReply(message = "Added ${visit!!.name}", open = ActionReply.Open("entry", 12))
			}
			"cancel" -> null
			else -> error("No button ${request.button}")
		}
	}

	private fun wantPage(visit: Visit) = ActionReply(
		ActionReply.Page(
			title = "${visit.name}: wants",
			fields = listOf(ActionReply.Field.text("want", "Wants", required = true, default = visit.want, format = TextFormat.Rich)),
			buttons = listOf(
				ActionReply.Button("back", "Back", validate = false),
				ActionReply.Button("cancel", "Cancel", validate = false),
				ActionReply.Button("next", "Add", primary = true),
			),
			state = stateOf(visit),
		),
	)

	private fun driver() = ActionDriver("visit", ::visit, fields = listOf(nameField)).open()

	@Test
	fun `the manifest's page is checked by Hammer before the first call`() {
		val driver = driver()
		assertEquals(mapOf("name" to JsonPrimitive("")), driver.values)
		driver.run()
		assertEquals(setOf("name"), driver.errors.keys)
		assertTrue(driver.requests.isEmpty())

		driver.fill("name", "Mara").run()
		assertEquals("Mara: wants", driver.page?.title)
		assertEquals(emptyMap(), driver.errors)
		assertNull(driver.requests.single().button)
		assertNull(driver.requests.single().state)
		assertFailsWith<IllegalStateException> { driver.run() }
	}

	@Test
	fun `state is carried forward, and Back skips the checks`() {
		val driver = driver().fill("name", "Mara").run()
		driver.press("next")
		assertEquals(setOf("want"), driver.errors.keys)
		assertEquals(1, driver.requests.size)

		driver.fill("want", "To go *home*").press("back")
		assertEquals("Mara", driver.state<Visit>()?.name)
		assertEquals(JsonPrimitive("Mara"), driver.values["name"])
		assertEquals("To go *home*", driver.requests.last().input["want"]?.jsonPrimitive?.content)

		driver.press("name")
		assertEquals(JsonPrimitive(""), driver.values["want"])
	}

	@Test
	fun `errors keep the page, its values, and its state`() {
		val driver = driver().fill("name", "Nobody").run()
		assertEquals(mapOf("name" to "Pick someone"), driver.errors)
		assertEquals(JsonPrimitive("Nobody"), driver.values["name"])
		assertNull(driver.page?.title)

		driver.fill("name", "Mara").run().fill("want", "nothing").press("next")
		assertEquals(mapOf("" to "Everyone wants something"), driver.errors)
		assertEquals(listOf("Really?"), driver.messages)
		assertEquals("Mara: wants", driver.page?.title)
		assertEquals(JsonPrimitive("nothing"), driver.values["want"])
		assertEquals("Mara", driver.state<Visit>()?.name)
	}

	@Test
	fun `errors for no field of the page are shown for the whole page`() {
		val driver = ActionDriver(
			"odd",
			{ ActionReply(errors = mapOf("name" to "Pick someone", "age" to "Too old", "" to "Try again")) },
			fields = listOf(nameField),
		).open()
		driver.fill("name", "Mara").run()
		assertEquals(mapOf("name" to "Pick someone", "" to "Too old\nTry again"), driver.errors)
	}

	@Test
	fun `errors with no page to keep end the action`() {
		val driver = ActionDriver("none", { ActionReply(errors = mapOf("" to "Nothing to do")) }).open()
		assertTrue(driver.ended)
		assertEquals(listOf("Nothing to do"), driver.messages)
	}

	@Test
	fun `opening again starts afresh`() {
		var calls = 0
		val driver = ActionDriver("again", { request ->
			calls++
			if (request.button == null) {
				ActionReply(ActionReply.Page(fields = listOf(nameField), buttons = listOf(ActionReply.Button("go", "Go")), state = stateOf(calls)))
			} else {
				null
			}
		}).open()
		driver.fill("name", "Mara").open()
		assertEquals(JsonObject(emptyMap()), driver.requests.last().input)
		assertNull(driver.requests.last().state)
		assertEquals(2, driver.state<Int>())
	}

	@Test
	fun `open ends the action, as does a reply of nothing`() {
		val driver = driver().fill("name", "Mara").run().fill("want", "Home").press("next")
		assertTrue(driver.ended)
		assertEquals("entry" to 12, driver.opened?.let { it.place to it.id })
		assertEquals(listOf("Added Mara"), driver.messages)
		assertNull(driver.page)
		assertFailsWith<IllegalStateException> { driver.press("next") }

		val cancelled = driver().fill("name", "Mara").run().press("cancel")
		assertTrue(cancelled.ended)
		assertNull(cancelled.opened)
	}

	@Test
	fun `a resumed run says so on its next call only`() {
		val driver = driver().fill("name", "Mara").run().fill("want", "Home").resume()
		assertEquals(JsonPrimitive("Home"), driver.values["want"])
		driver.press("back")
		assertTrue(driver.requests.last().resumed)
		driver.press("name")
		assertFalse(driver.requests.last().resumed)
	}

	@Test
	fun `an action without fields is called when opened`() {
		val driver = ActionDriver("hi", { ActionReply.message("Hi from ${it.project}") }, project = "Storm").open()
		assertTrue(driver.ended)
		assertEquals(listOf("Hi from Storm"), driver.messages)
	}

	@Test
	fun `Hammer's checks cover numbers, choices, and scenes`() {
		val fields = listOf(
			ActionReply.Field.int("count", "Count", min = 2, max = 4),
			ActionReply.Field.choice("set", "Set", listOf(ActionReply.Field.Option("quick", "Quick"), ActionReply.Field.Option("full", "Full"))),
			ActionReply.Field.scenes("scenes", "Scenes", required = true),
			ActionReply.Field.scenes("scene", "Scene", multiple = false),
			ActionReply.Field.bool("all", "All"),
		)
		val driver = ActionDriver("check", { null }, fields = fields).open()
		assertEquals(
			mapOf("count" to JsonPrimitive(2), "set" to JsonPrimitive("quick"), "scenes" to JsonArray(emptyList()), "scene" to JsonNull, "all" to JsonPrimitive(false)),
			driver.values,
		)
		driver.fill("count", 5).fill("set", "slow").run()
		assertEquals(setOf("count", "set", "scenes"), driver.errors.keys)
		driver.fill("count", 4).fill("set", "full").fill("scenes", listOf(3)).run()
		assertTrue(driver.ended)
	}

	@Test
	fun `entity fields start from their defaults, and Hammer checks what it can`() {
		val mara = EntityRef("entry", 12)
		val fields = listOf(
			ActionReply.Field.entity("a", "First", required = true, kinds = listOf("entry"), entryTypes = listOf("person"), default = mara),
			ActionReply.Field.entity("b", "Second", required = true, kinds = listOf("entry")),
			ActionReply.Field.entity("cast", "Cast", required = true, multiple = true),
			ActionReply.Field.entity("where", "Where", kinds = listOf("scene"), default = EntityRef("entry", 4)),
			ActionReply.Field.entity("seen", "Seen", kinds = listOf("scene"), default = listOf(EntityRef("scene", 3), mara)),
		)
		var met: Meeting? = null
		val driver = ActionDriver("meet", { met = it.input<Meeting>(); null }, fields = fields).open()
		val maraJson = buildJsonObject { put("kind", "entry"); put("id", 12) }
		val sceneJson = buildJsonObject { put("kind", "scene"); put("id", 3) }
		assertEquals(
			mapOf("a" to maraJson, "b" to JsonNull, "cast" to JsonArray(emptyList()), "where" to JsonNull, "seen" to JsonArray(listOf(sceneJson))),
			driver.values,
		)

		driver.run()
		assertEquals(mapOf("b" to "Required", "cast" to "Required"), driver.errors)
		driver.fill("b", EntityRef("scene", 3)).fill("cast", listOf(mara)).fill("where", JsonPrimitive(3)).fill("seen", null).run()
		assertEquals(mapOf("b" to "Not one of its kinds", "where" to "Not an item", "seen" to "Not an item"), driver.errors)
		driver.fill("a", buildJsonObject { put("kind", "entry"); put("id", "12") }).fill("b", EntityRef("chapter", 1)).run()
		assertEquals(setOf("a", "b", "where", "seen"), driver.errors.keys)

		// Entry types are the project's to check, so a place passes here.
		driver.fill("a", EntityRef("entry", 99)).fill("b", EntityRef("entry", 13)).fill("where", null).fill("seen", emptyList<EntityRef>()).run()
		assertTrue(driver.ended)
		assertEquals(listOf(EntityRef("entry", 99), EntityRef("entry", 13)), listOf(met?.a, met?.b))
		assertEquals(listOf(mara), met?.cast)
	}

	@Test
	fun `Hammer refuses an entity field it cannot show`() {
		fun refused(field: ActionReply.Field) = assertFailsWith<IllegalStateException> { ActionDriver("meet", { null }, fields = listOf(field)).open() }
		fun parsed(text: String) = json.decodeFromString(ActionReply.Field.serializer(), text)
		refused(ActionReply.Field.entity("a", "A", kinds = listOf("chapter")))
		refused(ActionReply.Field.entity("a", "A", entryTypes = listOf("villain")))
		refused(ActionReply.Field.entity("a", "A", kinds = listOf("scene"), entryTypes = listOf("person")))
		refused(parsed("""{"key":"a","type":"entity","label":"A","default":[{"kind":"entry","id":1}]}"""))
		refused(parsed("""{"key":"a","type":"entity","label":"A","multiple":true,"default":{"kind":"entry","id":1}}"""))
		refused(parsed("""{"key":"a","type":"entity","label":"A","default":{"kind":"entry"}}"""))
		ActionDriver("meet", { null }, fields = listOf(ActionReply.Field.entity("a", "A", entryTypes = listOf("person")))).open()
	}

	@Test
	fun `a reply Hammer would refuse fails the test`() {
		val big = ActionDriver("big", { ActionReply(ActionReply.Page(state = JsonPrimitive("x".repeat(1024 * 1024)))) })
		assertFailsWith<IllegalStateException> { big.open() }
		val nowhere = ActionDriver("nowhere", { ActionReply(open = ActionReply.Open("chapter", 1)) })
		assertFailsWith<IllegalStateException> { nowhere.open() }
	}

	@Test
	fun `the manifest's fields are read from it`() {
		val manifest = Files.createTempFile("manifest", ".toml").toFile().apply {
			writeText(
				"""
				id = "visit"
				api = 1

				[[actions]]
				name = "other"
				label = "Other"

				[[actions]]
				name = "visit"
				label = "Visit"
				keep = true

				[[actions.field]]
				key = "name"
				type = "text"
				label = "Name"
				required = true

				[[actions.field]]
				key = "set"
				type = "choice"
				label = "Set"
				default = "full"
				options = [
					{ value = "quick", label = "Quick" },
					{ value = "full", label = "Full" },
				]

				[[actions.field]]
				key = "count"
				type = "int"
				label = "Count"
				default = 8

				[[actions.field]]
				key = "who"
				type = "entity"
				label = "Who"
				kinds = ["entry"]
				entry_types = ["person"]
				default = { kind = "entry", id = 12 }
				""".trimIndent(),
			)
		}
		val fields = ActionDriver.manifestFields("visit", manifest)
		assertEquals(listOf("name", "set", "count", "who"), fields.map { it.key })
		assertEquals(listOf("person"), fields[3].entryTypes)
		assertFalse(fields[3].multiple)
		assertTrue(fields[0].required)
		assertEquals(listOf("quick", "full"), fields[1].options.map { it.value })
		val driver = ActionDriver("visit", ::visit, fields = fields).open()
		assertEquals(JsonPrimitive("full"), driver.values["set"])
		assertEquals(JsonPrimitive(8), driver.values["count"])
		assertEquals(buildJsonObject { put("kind", "entry"); put("id", 12) }, driver.values["who"])
		assertEquals(emptyList(), ActionDriver.manifestFields("other", manifest))
		assertFailsWith<IllegalStateException> { ActionDriver.manifestFields("missing", manifest) }
	}

	@Test
	fun `the request carries the driver's project, settings, place, and locale`() {
		val settings = buildJsonObject { put("greeting", "Ahoy") }
		val driver = ActionDriver(
			"hi", { ActionReply.message("Hi") }, project = "Storm", settings = settings,
			context = ActionRequest.Context("scene", 3), locale = "fr",
		).open()
		val request = driver.requests.single()
		assertEquals(listOf("hi", "Storm", "scene", "fr"), listOf(request.action, request.project, request.context.place, request.locale))
		assertEquals(3, request.context.id)
		assertEquals(settings, request.settings)
	}
}
