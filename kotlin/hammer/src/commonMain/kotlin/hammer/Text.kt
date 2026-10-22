package hammer

/** A word in a text: [text], from [start] up to [end], indexes into the text's String as a [Diagnostic] takes them. */
data class Word(val text: String, val start: Int, val end: Int)

/**
 * The words in this text, in order: runs of letters, with an apostrophe (`'`, `’`, or `ʼ`) between two
 * letters kept inside, so "don't" and "l'arbre" are one word each. Digits, hyphens, and everything else
 * end a word. `ʼ` is itself a letter, as in Ukrainian. The C kit's `hammer_words` and the Rust kit's
 * `words` split text the same way.
 *
 * It scans by hand rather than with a `Regex`, which is slow in Hammer's interpreter.
 */
fun String.words(): List<Word> {
	val words = mutableListOf<Word>()
	var i = 0
	while (i < length) {
		if (!this[i].isLetter()) {
			i++
			continue
		}
		val start = i
		while (i < length && (this[i].isLetter() || (this[i] in APOSTROPHES && getOrNull(i + 1)?.isLetter() == true))) i++
		words += Word(substring(start, i), start, i)
	}
	return words
}

private const val APOSTROPHES = "'’ʼ"
