// The kits' typed operations, generated from api/operations.json:
//   gradle :codegen:updateOperations   refresh the snapshot from hammer-editor, then generate
//   gradle :codegen:generateOperations generate from the snapshot as it is
//   gradle :codegen:checkOperations    fail if the generated code is not what the snapshot makes
// updateOperations takes -Peditor=<hammer-editor checkout> (default ../hammer-editor beside the SDK) and
// -Prevision=<git revision> (default origin/4.0.0).
//
// The kits' word classes, for words(), come from the Unicode Character Database:
//   gradle :codegen:generateWordClasses [-Punicode=<version>]
plugins {
	kotlin("jvm")
	application
}

dependencies {
	implementation("com.squareup:kotlinpoet:2.2.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
	testImplementation(kotlin("test"))
}

tasks.test {
	useJUnitPlatform()
}

application {
	mainClass = "hammer.codegen.MainKt"
}

val repo: File = rootDir.parentFile
val snapshot = repo.resolve("api/operations.json")
val kotlinOps = repo.resolve("kotlin/hammer/src/commonMain/kotlin/hammer/ops")
val rustOps = repo.resolve("rust/hammer/src/ops.rs")

val dumpOperations by tasks.registering(Exec::class) {
	group = "operations"
	description = "Refreshes api/operations.json from hammer-editor's operation registry."
	val editor = providers.gradleProperty("editor").orElse(repo.resolveSibling("hammer-editor").path)
	val revision = providers.gradleProperty("revision").orElse("origin/4.0.0")
	commandLine("sh", repo.resolve("tools/dump-operations.sh"), editor.get(), revision.get())
}

val generateOperations by tasks.registering(JavaExec::class) {
	group = "operations"
	description = "Writes the Kotlin kit's hammer.ops and the Rust kit's ops.rs from api/operations.json."
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass = application.mainClass
	args(snapshot, kotlinOps, rustOps)
	mustRunAfter(dumpOperations)
}

tasks.register("updateOperations") {
	group = "operations"
	description = "Refreshes api/operations.json from hammer-editor, then regenerates both kits' operations."
	dependsOn(dumpOperations, generateOperations)
}

val unicodeVersion = providers.gradleProperty("unicode").orElse("17.0.0")
val ucd = layout.buildDirectory.dir("ucd")

val downloadUnicode by tasks.registering {
	description = "Downloads the Unicode Character Database files the word classes come from."
	inputs.property("version", unicodeVersion)
	outputs.dir(ucd)
	doLast {
		for (name in listOf("UnicodeData.txt", "Scripts.txt")) {
			val url = uri("https://www.unicode.org/Public/${unicodeVersion.get()}/ucd/$name").toURL()
			url.openStream().use { input -> ucd.get().file(name).asFile.outputStream().use { input.copyTo(it) } }
		}
	}
}

tasks.register<JavaExec>("generateWordClasses") {
	group = "text"
	description = "Writes the table each kit's words() splits text with, from Unicode (-Punicode=<version>)."
	dependsOn(downloadUnicode)
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass = "hammer.codegen.WordClassesKt"
	args(
		unicodeVersion.get(),
		ucd.get().asFile,
		repo.resolve("kotlin/hammer/src/commonMain/kotlin/hammer/WordClasses.kt"),
		repo.resolve("rust/hammer/src/word_classes.rs"),
		repo.resolve("c/include/hammer_word_classes.h"),
	)
}

val checkDir = layout.buildDirectory.dir("check-operations")

val generateForCheck by tasks.registering(JavaExec::class) {
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass = application.mainClass
	val out = checkDir.get().asFile
	args(snapshot, out.resolve("kotlin/hammer/ops"), out.resolve("ops.rs"))
	doFirst { out.resolve("kotlin").mkdirs() }
}

tasks.register("checkOperations") {
	group = "verification"
	description = "Fails if the committed operations differ from what api/operations.json generates."
	dependsOn(generateForCheck)
	doLast {
		val out = checkDir.get().asFile
		fun text(file: File): String? = if (file.isFile) file.readText() else null
		val generated = out.resolve("kotlin/hammer/ops")
		val names = sortedSetOf<String>().apply {
			addAll(generated.list().orEmpty())
			addAll(kotlinOps.list().orEmpty())
		}
		val stale = mutableListOf<String>()
		names.filter { text(generated.resolve(it)) != text(kotlinOps.resolve(it)) }
			.forEach { stale += "kotlin/hammer/src/commonMain/kotlin/hammer/ops/$it" }
		if (text(out.resolve("ops.rs")) != text(rustOps)) stale += "rust/hammer/src/ops.rs"
		if (stale.isNotEmpty()) throw GradleException("Out of date; run gradle :codegen:generateOperations: ${stale.joinToString()}")
	}
}
