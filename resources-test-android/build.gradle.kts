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

        // Enable Android host/unit tests (task: :resources-test-android:testAndroidHostTest) to exercise classpath resources.
        withHostTest {}

        // Enable Android device/instrumented tests (task: :resources-test-android:connectedAndroidDeviceTest) to exercise asset resources.
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonTest by getting
        val androidHostTest by getting
        val androidDeviceTest by getting

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("com.goncalossilva:resources-library")
        }

        androidDeviceTest.dependencies {
            // androidDeviceTest must be in a different hierarchy tree than androidHostTest (which depends on commonTest),
            // so it doesn't inherit commonTest dependencies.
            implementation(kotlin("test"))
            implementation("com.goncalossilva:resources-library")
            implementation(libs.androidx.test.runner)
        }
    }
}

detekt {
    config.setFrom(files("../config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

// Exclude duplicate resources. This way, platform-specific take precedence.
afterEvaluate {
    tasks.withType<AbstractCopyTask>().configureEach {
        if (name.contains("Test") && (name.endsWith("Resources") || name.endsWith("JavaRes"))) {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}
