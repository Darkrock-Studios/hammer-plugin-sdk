package hammer

/**
 * What the kit reaches Hammer through. Inside Hammer it is the Extism host; in a JVM test, set [host]
 * to a [FakeHammerHost] (or your own) so plugin code runs without Wasm.
 */
interface HammerHost {
	/** Hammer's reply JSON to [request], `{"operation": ..., "input": ...}`. */
	fun dispatch(request: String): String

	fun cacheGet(key: ByteArray): ByteArray?

	/** Stores [value] under [key]; an empty value removes it. */
	fun cacheSet(key: ByteArray, value: ByteArray)

	fun log(level: LogLevel, message: String)

	/** Reports an action's progress, `{"fraction": ..., "message": ...}`. */
	fun progress(report: String) {}
}

enum class LogLevel { Info, Warn, Error }

/** The host the kit uses. Hammer's exports set it; tests set it themselves. */
var host: HammerHost = NoHost

private object NoHost : HammerHost {
	private fun missing(): Nothing = error("No Hammer host: in a test, set hammer.host to a FakeHammerHost")

	override fun dispatch(request: String): String = missing()

	override fun cacheGet(key: ByteArray): ByteArray = missing()

	override fun cacheSet(key: ByteArray, value: ByteArray): Unit = missing()

	override fun log(level: LogLevel, message: String): Unit = missing()
}
