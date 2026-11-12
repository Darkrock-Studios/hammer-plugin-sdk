package hammer

import hammer.languagetool.CheckRequest
import hammer.languagetool.LanguageTool
import hammer.languagetool.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LanguageToolTest {

	private val reply = """
		{
			"software": {"name": "LanguageTool", "version": "6.8"},
			"warnings": {"incompleteResults": false},
			"language": {"name": "English (US)", "code": "en-US",
				"detectedLanguage": {"name": "English (US)", "code": "en-US", "confidence": 0.98}},
			"matches": [{
				"message": "Did you mean \"is\"?", "shortMessage": "Agreement",
				"replacements": [{"value": "is"}, {"value": "was", "shortDescription": "past"}],
				"offset": 6, "length": 3,
				"context": {"text": "😀 It are fine.", "offset": 6, "length": 3},
				"sentence": "😀 It are fine.",
				"type": {"typeName": "Other"},
				"rule": {"id": "IT_VBZ", "subId": "1", "description": "Agreement", "issueType": "grammar",
					"category": {"id": "GRAMMAR", "name": "Grammar"},
					"urls": [{"value": "https://example.com/rule"}]},
				"ignoreForIncompleteSentence": false, "contextForSureMatch": 0
			}]
		}
	""".trimIndent()

	@Test
	fun `A check posts the form to the service and reads its matches`() {
		val fake = FakeHammerHost(services = mapOf("languagetool" to { _ -> ServerResponse(200, reply) }))
		host = fake
		val text = "😀 It are fine."

		val checked = LanguageTool.check(CheckRequest(text, language = "en-US", level = Level.Picky))

		val call = fake.serviceCalls.single()
		assertEquals(listOf("languagetool", "POST", "/v2/check"), listOf(call.service, call.method, call.path))
		assertEquals("application/x-www-form-urlencoded", call.headers["Content-Type"])
		assertEquals("text=%F0%9F%98%80+It+are+fine.&language=en-US&level=picky", call.bodyText)
		val found = checked.matches.single()
		assertEquals("are", found.textIn(text))
		assertEquals(listOf("is", "was"), found.replacements.map { it.value })
		assertEquals("GRAMMAR", found.rule.category.id)
		assertEquals("https://example.com/rule", found.rule.urls.single().value)
		assertEquals(0.98, checked.language?.detectedLanguage?.confidence)
	}

	@Test
	fun `Options join their lists and leave out the empty ones`() {
		val request = CheckRequest(
			"Hi",
			motherTongue = "de",
			preferredVariants = listOf("en-GB", "de-AT"),
			disabledRules = listOf("WHITESPACE_RULE"),
			enabledOnly = true,
		)

		assertEquals(
			"text=Hi&language=auto&motherTongue=de&preferredVariants=en-GB%2Cde-AT&disabledRules=WHITESPACE_RULE&enabledOnly=true",
			request.form(),
		)
		assertEquals("text=Hi&language=auto", CheckRequest("Hi", language = " ").form())
	}

	@Test
	fun `Languages are read from the service`() {
		val fake = FakeHammerHost(
			services = mapOf("languagetool" to { _ -> ServerResponse(200, """[{"name": "German (Austria)", "code": "de", "longCode": "de-AT"}]""") }),
		)
		host = fake

		val languages = LanguageTool.languages()

		assertEquals("de-AT", languages.single().longCode)
		assertEquals("GET" to "/v2/languages", fake.serviceCalls.single().let { it.method to it.path })
		assertNull(fake.serviceCalls.single().body)
	}

	@Test
	fun `A status other than success throws its kind with the server's message`() {
		host = FakeHammerHost(services = mapOf("languagetool" to { _ -> ServerResponse(429, "") }))
		assertEquals(ServerErrorKind.RateLimited, assertFailsWith<ServerException> { LanguageTool.check(CheckRequest("Hi")) }.kind)

		val tooLarge = """{"error": "api_services_error_toolarge", "displayMessage": "Too large"}"""
		host = FakeHammerHost(services = mapOf("languagetool" to { _ -> ServerResponse(413, tooLarge) }))
		val refused = assertFailsWith<ServerException> { LanguageTool.check(CheckRequest("Hi")) }
		assertEquals(ServerErrorKind.Rejected to "Too large", refused.kind to refused.message)

		host = FakeHammerHost(services = mapOf("languagetool" to { _ -> ServerResponse(502, "") }))
		val failed = assertFailsWith<ServerException> { LanguageTool.check(CheckRequest("Hi")) }
		assertEquals(ServerErrorKind.ServiceFailed to "The service replied 502", failed.kind to failed.message)

		host = FakeHammerHost(services = mapOf("languagetool" to { _ -> ServerResponse(200, "<html>") }))
		assertEquals(ServerErrorKind.Invalid, assertFailsWith<ServerException> { LanguageTool.check(CheckRequest("Hi")) }.kind)
	}

	@Test
	fun `Hammer's own failures arrive as their kinds`() {
		host = FakeHammerHost(
			services = mapOf("languagetool" to { _ -> throw ServerException(ServerErrorKind.NotSignedIn, "Sign in") }),
		)
		val signedOut = assertFailsWith<ServerException> { LanguageTool.check(CheckRequest("Hi")) }
		assertEquals(ServerErrorKind.NotSignedIn to "Sign in", signedOut.kind to signedOut.message)

		host = FakeHammerHost()
		assertEquals(ServerErrorKind.NotPermitted, assertFailsWith<ServerException> { LanguageTool.languages() }.kind)
	}

	@Test
	fun `The fake refuses what Hammer refuses and drops the headers it would`() {
		val fake = FakeHammerHost(services = mapOf("languagetool" to { _ -> ServerResponse(200, "") }))
		host = fake

		listOf("DELETE" to "/v2/check", "GET" to "/v2/../admin", "GET" to "v2/check").forEach { (method, path) ->
			assertEquals(ServerErrorKind.Invalid, assertFailsWith<ServerException> { serverRequest("languagetool", method, path) }.kind)
		}
		serverRequest("languagetool", "post", "/v2/check", mapOf("Authorization" to "Bearer x", "accept" to "application/json"))
		assertEquals(mapOf("accept" to "application/json"), fake.serviceCalls.last().headers)
	}

	@Test
	fun `A low-level request returns any status`() {
		val fake = FakeHammerHost(services = mapOf("languagetool" to { call -> ServerResponse(404, call.path) }))
		host = fake

		val response = serverRequest("languagetool", "GET", "/v2/missing", mapOf("Accept" to "text/plain"))

		assertEquals(404 to "/v2/missing", response.status to response.body)
		assertEquals(ServerErrorKind.Rejected, response.error()?.kind)
		assertEquals("text/plain", fake.serviceCalls.single().headers["Accept"])
	}
}
