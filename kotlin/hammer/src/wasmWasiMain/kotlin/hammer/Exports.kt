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

/** The `action` export's body: [handle] returns what the user sees, or null for nothing. */
fun action(handle: (ActionRequest) -> String?): Int = run { handle(json.decodeFromString(inputText())).orEmpty() }

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

fun inputText(): String = input().decodeToString()

private object ExtismHost : HammerHost {
	override fun dispatch(request: String): String {
		val requestHandle = toKernel(request.encodeToByteArray())
		val replyHandle = hammerDispatch(requestHandle)
		free(requestHandle)
		return fromKernel(replyHandle).decodeToString().also { free(replyHandle) }
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
