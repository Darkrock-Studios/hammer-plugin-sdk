@file:OptIn(ExperimentalWasmInterop::class)

package hammer

import kotlin.wasm.ExperimentalWasmInterop
import kotlin.wasm.WasmImport
import kotlinx.serialization.decodeFromString

// Bodies for a plugin's exports, which must be top-level functions in the plugin itself:
//
//     @WasmExport
//     fun action(): Int = action { request -> report(request.project) }

/** The `export` export's body: [render] returns the file's bytes. */
fun exporter(render: (ExportRequest) -> ByteArray): Int = runBytes { render(json.decodeFromString(inputText())) }

/**
 * The `action` export's body: [handle] returns what the user sees, or null for nothing. For an
 * `interactive` action, that is an [ActionReply]'s [toJson].
 */
fun action(handle: (ActionRequest) -> String?): Int = run { handle(json.decodeFromString(inputText())).orEmpty() }

/** The `diagnose` export's body: [check] returns the issues in the request's paragraphs. */
fun diagnose(check: (DiagnoseRequest) -> List<Diagnostic>): Int = run {
	val request = json.decodeFromString<DiagnoseRequest>(inputText())
	diagnoseReply(request.paragraphs, check(request))
}

/** The `command` export's body: [answer] returns the line to write to standard output, or null for none. */
fun command(answer: (CommandRequest) -> String?): Int = run { answer(json.decodeFromString(inputText())).orEmpty() }

/**
 * Runs an export's body: its output on success, and on failure the exception's message, which is what
 * the user sees.
 */
fun run(body: () -> String): Int = runBytes { body().encodeToByteArray() }

/** [run], for output that is not text. */
fun runBytes(body: () -> ByteArray): Int {
	host = ExtismHost
	val output = try {
		body()
	} catch (e: Throwable) {
		// The call fails either way; this way the user sees why, even for an Error such as TODO()'s.
		errorSet(toKernel((e.message ?: e.toString()).encodeToByteArray()))
		return 1
	}
	outputSet(toKernel(output), output.size.toLong())
	return 0
}

/** The call's raw input. [exporter], [action], and [command] read it as their request. */
fun input(): ByteArray = readKernel(inputLength().toInt(), ::inputLoadU64, ::inputLoadU8)

fun inputText(): String = readKernelText(inputLength().toInt(), ::inputLoadU64, ::inputLoadU8)

private object ExtismHost : HammerHost {
	override fun dispatch(request: String): String {
		val requestHandle = toKernel(request.encodeToByteArray())
		val replyHandle = hammerDispatch(requestHandle)
		free(requestHandle)
		return fromKernelText(replyHandle).also { free(replyHandle) }
	}

	override fun cacheGet(key: ByteArray): ByteArray? {
		val keyHandle = toKernel(key)
		val value = hammerCacheGet(keyHandle)
		free(keyHandle)
		if (value == 0L) return null
		return fromKernel(value).also { free(value) }
	}

	override fun cacheSet(key: ByteArray, value: ByteArray) {
		val keyHandle = toKernel(key)
		val valueHandle = if (value.isEmpty()) 0L else toKernel(value)
		hammerCacheSet(keyHandle, valueHandle)
		free(keyHandle)
		if (valueHandle != 0L) free(valueHandle)
	}

	override fun progress(report: String) {
		val handle = toKernel(report.encodeToByteArray())
		hammerProgress(handle)
		free(handle)
	}

	override fun log(level: LogLevel, message: String) {
		val handle = toKernel(message.encodeToByteArray())
		when (level) {
			LogLevel.Info -> logInfo(handle)
			LogLevel.Warn -> logWarn(handle)
			LogLevel.Error -> logError(handle)
		}
		free(handle)
	}
}

// Extism's kernel keeps the call's input, output, and any memory passed to or from Hammer. Offsets are
// into the kernel's memory, not the module's.

@WasmImport("extism:host/env", "input_length")
private external fun inputLength(): Long

@WasmImport("extism:host/env", "input_load_u8")
private external fun inputLoadU8(index: Long): Int

@WasmImport("extism:host/env", "input_load_u64")
private external fun inputLoadU64(index: Long): Long

@WasmImport("extism:host/env", "alloc")
private external fun alloc(length: Long): Long

@WasmImport("extism:host/env", "free")
private external fun free(offset: Long)

@WasmImport("extism:host/env", "length")
private external fun length(offset: Long): Long

@WasmImport("extism:host/env", "load_u8")
private external fun loadU8(offset: Long): Int

@WasmImport("extism:host/env", "load_u64")
private external fun loadU64(offset: Long): Long

@WasmImport("extism:host/env", "store_u8")
private external fun storeU8(offset: Long, value: Int)

@WasmImport("extism:host/env", "store_u64")
private external fun storeU64(offset: Long, value: Long)

@WasmImport("extism:host/env", "output_set")
private external fun outputSet(offset: Long, length: Long)

@WasmImport("extism:host/env", "error_set")
private external fun errorSet(offset: Long)

@WasmImport("extism:host/env", "log_info")
private external fun logInfo(offset: Long)

@WasmImport("extism:host/env", "log_warn")
private external fun logWarn(offset: Long)

@WasmImport("extism:host/env", "log_error")
private external fun logError(offset: Long)

@WasmImport("extism:host/user", "hammer_dispatch")
private external fun hammerDispatch(request: Long): Long

@WasmImport("extism:host/user", "hammer_progress")
private external fun hammerProgress(report: Long)

@WasmImport("extism:host/user", "hammer_cache_get")
private external fun hammerCacheGet(key: Long): Long

@WasmImport("extism:host/user", "hammer_cache_set")
private external fun hammerCacheSet(key: Long, value: Long)

private fun toKernel(bytes: ByteArray): Long {
	val offset = alloc(bytes.size.toLong())
	var at = 0
	while (at + 8 <= bytes.size) {
		var word = 0L
		for (i in 7 downTo 0) word = (word shl 8) or (bytes[at + i].toLong() and 0xFF)
		storeU64(offset + at, word)
		at += 8
	}
	while (at < bytes.size) {
		storeU8(offset + at, bytes[at].toInt())
		at++
	}
	return offset
}

private inline fun readKernel(size: Int, loadU64: (Long) -> Long, loadU8: (Long) -> Int): ByteArray {
	val bytes = ByteArray(size)
	var at = 0
	while (at + 8 <= size) {
		var word = loadU64(at.toLong())
		for (i in 0 until 8) {
			bytes[at + i] = word.toByte()
			word = word ushr 8
		}
		at += 8
	}
	while (at < size) {
		bytes[at] = loadU8(at.toLong()).toByte()
		at++
	}
	return bytes
}

private fun fromKernel(offset: Long): ByteArray =
	readKernel(length(offset).toInt(), { loadU64(offset + it) }, { loadU8(offset + it) })

private fun fromKernelText(offset: Long): String =
	readKernelText(length(offset).toInt(), { loadU64(offset + it) }, { loadU8(offset + it) })

// Kotlin's own UTF-8 decoding costs more than the copy itself in the interpreter, so text is read
// from the kernel into a String directly, eight ASCII bytes at a time. Text that is not well-formed
// goes through Kotlin's decoder, which replaces what is malformed.

private const val HIGH_BITS = -0x7F7F7F7F7F7F7F80L // 0x8080808080808080

private inline fun readKernelText(size: Int, loadU64: (Long) -> Long, loadU8: (Long) -> Int): String {
	val chars = CharArray(size)
	var count = 0
	var codePoint = 0
	var pending = 0
	var least = 0
	var wellFormed = true
	var at = 0
	read@ while (at < size) {
		val whole = at + 8 <= size
		var word = if (whole) loadU64(at.toLong()) else loadU8(at.toLong()).toLong()
		if (whole && pending == 0 && word and HIGH_BITS == 0L) {
			for (i in 0 until 8) {
				chars[count++] = (word and 0x7F).toInt().toChar()
				word = word ushr 8
			}
			at += 8
			continue
		}
		for (i in 0 until if (whole) 8 else 1) {
			val byte = (word and 0xFF).toInt()
			word = word ushr 8
			if (pending == 0) {
				when {
					byte < 0x80 -> chars[count++] = byte.toChar()
					byte in 0xC2..0xDF -> { codePoint = byte and 0x1F; pending = 1; least = 0x80 }
					byte in 0xE0..0xEF -> { codePoint = byte and 0x0F; pending = 2; least = 0x800 }
					byte in 0xF0..0xF4 -> { codePoint = byte and 0x07; pending = 3; least = 0x10000 }
					else -> { wellFormed = false; break@read }
				}
				continue
			}
			if (byte and 0xC0 != 0x80) { wellFormed = false; break@read }
			codePoint = (codePoint shl 6) or (byte and 0x3F)
			if (--pending > 0) continue
			if (codePoint < least || codePoint in 0xD800..0xDFFF || codePoint > 0x10FFFF) { wellFormed = false; break@read }
			if (codePoint < 0x10000) {
				chars[count++] = codePoint.toChar()
			} else {
				chars[count++] = (0xD7C0 + (codePoint shr 10)).toChar()
				chars[count++] = (0xDC00 or (codePoint and 0x3FF)).toChar()
			}
		}
		at += if (whole) 8 else 1
	}
	if (!wellFormed || pending > 0) return readKernel(size, loadU64, loadU8).decodeToString()
	return chars.concatToString(0, count)
}
