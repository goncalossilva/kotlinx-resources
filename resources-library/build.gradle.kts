import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport

plugins {
    alias(libs.plugins.kotlin.multiplatform)

    id("maven-publish")
    id("signing")
    alias(libs.plugins.nexus.publish)

    alias(libs.plugins.detekt)
}

rootProject.plugins.withType<NodeJsRootPlugin> {
    rootProject.configure<NodeJsRootExtension> {
        nodeVersion = libs.versions.nodejs.get()
    }
}

rootProject.plugins.withType(YarnPlugin::class.java) {
    rootProject.configure<YarnRootExtension> {
        version = libs.versions.yarn.get()
        yarnLockMismatchReport = YarnLockMismatchReport.WARNING
        yarnLockAutoReplace = true
    }
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

    ios()
    iosSimulatorArm64()
    watchos()
    watchosSimulatorArm64()
    tvos()
    tvosSimulatorArm64()

    mingwX64()
    macosX64()
    macosArm64()
    linuxX64()
    linuxArm64()

    sourceSets {
        val commonMain by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val watchosX64Main by getting
        val watchosArm32Main by getting
        val watchosArm64Main by getting
        val watchosSimulatorArm64Main by getting
        val tvosX64Main by getting
        val tvosArm64Main by getting
        val tvosSimulatorArm64Main by getting
        val macosX64Main by getting
        val macosArm64Main by getting
        val darwinMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            watchosX64Main.dependsOn(this)
            watchosArm32Main.dependsOn(this)
            watchosArm64Main.dependsOn(this)
            watchosSimulatorArm64Main.dependsOn(this)
            tvosX64Main.dependsOn(this)
            tvosArm64Main.dependsOn(this)
            tvosSimulatorArm64Main.dependsOn(this)
            macosX64Main.dependsOn(this)
            macosArm64Main.dependsOn(this)
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

        // Publish docs with each artifact.
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
    config = files("../config/detekt/detekt.yml")
    buildUponDefaultConfig = true
}
