import com.goncalossilva.useanybrowser.useAnyBrowser
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

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

    iosArm64()
    iosX64()
    iosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    mingwX64()
    macosX64()
    macosArm64()
    linuxX64()
    linuxArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("com.goncalossilva:resources-library")
            }
        }
    }
}

rootProject.configure<NodeJsRootExtension> {
    version = libs.versions.nodejs.get()
}

rootProject.plugins.withType<YarnPlugin> {
    rootProject.configure<YarnRootExtension> {
        version = libs.versions.yarn.get()
        yarnLockMismatchReport = YarnLockMismatchReport.WARNING
        yarnLockAutoReplace = true
    }
}


detekt {
    config.setFrom(files("../config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}
