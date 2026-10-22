package hammer

import kotlin.test.Test
import kotlin.test.assertEquals

class TextTest {
	private fun texts(text: String) = text.words().map { it.text }

	@Test
	fun `words are runs of letters, with apostrophes between letters`() {
		assertEquals(listOf("Don't", "say", "l'arbre", "rock’n’roll", "ʼtwas"), texts("Don't say l'arbre — rock’n’roll! ʼtwas"))
		assertEquals(listOf("very", "every", "Very"), texts("very every, “Very.”"))
	}

	@Test
	fun `digits, hyphens, and trailing apostrophes end a word`() {
		assertEquals(listOf("well", "known", "abc", "def", "dogs"), texts("well-known abc123def dogs' "))
		assertEquals(emptyList(), texts("  42 — … "))
	}

	@Test
	fun `offsets index the text`() {
		val text = "Ça va, très bien."
		assertEquals(listOf(Word("Ça", 0, 2), Word("va", 3, 5), Word("très", 7, 11), Word("bien", 12, 16)), text.words())
		text.words().forEach { assertEquals(it.text, text.substring(it.start, it.end)) }
	}
}
