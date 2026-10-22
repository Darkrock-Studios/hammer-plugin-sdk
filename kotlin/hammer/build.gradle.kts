import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import java.util.Properties
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
	kotlin("multiplatform")
	kotlin("plugin.serialization")
	id("com.vanniktech.maven.publish") version "0.37.0"
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

mavenPublishing {
	configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty(), sourcesJar = true))
	coordinates(artifactId = "hammer-plugin-kit")
	publishToMavenCentral(automaticRelease = true)
	// Signed when CI provides the key; a local publishToMavenLocal works without one.
	if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
	pom {
		name = "Hammer plugin kit"
		description = "Write plugins for the Hammer editor in Kotlin/Wasm: typed requests, replies, and operations."
		val repo = "github.com/Darkrock-Studios/hammer-plugin-sdk"
		url = "https://$repo"
		licenses {
			license {
				name = "MIT"
				url = "https://opensource.org/licenses/MIT"
			}
		}
		issueManagement {
			system = "GitHub"
			url = "https://$repo/issues"
		}
		scm {
			connection = "scm:git:git://$repo.git"
			developerConnection = "scm:git:ssh://$repo.git"
			url = "https://$repo"
		}
		developers {
			developer {
				name = "Adam Brown"
				id = "Wavesonics"
				email = "adamwbrown@gmail.com"
			}
		}
	}
}
