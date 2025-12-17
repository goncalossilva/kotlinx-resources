package com.goncalossilva.resources

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasAndroidTest
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Wires KMP `androidDeviceTest` resources into AGP's androidTest assets.
 *
 * KMP puts device test resources under `src/androidDeviceTest/resources/`, but AGP expects files to be
 * packaged as assets for device tests. This configurer hooks into AGP's variant API and adds those resource
 * directories as static asset sources for the device test component.
 *
 * Supports both the new `com.android.kotlin.multiplatform.library` plugin (with `androidDeviceTest` source sets)
 * and legacy `com.android.library` plugin (with `androidInstrumentedTest` source sets).
 */
internal class AndroidInstrumentedTestAssetsConfigurer {
    fun configure(project: Project) {
        val kotlinExt = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return
        val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java) ?: return

        androidComponents.onVariants { variant ->
            val androidTest = (variant as? HasAndroidTest)?.androidTest ?: return@onVariants
            val assets = androidTest.sources.assets ?: return@onVariants

            // Collect resources from both androidDeviceTest (new) and androidInstrumentedTest (legacy) source sets.
            val resources = collectDeviceTestResources(kotlinExt, variant.name)
            resources.forEach(assets::addStaticSourceDirectory)
        }
    }

    private fun collectDeviceTestResources(
        kotlinExt: KotlinMultiplatformExtension,
        variantName: String
    ): Sequence<String> {
        val variantSuffix = variantName.replaceFirstChar { it.uppercaseChar() }
        val targetSourceSetNames = setOf(
            "androidDeviceTest",
            "androidDeviceTest$variantSuffix",
            // Legacy source set names, to remove when AGP 9.0 is widely adopted.
            "androidInstrumentedTest",
            "androidInstrumentedTest$variantSuffix",
        )

        return kotlinExt.sourceSets
            .asSequence()
            .filter { it.name in targetSourceSetNames }
            .flatMap { it.resources.srcDirs.asSequence() }
            .filter { it.exists() }
            .map { it.absolutePath }
            .distinct()
    }
}
