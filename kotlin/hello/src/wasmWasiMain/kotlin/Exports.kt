import hammer.action
import hammer.command
import hammer.diagnose
import hammer.exporter
import kotlin.wasm.WasmExport

@WasmExport
fun export(): Int = exporter { helloExport(it).encodeToByteArray() }

@WasmExport
fun action(): Int = action { request ->
	when (request.action) {
		"greet" -> greet(request)
		"outline" -> outline(request)
		"count" -> countWords(request)
		"about" -> about(request)
		"images" -> sceneImages(request)
		"tip" -> tip()
		"wave" -> wave(request)
		else -> null
	}
}

@WasmExport
fun diagnose(): Int = diagnose(::findVery)

@WasmExport
fun command(): Int = command(::helloCommand)
