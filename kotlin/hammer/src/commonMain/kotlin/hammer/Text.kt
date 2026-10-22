package hammer

/** A word in a text: [text], from [start] up to [end], indexes into the text's String as a [Diagnostic] takes them. */
data class Word(val text: String, val start: Int, val end: Int)

/**
 * The words in this text, in order. A word is a run of letters, with any combining marks on them, so
 * accents, Indic vowel signs, and Arabic vowel marks stay in their word. An apostrophe (`'` or `’`), the
 * Catalan middle dot, Hebrew's geresh and gershayim, ZWNJ, ZWJ, or a soft hyphen between two letters
 * stays inside too, so "don't", "l'arbre", and "col·lecció" are one word each. Digits, hyphens, and
 * everything else end a word.
 *
 * Chinese and Japanese put no spaces between words, so each of their characters (Han, kana, and
 * Bopomofo) is a word of its own. Thai, Lao, Khmer, and Burmese don't either, but there each run of
 * letters is one word.
 *
 * Letters come from a table of Unicode 17, the same one the Rust kit's `words` and the C kit's
 * `hammer_words` use, so all three split text the same way.
 */
fun String.words(): List<Word> {
	val words = mutableListOf<Word>()
	var i = 0
	while (i < length) {
		val start = i
		val cls = wordClassAt(i)
		i += charCountAt(i)
		when (cls) {
			IDEOGRAPH -> {
				while (i < length && wordClassAt(i) == MARK) i += charCountAt(i)
			}
			LETTER -> while (i < length) {
				val next = wordClassAt(i)
				val after = i + charCountAt(i)
				if (next == LETTER || next == MARK || (next == JOINER && after < length && wordClassAt(after) == LETTER)) {
					i = after
				} else {
					break
				}
			}
			else -> continue
		}
		words += Word(substring(start, i), start, i)
	}
	return words
}

internal const val OTHER = 0
internal const val LETTER = 1
internal const val MARK = 2
internal const val IDEOGRAPH = 3
internal const val JOINER = 4

private fun String.charCountAt(i: Int): Int =
	if (this[i].isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) 2 else 1

internal fun String.wordClassAt(i: Int): Int {
	val c = this[i]
	if (c.code < 0x80) {
		return when {
			c in 'a'..'z' || c in 'A'..'Z' -> LETTER
			c == '\'' -> JOINER
			else -> OTHER
		}
	}
	val code = if (charCountAt(i) == 2) 0x10000 + ((c.code - 0xD800) shl 10) + (this[i + 1].code - 0xDC00) else c.code
	return wordClass(code)
}

internal fun wordClass(code: Int): Int {
	var low = 0
	var high = WORD_CLASSES.size / 3 - 1
	while (low <= high) {
		val mid = (low + high) ushr 1
		when {
			code < WORD_CLASSES[mid * 3] -> high = mid - 1
			code > WORD_CLASSES[mid * 3 + 1] -> low = mid + 1
			else -> return WORD_CLASSES[mid * 3 + 2]
		}
	}
	return OTHER
}
