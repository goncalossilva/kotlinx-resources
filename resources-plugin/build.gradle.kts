import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0"
    id("java-gradle-plugin")
    id("com.github.gmazzo.buildconfig") version "3.0.3"
    id("pl.allegro.tech.build.axion-release") version "1.13.6"
}

version = scmVersion.version
val artifactId: String by project
val pluginId = "$group.$artifactId"

gradlePlugin {
    val resources by plugins.creating {
        id = pluginId
        implementationClass = "com.goncalossilva.ResourcesPlugin"
    }
}

buildConfig {
    buildConfigField("String", "GROUP_ID", "\"$group\"")
    buildConfigField("String", "ARTIFACT_ID", "\"$artifactId\"")
    buildConfigField("String", "PLUGIN_ID", "\"$pluginId\"")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation(kotlin("stdlib"))
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.contracts.ExperimentalContracts"
    }
}
