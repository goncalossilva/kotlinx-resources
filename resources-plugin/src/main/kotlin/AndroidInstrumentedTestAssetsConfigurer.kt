package com.goncalossilva.resources

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasAndroidTest
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Wires KMP `androidInstrumentedTest` resources into AGP's androidTest assets.
 *
 * KMP puts instrumented test resources under `src/androidInstrumentedTest/resources/`, but AGP expects files to be
 * packaged as assets for instrumented tests. This configurer hooks into AGP's variant API and adds those resource
 * directories as static asset sources for the `androidTest` component.
 */
internal class AndroidInstrumentedTestAssetsConfigurer {
    fun configure(project: Project) {
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        val kotlinExt = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return
        val androidInstrumentedTestCommon = "androidInstrumentedTest"

        androidComponents.onVariants { variant ->
            val androidTest = (variant as? HasAndroidTest)?.androidTest ?: return@onVariants
            val assets = androidTest.sources.assets ?: return@onVariants

            val variantSuffix = variant.name.replaceFirstChar { it.uppercaseChar() }
            val targetSourceSetNames = setOf(
                androidInstrumentedTestCommon,
                "$androidInstrumentedTestCommon$variantSuffix",
            )

            kotlinExt.sourceSets
                .asSequence()
                // Source sets are only identified by name (no typed API), but we can be strict and support
                // variant-specific source sets like androidInstrumentedTestDebug.
                .filter { it.name in targetSourceSetNames }
                .flatMap { it.resources.srcDirs.asSequence() }
                .filter { it.exists() }
                .map { it.absolutePath }
                .distinct()
                .forEach(assets::addStaticSourceDirectory)
        }
    }
}
