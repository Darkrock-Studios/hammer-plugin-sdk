package hammer

import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64

/**
 * What the `export` export returns for a format whose manifest entry sets `output = "archive"`: files
 * Hammer zips, in the order they were added, into the one export file, whose `extension` and `mime` the
 * manifest still gives (`zip` and `application/zip`). Return `toJson().encodeToByteArray()` from
 * `exporter`.
 *
 * A file is text, bytes, or one of the request's [ExportRequest.images] named by its id, whose bytes
 * Hammer writes itself, so forwarding the story's images sends none back.
 *
 * A path is relative, with `/` between its segments: not empty, not starting with `/`, no `\`, no empty
 * segment (so no `//` or trailing `/`), no `.` or `..` segment, and not a path already added. Adding a
 * file with any other path throws [IllegalArgumentException] naming it, as Hammer would refuse the reply.
 */
class ExportArchive {
	private val files = mutableListOf<File>()
	private val paths = hashSetOf<String>()

	/** Adds [text] as the file at [path], written as UTF-8. */
	fun text(path: String, text: String): ExportArchive = add(File(path, text = text))

	/** Adds [bytes] as the file at [path]. */
	fun bytes(path: String, bytes: ByteArray): ExportArchive = add(File(path, content = Base64.encode(bytes)))

	/**
	 * Adds the file at [path] whose bytes [encoded] holds as standard padded base64, passed on without
	 * decoding it, for base64 a plugin kept itself; Hammer fails the export when it is not base64.
	 */
	fun base64(path: String, encoded: String): ExportArchive = add(File(path, content = encoded))

	/**
	 * Adds image [id] of the request's [ExportRequest.images] as the file at [path]. Hammer writes its
	 * bytes, so the reply carries none; an id it did not send fails the export.
	 */
	fun image(path: String, id: Int): ExportArchive = add(File(path, image = id))

	/** Adds [image] as the file at [path], naming it by its id, so Hammer writes its bytes. */
	fun image(path: String, image: ExportRequest.Image): ExportArchive = image(path, image.id)

	/** The `export` export's reply. Throws [IllegalArgumentException] when no file was added. */
	fun toJson(): String {
		require(files.isNotEmpty()) { "An archive needs at least one file" }
		return json.encodeToString(Reply.serializer(), Reply(files))
	}

	private fun add(file: File): ExportArchive {
		val path = file.path
		require(path.isNotEmpty()) { "Archive path \"$path\" is empty" }
		require(!path.startsWith('/')) { "Archive path \"$path\" starts with /" }
		require('\\' !in path) { "Archive path \"$path\" has a backslash" }
		path.split('/').forEach { segment ->
			require(segment.isNotEmpty()) { "Archive path \"$path\" has an empty segment" }
			require(segment != "." && segment != "..") { "Archive path \"$path\" has a $segment segment" }
		}
		require(paths.add(path)) { "Archive path \"$path\" is already in the archive" }
		files += file
		return this
	}

	@Serializable
	private class File(val path: String, val text: String? = null, val content: String? = null, val image: Int? = null)

	@Serializable
	private class Reply(val files: List<File>)
}
