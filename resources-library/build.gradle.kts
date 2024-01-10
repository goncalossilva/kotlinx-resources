import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.plugins.signing.Sign
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
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

    iosArm64()
    iosX64()
    iosSimulatorArm64()
    watchosArm32()
    watchosArm64()
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
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
            languageSettings.optIn("kotlinx.cinterop.BetaInteropApi")
        }

        val commonMain by getting
        val jsMain by getting
        val wasmJsMain by getting
        val jsSharedMain by creating {
            dependsOn(commonMain)
            jsMain.dependsOn(this)
            wasmJsMain.dependsOn(this)
        }

        val mingwX64Main by getting
        val linuxX64Main by getting
        val linuxArm64Main by getting
        val posixMain by creating {
            dependsOn(commonMain)
            mingwX64Main.dependsOn(this)
            linuxX64Main.dependsOn(this)
            linuxArm64Main.dependsOn(this)
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xexpect-actual-classes"
    }
}

rootProject.configure<NodeJsRootExtension> {
    nodeVersion = libs.versions.nodejs.get()
}

rootProject.plugins.withType<YarnPlugin> {
    rootProject.configure<YarnRootExtension> {
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
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))

            // Read `ossrhUsername` and `ossrhPassword` properties.
            // DO NOT ADD THESE TO SOURCE CONTROL. Store them in your system properties,
            // or pass them in using ORG_GRADLE_PROJECT_* environment variables.
            val ossrhUsername: String? by project
            val ossrhPassword: String? by project
            val ossrhStagingProfileId: String? by project
            username.set(ossrhUsername)
            password.set(ossrhPassword)
            stagingProfileId.set(ossrhStagingProfileId)
        }
    }
}

detekt {
    config.setFrom(files("../config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}
