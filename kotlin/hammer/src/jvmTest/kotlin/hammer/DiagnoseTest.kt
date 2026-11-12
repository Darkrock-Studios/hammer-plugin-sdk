package hammer

import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

class DiagnoseTest {
	@Test
	fun `A request names its project`() {
		val request = json.decodeFromString<DiagnoseRequest>(
			"""{"diagnostics": "names", "paragraphs": ["Mara ran."], "language": "en", "project": "Storm", "settings": {}, "locale": null}""",
		)

		assertEquals("Storm", request.project)
	}

	@Test
	fun `Only a suggestion names its severity in the reply`() {
		val reply = diagnoseReply(
			listOf("It really is."),
			listOf(
				Diagnostic(0, 3, 9, "Wordy", severity = Severity.Suggestion),
				Diagnostic(0, 10, 12, "Wrong"),
			),
		)

		assertEquals(
			"""{"diagnostics":[{"paragraph":0,"start":3,"end":9,"message":"Wordy","severity":"suggestion","fixes":[]},""" +
				"""{"paragraph":0,"start":10,"end":12,"message":"Wrong","fixes":[]}]}""",
			reply,
		)
	}
}
