import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)

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

kotlin {
    androidLibrary {
        namespace = "com.goncalossilva.resources.test"
        compileSdk = libs.versions.android.sdk.compile.get().toInt()
        minSdk = libs.versions.android.sdk.min.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }

        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonTest by getting
        val androidDeviceTest by getting {
            dependsOn(commonTest)
        }

        androidDeviceTest.dependencies {
            implementation(kotlin("test"))
            implementation(kotlin("test-junit"))
            implementation("com.goncalossilva:resources-library")
            implementation(libs.androidx.test.runner)
        }
    }
}

detekt {
    config.setFrom(files("../config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}
