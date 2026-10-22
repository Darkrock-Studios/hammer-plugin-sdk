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

include("hammer-plugin-kit", "hello", "codegen")
project(":hammer-plugin-kit").projectDir = file("hammer")
