import com.goncalossilva.useanybrowser.useAnyBrowser
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec

plugins {
    alias(libs.plugins.kotlin.multiplatform)

    alias(libs.plugins.useanybrowser)

    alias(libs.plugins.detekt)
}

buildscript {
    dependencies {
        classpath("com.goncalossilva:resources")
    }
}
apply(plugin = "com.goncalossilva.resources")

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }

    js(IR) {
        browser {
            testTask {
                useKarma {
                    useAnyBrowser()
                }
            }
        }
        nodejs()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()

    mingwX64()
    macosX64()
    macosArm64()
    linuxX64()
    linuxArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("com.goncalossilva:resources-library")
        }
    }
}

// Exclude duplicate resources. This way, platform-specific take precedence.
tasks.withType<Copy>().configureEach {
    if (name.contains("Test(?:Copy|Process)Resources$".toRegex())) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

plugins.withType<NodeJsPlugin> {
    the<NodeJsEnvSpec>().apply {
        version = libs.versions.nodejs.get()
    }
}

plugins.withType<YarnPlugin> {
    the<YarnRootEnvSpec>().apply {
        version = libs.versions.yarn.get()
        yarnLockMismatchReport = YarnLockMismatchReport.WARNING
        yarnLockAutoReplace = true
    }
}

detekt {
    config.setFrom(files("../config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}
