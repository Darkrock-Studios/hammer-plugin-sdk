package hammer.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

/**
 * Builds a Hammer plugin in Kotlin/Wasm. Applying `com.darkrockstudios.hammer.plugin` sets up Kotlin Multiplatform with a
 * `wasmWasi` target for the plugin and a `jvm` target for testing its common code, adds the kit, and
 * adds `hammerPlugin`, which builds `build/<name>.hammerplugin` from `manifest.toml`, `settings.toml`,
 * `locales/`, and `resources/` if there are any, and the optimized module. The plugin's own code is common code plus its
 * `@WasmExport` functions in `wasmWasiMain`.
 */
class HammerPluginPlugin : Plugin<Project> {
	override fun apply(project: Project) {
		project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
		project.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
		val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

		kotlin.jvm()
		@OptIn(ExperimentalWasmDsl::class)
		kotlin.wasmWasi {
			// The file is named for the plugin, not for a multi-project build it sits in. The Wasm target is
			// a JS target underneath, which is where the setting lives.
			(this as KotlinJsTargetDsl).outputModuleName.set(project.name)
			// Hammer runs the module; Node is only named because Kotlin requires an environment.
			nodejs()
			binaries.executable()
		}

		// An executable Kotlin/Wasm module needs a main, which Hammer never calls.
		val generatedMain = project.layout.buildDirectory.dir("generated/hammer/wasmWasiMain")
		val generateMain = project.tasks.register("generateHammerMain") { task ->
			task.outputs.dir(generatedMain)
			task.doLast {
				generatedMain.get().file("HammerMain.kt").asFile.apply { parentFile.mkdirs() }
					.writeText("// Generated: Kotlin/Wasm executables need a main, which Hammer never calls.\nfun main() {}\n")
			}
		}
		kotlin.sourceSets.getByName("wasmWasiMain").kotlin.srcDir(generateMain)

		// A build holding the kit itself names its project in `hammer.kit.project`; others get it from Maven.
		val kit: Any = (project.findProperty("hammer.kit.project") as String?)?.let(project::project)
			?: "com.darkrockstudios.hammer:hammer-plugin-kit:$KIT_VERSION"
		project.dependencies.add("commonMainImplementation", kit)
		project.dependencies.add("jvmTestImplementation", "org.jetbrains.kotlin:kotlin-test")

		val optimized = project.layout.buildDirectory
			.file("compileSync/wasmWasi/main/productionExecutable/optimized/${project.name}.wasm")
		project.tasks.register("hammerPlugin", Zip::class.java) { zip ->
			zip.group = "build"
			zip.description = "Builds the .hammerplugin package from the manifest, settings, translations, resources, and optimized module."
			zip.dependsOn("compileProductionExecutableKotlinWasmWasiOptimize")
			val manifest = project.file("manifest.toml")
			zip.doFirst { checkManifest(manifest) }
			zip.from(manifest)
			zip.from(project.file("settings.toml").takeIf { it.exists() } ?: project.files())
			zip.from(project.file("locales")) { it.include("*.toml"); it.into("locales") }
			zip.from(project.file("resources")) { it.into("resources") }
			zip.from(optimized) { it.rename { "plugin.wasm" } }
			zip.archiveFileName.set("${project.name}.hammerplugin")
			zip.destinationDirectory.set(project.layout.buildDirectory)
			zip.isPreserveFileTimestamps = false
			zip.isReproducibleFileOrder = true
		}
	}

	private fun checkManifest(manifest: java.io.File) {
		if (!manifest.exists()) throw GradleException("A Hammer plugin needs a manifest.toml next to its build file")
		val api = Regex("""^api\s*=\s*(\d+)""", RegexOption.MULTILINE).find(manifest.readText())?.groupValues?.get(1)
		if (api != API_VERSION.toString()) {
			throw GradleException("manifest.toml says api = ${api ?: "nothing"}, but this kit targets api = $API_VERSION")
		}
	}
}
