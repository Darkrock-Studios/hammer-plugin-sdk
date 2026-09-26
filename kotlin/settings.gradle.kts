rootProject.name = "hammer-plugin-sdk"

pluginManagement {
	includeBuild("gradle-plugin")
	repositories {
		gradlePluginPortal()
		mavenCentral()
	}
}

dependencyResolutionManagement {
	repositories {
		mavenCentral()
	}
}

include("plugin-kit", "hello")
project(":plugin-kit").projectDir = file("hammer")
