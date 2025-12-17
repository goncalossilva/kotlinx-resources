@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
plugins {
    alias(libs.plugins.kotlin.multiplatform)

    id("maven-publish")
    id("signing")
    alias(libs.plugins.nexus.publish)

    alias(libs.plugins.detekt)
}

repositories {
    mavenCentral()
}

kotlin {
    explicitApi()

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }

    js(IR) {
        browser()
        nodejs()
    }

    wasmJs {
        browser()
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
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
            languageSettings.optIn("kotlinx.cinterop.BetaInteropApi")
        }

        val commonMain by getting
        val jsMain by getting
        val wasmJsMain by getting
        val mingwX64Main by getting
        val linuxX64Main by getting
        val linuxArm64Main by getting
        val webMain by getting
        val posixMain by creating {
            dependsOn(commonMain)
            mingwX64Main.dependsOn(this)
            linuxX64Main.dependsOn(this)
            linuxArm64Main.dependsOn(this)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
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

// TODO: Remove when https://youtrack.jetbrains.com/issue/KT-46466 is fixed.
val signingTasks = tasks.withType<Sign>()
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(signingTasks)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    // Configure all publications.
    publications.withType<MavenPublication> {
        val artifactId: String by project
        if (name == "kotlinMultiplatform") {
            setArtifactId(artifactId)
        } else {
            setArtifactId("$artifactId-$name")
        }

        // Stub javadoc jar where missing.
        artifact(javadocJar)

        // Provide information requited by Maven Central.
        pom {
            name.set(rootProject.name)
            description.set(findProperty("publicationDescriptionLibrary") as String)
            url.set(findProperty("publicationUrl") as String)

            licenses {
                license {
                    name.set(findProperty("publicationLicenseName") as String)
                    url.set(findProperty("publicationLicenseUrl") as String)
                }
            }

            scm {
                url.set(findProperty("publicationScmUrl") as String)
                connection.set(findProperty("publicationScmConnection") as String)
                developerConnection.set(findProperty("publicationScmDeveloperConnection") as String)
            }

            developers {
                developer {
                    id.set(findProperty("publicationDeveloperId") as String)
                    name.set(findProperty("publicationDeveloperName") as String)
                }
            }
        }
    }
}

signing {
    // Use `signingKey` and `signingPassword` properties to sign artifacts, if provided.
    // Otherwise, default to `signing.keyId`, `signing.password` and `signing.secretKeyRingFile`.
    val signingKey: String? by project
    val signingPassword: String? by project
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    sign(publishing.publications)
}

// Leverage Gradle Nexus Publish Plugin to create, close and release staging repositories,
// covering the last part of the release process to Maven Central.
nexusPublishing {
    repositories {
        // See https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#configuration
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

detekt {
    config.setFrom(files("../config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}
