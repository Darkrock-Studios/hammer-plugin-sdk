package hammer

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class TextTest {
	@Test
	fun `words split the shared cases as every kit does`() {
		val escape = Regex("""\\u\{([0-9A-Fa-f]+)}""")
		fun unescape(s: String) = escape.replace(s) { String(Character.toChars(it.groupValues[1].toInt(16))) }
		File("../../tests/words.txt").readLines().filter { it.isNotEmpty() && !it.startsWith("#") }.forEach { line ->
			val (text, expected) = line.split('\t').map(::unescape)
			val words = text.words()
			assertEquals(expected.split(' ').filter { it.isNotEmpty() }, words.map { it.text }, line)
			words.forEach { assertEquals(it.text, text.substring(it.start, it.end), line) }
		}
	}

	@Test
	fun `offsets index the text`() {
		assertEquals(
			listOf(Word("Ça", 0, 2), Word("va", 3, 5), Word("très", 7, 11), Word("𐌰𐌱", 12, 16)),
			"Ça va, très 𐌰𐌱.".words(),
		)
	}

	@Test
	fun `the ASCII shortcut agrees with the table`() {
		for (code in 0 until 0x80) assertEquals(wordClass(code), code.toChar().toString().wordClassAt(0), "U+%04X".format(code))
	}
}
