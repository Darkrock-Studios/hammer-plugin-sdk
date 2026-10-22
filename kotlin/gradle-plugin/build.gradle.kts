import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import java.util.Properties

plugins {
	kotlin("jvm") version "2.4.10"
	`java-gradle-plugin`
	id("com.vanniktech.maven.publish") version "0.37.0"
}

dependencies {
	// The Kotlin version plugins build with: the kit's, and one Kotlin/Wasm output Hammer is tested with.
	implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
	implementation("org.jetbrains.kotlin:kotlin-serialization:2.4.10")
}

val versions = Properties().apply { rootDir.resolve("../versions.properties").reader().use(::load) }

group = "com.darkrockstudios.hammer"
version = versions.getProperty("kitVersion")

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
	website = "https://github.com/Darkrock-Studios/hammer-plugin-sdk"
	vcsUrl = "https://github.com/Darkrock-Studios/hammer-plugin-sdk"
	plugins {
		create("hammerPlugin") {
			id = "com.darkrockstudios.hammer.plugin"
			displayName = "Hammer plugin"
			description = "Builds a Hammer editor plugin in Kotlin/Wasm into a .hammerplugin package."
			implementationClass = "hammer.gradle.HammerPluginPlugin"
		}
	}
}

mavenPublishing {
	configure(GradlePlugin(javadocJar = JavadocJar.Empty(), sourcesJar = true))
	coordinates(artifactId = "hammer-gradle-plugin")
	publishToMavenCentral(automaticRelease = true)
	// Signed when CI provides the key; a local publishToMavenLocal works without one.
	if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
	pom {
		name = "Hammer plugin Gradle plugin"
		description = "Builds a Hammer editor plugin in Kotlin/Wasm into a .hammerplugin package."
		hammerPom()
	}
}

fun org.gradle.api.publish.maven.MavenPom.hammerPom() {
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
