import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)

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
    google()
}

android {
    namespace = "com.goncalossilva.resources.test"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("com.goncalossilva:resources-library")
                implementation(libs.androidx.test.runner)
            }
        }
    }
}

detekt {
    config.setFrom(files("../config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}
