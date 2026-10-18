package hammer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.Serializable

class ActionTest {
	@Serializable
	class Step(val step: Int, val name: String = "")

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
}
