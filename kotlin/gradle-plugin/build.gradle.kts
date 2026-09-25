import java.util.Properties

plugins {
	kotlin("jvm") version "2.4.10"
	`java-gradle-plugin`
}

dependencies {
	// The Kotlin version plugins build with: the kit's, and one Kotlin/Wasm output Hammer is tested with.
	implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
	implementation("org.jetbrains.kotlin:kotlin-serialization:2.4.10")
}

val versions = Properties().apply { rootDir.resolve("../versions.properties").reader().use(::load) }

val generateVersions by tasks.registering {
	val apiVersion = versions.getProperty("apiVersion")
	val kitVersion = versions.getProperty("kitVersion")
	val output = layout.buildDirectory.dir("generated/hammer")
	inputs.property("apiVersion", apiVersion)
	inputs.property("kitVersion", kitVersion)
	outputs.dir(output)
	doLast {
		output.get().file("Versions.kt").asFile.apply { parentFile.mkdirs() }.writeText(
			"package hammer.gradle\n\ninternal const val API_VERSION = $apiVersion\ninternal const val KIT_VERSION = \"$kitVersion\"\n"
		)
	}
}

sourceSets.main {
	kotlin.srcDir(generateVersions)
}

gradlePlugin {
	plugins {
		create("hammerPlugin") {
			id = "hammer.plugin"
			implementationClass = "hammer.gradle.HammerPluginPlugin"
		}
	}
}
