package hammer

import hammer.ops.Hammer
import hammer.ops.ImageContent

// Markdown shows a project image as `![image](hammer://image/<id>)`, or with the destination in angle
// brackets, `<hammer://image/<id>>`. The alt text means nothing: Hammer shows the image's own name.

/** What every image reference starts with. */
const val IMAGE_REFERENCE_PREFIX = "hammer://image/"

/** The reference to the image [imageId], `hammer://image/<id>`, for Markdown such as `![image](…)`. */
fun imageReference(imageId: Int): String = "$IMAGE_REFERENCE_PREFIX$imageId"

/**
 * The image id a reference names: `hammer://image/3`, or `<hammer://image/3>`, is 3. Null for anything
 * else, such as another scheme, an id that is empty, not all ASCII digits, or too large for an Int.
 */
fun parseImageReference(reference: String): Int? {
	val source = if (reference.length >= 2 && reference.first() == '<' && reference.last() == '>') {
		reference.substring(1, reference.length - 1)
	} else {
		reference
	}
	if (!source.startsWith(IMAGE_REFERENCE_PREFIX)) return null
	val digits = source.substring(IMAGE_REFERENCE_PREFIX.length)
	if (digits.isEmpty() || !digits.all { it in '0'..'9' }) return null
	return digits.toIntOrNull()
}

/**
 * Every image id [markdown] refers to, each once, in the order they first appear: in images, angle-bracket destinations, reference definitions
 * (`[map]: hammer://image/3`), and code alike, as Hammer counts them. A reference is
 * `hammer://image/` and its digits where no letter, digit, `_`, `+`, `.`, or `-` comes just before
 * it and no letter, digit, or `_` just after, so `hammer://image/12x` names none. An id too large for
 * an Int names none either.
 */
fun findImageIds(markdown: String): Set<Int> {
	val ids = linkedSetOf<Int>()
	var at = markdown.indexOf(IMAGE_REFERENCE_PREFIX)
	while (at >= 0) {
		val start = at + IMAGE_REFERENCE_PREFIX.length
		var end = start
		while (end < markdown.length && markdown[end] in '0'..'9') end++
		val bounded = (at == 0 || !isImageReferenceLead(markdown[at - 1])) &&
			(end == markdown.length || !isAsciiWordChar(markdown[end]))
		if (end > start && bounded) markdown.substring(start, end).toIntOrNull()?.let(ids::add)
		at = markdown.indexOf(IMAGE_REFERENCE_PREFIX, end)
	}
	return ids
}

/**
 * The image a reference names, as [parseImageReference] reads it, with its bytes: `image.get`, which
 * the manifest must ask for. Throws [OperationException] of kind `InvalidInput` for a reference that
 * names no image, and as `image.get` does for one not in [project].
 */
fun getImage(project: String, reference: String): ImageContent {
	val id = parseImageReference(reference) ?: throw OperationException("InvalidInput", "Not an image reference: $reference")
	return Hammer.image.get(project, id)
}

private fun isAsciiWordChar(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '_'

private fun isImageReferenceLead(c: Char): Boolean = isAsciiWordChar(c) || c == '+' || c == '.' || c == '-'
