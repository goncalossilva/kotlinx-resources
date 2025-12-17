package com.goncalossilva.resources

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasAndroidTest
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal class AndroidInstrumentedTestAssetsConfigurer : AndroidAssetsConfigurer {
    override fun configure(project: Project) {
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        val kotlinExt = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return

        androidComponents.onVariants { variant ->
            val androidTest = (variant as? HasAndroidTest)?.androidTest ?: return@onVariants
            val assets = androidTest.sources.assets ?: return@onVariants

            kotlinExt.sourceSets
                .asSequence()
                .filter { it.name.contains("androidInstrumentedTest", ignoreCase = true) }
                .flatMap { it.resources.srcDirs.asSequence() }
                .filter { it.exists() }
                .map { it.absolutePath }
                .distinct()
                .forEach(assets::addStaticSourceDirectory)
        }
    }
}
