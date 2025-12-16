pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "resources"

include("resources-test")
include("resources-test-android")
includeBuild("resources-plugin") {
    dependencySubstitution {
        substitute(module("com.goncalossilva:resources")).using(project(":"))
    }
}
includeBuild("resources-library")
