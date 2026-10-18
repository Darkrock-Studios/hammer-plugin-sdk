package hammer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

class ActionTest {
	@Serializable
	class Step(val step: Int, val name: String = "")

	@Serializable
	class Picks(val who: EntityRef? = null, val cast: List<EntityRef> = emptyList())

	@Test
	fun `a reply leaves out what is unset`() {
		assertEquals("{}", ActionReply().toJson())
		assertEquals("""{"message":"Hi"}""", ActionReply.message("Hi").toJson())
		assertEquals("""{"page":{"markdown":"# Hi"}}""", ActionReply.document("# Hi").toJson())
		assertEquals(
			"""{"message":"Added","errors":{"":"No"},"open":{"place":"entry","id":12}}""",
			ActionReply(message = "Added", errors = mapOf("" to "No"), open = ActionReply.Open("entry", 12)).toJson(),
		)
	}

	@Test
	fun `a page has its fields, buttons, and state`() {
		val page = ActionReply.Page(
			title = "Who?",
			fields = listOf(
				ActionReply.Field.text("name", "Name", required = true),
				ActionReply.Field.text("want", "Wants", hint = "A line", format = TextFormat.RichExtended),
				ActionReply.Field.text("notes", "Notes", default = "None", multiline = true),
				ActionReply.Field.choice("set", "Set", listOf(ActionReply.Field.Option("quick", "Quick")), default = "quick"),
				ActionReply.Field.int("count", "Count", default = 8, min = 1),
				ActionReply.Field.bool("all", "All"),
				ActionReply.Field.scenes("scene", "Scene", multiple = false, default = listOf(3)),
				ActionReply.Field.scenes("scenes", "Scenes", required = true, default = listOf(3, 4)),
			),
			buttons = listOf(
				ActionReply.Button("back", "Back", validate = false),
				ActionReply.Button("next", "Next", primary = true, inline = true),
			),
			state = stateOf(Step(2, "Mara")),
		)
		assertEquals(
			"""{"page":{"title":"Who?","fields":[""" +
				"""{"key":"name","type":"text","label":"Name","required":true},""" +
				"""{"key":"want","type":"text","label":"Wants","hint":"A line","format":"rich-extended"},""" +
				"""{"key":"notes","type":"text","label":"Notes","default":"None","multiline":true},""" +
				"""{"key":"set","type":"choice","label":"Set","default":"quick","options":[{"value":"quick","label":"Quick"}]},""" +
				"""{"key":"count","type":"int","label":"Count","default":8,"min":1},""" +
				"""{"key":"all","type":"bool","label":"All"},""" +
				"""{"key":"scene","type":"scenes","label":"Scene","default":3,"multiple":false},""" +
				"""{"key":"scenes","type":"scenes","label":"Scenes","required":true,"default":[3,4]}],""" +
				""""buttons":[{"id":"back","label":"Back","validate":false},{"id":"next","label":"Next","primary":true,"inline":true}],""" +
				""""state":{"step":2,"name":"Mara"}}}""",
			ActionReply(page).toJson(),
		)
	}

	@Test
	fun `a request carries the last page's state back`() {
		val request = json.decodeFromString(
			ActionRequest.serializer(),
			"""{"action":"wave","project":"Storm","settings":{},"locale":null,"context":{"place":"project","id":null},""" +
				""""input":{},"button":"next","changed":null,"state":{"step":2,"name":"Mara"},"resumed":true}""",
		)
		assertEquals("Mara", request.state<Step>()?.name)
		assertNull(request.state<List<Int>>())
		assertTrue(request.resumed)

		val first = json.decodeFromString(
			ActionRequest.serializer(),
			"""{"action":"wave","project":"Storm","button":null,"changed":null,"state":null,"resumed":false}""",
		)
		assertNull(first.state)
		assertNull(first.state<Step>())
		assertFalse(first.resumed)
	}

	@Test
	fun `an entity field is one item unless multiple, leaving out what is unset`() {
		val mara = EntityRef("entry", 12)
		val fields = listOf(
			ActionReply.Field.entity("who", "Who"),
			ActionReply.Field.entity(
				"a", "First", required = true, kinds = listOf("entry"), entryTypes = listOf("person"), tags = listOf("pov"), default = mara,
			),
			ActionReply.Field.entity("cast", "Cast", multiple = true, default = mara),
			ActionReply.Field.entity("pair", "Pair", kinds = listOf("entry", "scene"), default = listOf(mara, EntityRef("scene", 3))),
			ActionReply.Field.entity("one", "One", multiple = false, default = listOf(EntityRef("note", 4))),
			ActionReply.Field.entity("none", "None", multiple = false, default = emptyList()),
		)
		assertEquals(
			"""[{"key":"who","type":"entity","label":"Who"},""" +
				"""{"key":"a","type":"entity","label":"First","required":true,"default":{"kind":"entry","id":12},""" +
				""""kinds":["entry"],"entry_types":["person"],"tags":["pov"]},""" +
				"""{"key":"cast","type":"entity","label":"Cast","default":[{"kind":"entry","id":12}],"multiple":true},""" +
				"""{"key":"pair","type":"entity","label":"Pair","default":[{"kind":"entry","id":12},{"kind":"scene","id":3}],""" +
				""""kinds":["entry","scene"],"multiple":true},""" +
				"""{"key":"one","type":"entity","label":"One","default":{"kind":"note","id":4}},""" +
				"""{"key":"none","type":"entity","label":"None"}]""",
			json.encodeToString(ListSerializer(ActionReply.Field.serializer()), fields),
		)
		assertEquals(listOf(false, false, true, true, false, false), fields.map { it.multiple })
		assertFailsWith<IllegalArgumentException> {
			ActionReply.Field.entity("two", "Two", multiple = false, default = listOf(mara, EntityRef("entry", 13)))
		}
	}

	@Test
	fun `a field read without multiple is a list for scenes and one item for an entity`() {
		fun field(text: String) = json.decodeFromString(ActionReply.Field.serializer(), text)
		assertTrue(field("""{"key":"s","type":"scenes","label":"S"}""").multiple)
		assertFalse(field("""{"key":"s","type":"scenes","label":"S","multiple":false}""").multiple)
		assertFalse(field("""{"key":"e","type":"entity","label":"E"}""").multiple)
		val many = field("""{"key":"e","type":"entity","label":"E","entry_types":["place"],"multiple":true}""")
		assertTrue(many.multiple)
		assertEquals(listOf("place"), many.entryTypes)
		assertEquals("""{"key":"e","type":"entity","label":"E","entry_types":["place"],"multiple":true}""", json.encodeToString(ActionReply.Field.serializer(), many))
	}

	@Test
	fun `entity input reads as EntityRef`() {
		fun picks(input: String) = json.decodeFromString(
			ActionRequest.serializer(),
			"""{"action":"meet","project":"Storm","input":$input}""",
		).input<Picks>()
		val picked = picks("""{"who":{"kind":"entry","id":12},"cast":[{"kind":"scene","id":3},{"kind":"event","id":4}]}""")
		assertEquals(EntityRef("entry", 12), picked.who)
		assertEquals(listOf(EntityRef("scene", 3), EntityRef("event", 4)), picked.cast)
		val none = picks("""{"who":null,"cast":[]}""")
		assertNull(none.who)
		assertEquals(emptyList(), none.cast)
	}
}
