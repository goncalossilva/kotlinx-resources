import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

plugins {
    kotlin("multiplatform") version "1.6.0"

    id("com.goncalossilva.resources")

    id("maven-publish")
    id("signing")
}

rootProject.plugins.withType<NodeJsRootPlugin> {
    rootProject.configure<NodeJsRootExtension> {
        nodeVersion = "16.13.1"
    }
}

repositories {
    mavenCentral()
    jcenter() // For kotlinx-nodejs, can be removed when updating from 0.0.7.
}

kotlin {
    explicitApi()

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js("jsBrowser", IR) {
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
    }
    js("jsNode", IR) {
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
            }
        }

        val jsBrowserTest by getting {
            dependencies {
                implementation(npm("karma-detect-browsers", "^2.0"))
            }
        }

        val jsNodeMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-nodejs:0.0.7")
            }
        }

        val iosX64Main by getting
        val iosX64Test by getting
        val iosArm64Main by getting
        val iosArm64Test by getting
        val watchosX64Main by getting
        val watchosX64Test by getting
        val watchosArm32Main by getting
        val watchosArm32Test by getting
        val watchosArm64Main by getting
        val watchosArm64Test by getting
        val tvosX64Main by getting
        val tvosX64Test by getting
        val tvosArm64Main by getting
        val tvosArm64Test by getting
        val macosX64Main by getting
        val macosX64Test by getting
        val macosArm64Main by getting
        val macosArm64Test by getting
        val darwinMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            watchosX64Main.dependsOn(this)
            watchosArm32Main.dependsOn(this)
            watchosArm64Main.dependsOn(this)
            tvosX64Main.dependsOn(this)
            tvosArm64Main.dependsOn(this)
            macosX64Main.dependsOn(this)
            macosArm64Main.dependsOn(this)
        }
        val darwinTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            watchosX64Test.dependsOn(this)
            watchosArm32Test.dependsOn(this)
            watchosArm64Test.dependsOn(this)
            tvosX64Test.dependsOn(this)
            tvosArm64Test.dependsOn(this)
            macosX64Test.dependsOn(this)
            macosArm64Test.dependsOn(this)
        }

        val mingwX64Main by getting
        val mingwX64Test by getting
        val linuxX64Main by getting
        val linuxX64Test by getting
        val linuxArm64Main by getting
        val linuxArm64Test by getting
        val posixMain by creating {
            dependsOn(commonMain)
            mingwX64Main.dependsOn(this)
            linuxX64Main.dependsOn(this)
            linuxArm64Main.dependsOn(this)
        }
        val posixTest by creating {
            dependsOn(commonTest)
            mingwX64Test.dependsOn(this)
            linuxX64Test.dependsOn(this)
            linuxArm64Test.dependsOn(this)
        }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    // Configure all publications.
    @Suppress("LocalVariableName")
    publications.withType<MavenPublication> {
        val publicationDescriptionLibrary: String by project
        val publicationUrl: String by project
        val publicationLicenseName: String by project
        val publicationLicenseUrl: String by project
        val publicationScmUrl: String by project
        val publicationScmConnection: String by project
        val publicationScmDeveloperConnection: String by project
        val publicationDeveloperId: String by project
        val publicationDeveloperName: String by project

        // Publish docs with each artifact.
        artifact(javadocJar)

        // Provide information requited by Maven Central.
        pom {
            name.set(rootProject.name)
            description.set(publicationDescriptionLibrary)
            url.set(publicationUrl)

            licenses {
                license {
                    name.set(publicationLicenseName)
                    url.set(publicationLicenseUrl)
                }
            }

            scm {
                url.set(publicationScmUrl)
                connection.set(publicationScmConnection)
                developerConnection.set(publicationScmDeveloperConnection)
            }

            developers {
                developer {
                    id.set(publicationDeveloperId)
                    name.set(publicationDeveloperName)
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
