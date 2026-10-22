package hammer.codegen

import java.io.File

/** Usage: codegen <operations.json> <Kotlin hammer/ops directory> <Rust ops.rs> */
fun main(args: Array<String>) {
	require(args.size == 3) { "Usage: codegen <operations.json> <kotlin ops dir> <rust ops.rs>" }
	val api = readApi(File(args[0]).readText())
	KotlinGenerator(api).write(File(args[1]))
	RustGenerator(api).write(File(args[2]))
	println("Generated ${api.operations.size} operations and ${api.reachable().size} types")
}
