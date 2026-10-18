import java.util.Properties
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
	kotlin("multiplatform")
	kotlin("plugin.serialization")
}

val versions = Properties().apply { rootDir.resolve("versions.properties").reader().use(::load) }

group = "com.darkrockstudios.hammer"
version = versions.getProperty("kitVersion")

val generateApiVersion by tasks.registering {
	val apiVersion = versions.getProperty("apiVersion")
	val output = layout.buildDirectory.dir("generated/hammer/commonMain")
	inputs.property("apiVersion", apiVersion)
	outputs.dir(output)
	doLast {
		output.get().file("ApiVersion.kt").asFile.apply { parentFile.mkdirs() }.writeText(
			"package hammer\n\n/** The host API version this kit targets; the manifest's `api` must match. */\nconst val API_VERSION = $apiVersion\n"
		)
	}
}

kotlin {
	jvm()
	@OptIn(ExperimentalWasmDsl::class)
	wasmWasi {
		nodejs()
	}

	sourceSets {
		commonMain {
			kotlin.srcDir(generateApiVersion)
		}
		commonMain.dependencies {
			api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
		}
		jvmMain.dependencies {
			// ActionDriver reads a plugin's manifest with the TOML parser Hammer itself uses.
			implementation("net.peanuuutz.tomlkt:tomlkt:0.5.0")
		}
		jvmTest.dependencies {
			implementation(kotlin("test"))
		}
	}
}
