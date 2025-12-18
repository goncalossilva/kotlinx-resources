import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)

    alias(libs.plugins.buildconfig)

    id("java-gradle-plugin")
    id("maven-publish")
    id("signing")
    alias(libs.plugins.gradle.publish)

    alias(libs.plugins.detekt)
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    compileOnly(kotlin("gradle-plugin"))
    compileOnly(kotlin("stdlib"))
    compileOnly("com.android.tools.build:gradle:${libs.versions.agp.get()}")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
        freeCompilerArgs.add("-opt-in=kotlin.contracts.ExperimentalContracts")
    }
}

val artifactId: String by project
val pluginId = "$group.$artifactId"

buildConfig {
    packageName.set(pluginId)
    buildConfigField("String", "GROUP_ID", "\"$group\"")
    buildConfigField("String", "ARTIFACT_ID", "\"$artifactId\"")
    buildConfigField("String", "PLUGIN_ID", "\"$pluginId\"")
    buildConfigField("String", "VERSION", "\"$version\"")
    useKotlinOutput { internalVisibility = true }
}

gradlePlugin {
    val publicationUrl: String by project
    val publicationScmUrl: String by project

    website.set(publicationUrl)
    vcsUrl.set(publicationScmUrl)

    val resources by plugins.creating {
        val publicationDisplayNamePlugin: String by project
        val publicationDescriptionPlugin: String by project
        val publicationTags: String by project

        id = pluginId
        implementationClass = "com.goncalossilva.resources.ResourcesPlugin"
        displayName = publicationDisplayNamePlugin
        description = publicationDescriptionPlugin
        tags.set(publicationTags.split(','))
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

tasks.named("publish") {
    dependsOn("publishPlugins")
}

detekt {
    config.setFrom(files("../config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}
