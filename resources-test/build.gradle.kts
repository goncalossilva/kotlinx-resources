import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

plugins {
    kotlin("multiplatform") version "1.6.0"

    id("io.gitlab.arturbosch.detekt") version "1.19.0"
}
buildscript {
    dependencies {
        classpath("com.goncalossilva:resources")
    }
}
apply(plugin = "com.goncalossilva.resources")

rootProject.plugins.withType<NodeJsRootPlugin> {
    rootProject.configure<NodeJsRootExtension> {
        nodeVersion = "16.13.1"
    }
}

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js(IR) {
        browser {
            testTask {
                useKarma {
                    // List all browsers so that the plugin downloads their runners and sets up
                    // failure capture. `karma-detect-browsers` (below) figures out which ones are
                    // available, and `karma.config.d/select-browser.js` selects one to run tests.
                    useChromeHeadless()
                    useChromiumHeadless()
                    useFirefoxHeadless()
                    useFirefoxDeveloperHeadless()
                    useOpera()
                    useSafari()
                    useIe()
                }
            }
        }
        nodejs()
    }

    ios()
    watchos()
    tvos()

    mingwX64()
    macosX64()
    macosArm64()
    linuxX64()
    linuxArm64()

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("com.goncalossilva:resources-library")
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(npm("karma-detect-browsers", "^2.0"))
            }
        }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}
