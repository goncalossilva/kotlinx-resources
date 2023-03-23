package com.goncalossilva.resources

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetContainerDsl
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import java.io.File
import kotlin.contracts.contract

@Suppress("TooManyFunctions")
class ResourcesPlugin : KotlinCompilerPluginSupportPlugin {
    override fun getCompilerPluginId() = BuildConfig.PLUGIN_ID

    override fun getPluginArtifact() = SubpluginArtifact(
        groupId = BuildConfig.GROUP_ID,
        artifactId = BuildConfig.ARTIFACT_ID,
        version = BuildConfig.VERSION
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>) =
        isCompiledForTesting(kotlinCompilation) && hasResources(kotlinCompilation) && (
            isAppleCompilation(kotlinCompilation) || isJsNodeCompilation(kotlinCompilation) ||
                isJsBrowserCompilation(kotlinCompilation)
            )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        /*
         * For Apple platforms, move resources into the binary's output directory so that they
         * can be loaded using `NSBundle.mainBundle` and related APIs.
         */
        if (isAppleCompilation(kotlinCompilation)) {
            val target = kotlinCompilation.target
            target.binaries.forEach { binary ->
                setupCopyResourcesTask(
                    kotlinCompilation = kotlinCompilation,
                    taskName = getTaskName("copyResources", binary.name, target.targetName),
                    outputDirectory = binary.outputDirectory,
                    mustRunAfterTasks = listOf(kotlinCompilation.processResourcesTaskName),
                    dependantTasks = listOf(binary.linkTaskName)
                )
            }
        }

        /*
         * For Node and the browser, move resources into the script's output directory.
         */
        if (isJsNodeCompilation(kotlinCompilation) || isJsBrowserCompilation(kotlinCompilation)) {
            setupCopyResourcesTask(
                kotlinCompilation = kotlinCompilation,
                taskName = getTaskName("copyResources", kotlinCompilation.target.targetName),
                outputDirectory = kotlinCompilation.npmProject.dir,
                mustRunAfterTasks = mutableListOf(kotlinCompilation.processResourcesTaskName).apply {
                    kotlinCompilation.npmProject.nodeJs.npmInstallTaskProvider?.let {
                        add(":${it.name}")
                    }
                },
                dependantTasks = listOf(kotlinCompilation.compileKotlinTaskName)
            )
        }

        /*
         * For the browser, move resources into the script's output directory and leverage
         * Karma's proxies to load them from the filesystem.
         */
        if (isJsBrowserCompilation(kotlinCompilation)) {
            setupProxyResourcesTask(
                kotlinCompilation = kotlinCompilation,
                taskName = getTaskName("proxyResources", kotlinCompilation.target.targetName),
                // Task where karma.conf.js is created, in KotlinKarma.createTestExecutionSpec.
                mustRunAfterTask = kotlinCompilation.processResourcesTaskName,
                dependantTask = kotlinCompilation.compileKotlinTaskName
            )
        }

        return project.provider { emptyList() }
    }

    private fun isCompiledForTesting(kotlinCompilation: KotlinCompilation<*>) =
        kotlinCompilation.compilationName == KotlinCompilation.TEST_COMPILATION_NAME

    private fun isAppleCompilation(kotlinCompilation: KotlinCompilation<*>): Boolean {
        contract {
            returns(true) implies (kotlinCompilation is KotlinNativeCompilation)
        }
        return kotlinCompilation is KotlinNativeCompilation &&
            kotlinCompilation.konanTarget.family.isAppleFamily
    }

    private fun isJsNodeCompilation(kotlinCompilation: KotlinCompilation<*>): Boolean {
        contract {
            returns(true) implies (kotlinCompilation is KotlinJsCompilation)
        }
        return kotlinCompilation is KotlinJsCompilation && kotlinCompilation.target.let {
            it is KotlinJsSubTargetContainerDsl && it.isNodejsConfigured
        }
    }

    private fun isJsBrowserCompilation(kotlinCompilation: KotlinCompilation<*>): Boolean {
        contract {
            returns(true) implies (kotlinCompilation is KotlinJsCompilation)
        }
        return kotlinCompilation is KotlinJsCompilation && kotlinCompilation.target.let {
            it is KotlinJsSubTargetContainerDsl && it.isBrowserConfigured
        }
    }

    private fun hasResources(kotlinCompilation: KotlinCompilation<*>) =
        kotlinCompilation.allKotlinSourceSets.any { sourceSet ->
            sourceSet.resources.srcDirs.any { !it.listFiles().isNullOrEmpty() }
        }

    private fun getResourceDirs(kotlinCompilation: KotlinCompilation<*>): List<String> {
        val projectDirPath = kotlinCompilation.target.project.projectDir.invariantSeparatorsPath
        return kotlinCompilation.allKotlinSourceSets.flatMap { sourceSet ->
            // Paths should be relative to the project's directory.
            sourceSet.resources.srcDirs.filter { it.exists() }.map {
                it.invariantSeparatorsPath.removePrefix(projectDirPath).trimStart('/')
            }
        }
    }

    private fun getTaskName(prefix: String, vararg qualifiers: String) =
        "$prefix${qualifiers.joinToString("") { it.replaceFirstChar(Char::titlecase) }}"

    private fun setupCopyResourcesTask(
        kotlinCompilation: KotlinCompilation<*>,
        taskName: String,
        outputDirectory: File,
        mustRunAfterTasks: List<String>,
        dependantTasks: List<String>
    ): TaskProvider<Copy>? {
        val project = kotlinCompilation.target.project
        val tasks = project.tasks
        val resourceDirs = getResourceDirs(kotlinCompilation).map { "$it/**" }

        val copyResourcesTask = tasks.register(taskName, Copy::class.java) { task ->
            task.from(project.projectDir)
            task.include(resourceDirs)
            task.into(outputDirectory)
            for (mustRunAfterTask in mustRunAfterTasks) {
                task.mustRunAfter(mustRunAfterTask)
            }
        }
        for (dependantTask in dependantTasks) {
            tasks.named(dependantTask).configure {
                it.dependsOn(copyResourcesTask)
            }
        }

        return copyResourcesTask
    }

    private fun setupProxyResourcesTask(
        kotlinCompilation: KotlinCompilation<*>,
        taskName: String,
        mustRunAfterTask: String,
        dependantTask: String
    ): TaskProvider<Task> {
        val project = kotlinCompilation.target.project
        val tasks = project.tasks
        val confFile = project.projectDir
            .resolve("karma.config.d")
            .apply { mkdirs() }
            .resolve("proxy-resources.js")

        val proxyResourcesTask = tasks.register(taskName) { task ->
            @Suppress("ObjectLiteralToLambda")
            task.doLast(object : Action<Task> {
                override fun execute(task: Task) {
                    // Create karma configuration file in the expected location, deleting when done.
                    confFile.printWriter().use { confWriter ->
                        getResourceDirs(kotlinCompilation).forEach { resourceDir ->
                            confWriter.println(
                                """
                                |config.files.push({
                                |   pattern: __dirname + "/$resourceDir/**",
                                |   watched: false,
                                |   included: false,
                                |   served: true,
                                |   nocache: false
                                |});
                                """.trimMargin()
                            )
                        }
                        confWriter.println(
                            """
                            |config.set({
                            |    "proxies": {
                            |       "/": __dirname + "/"
                            |    },
                            |    "urlRoot": "/__karma__/"
                            |});
                            """.trimMargin()
                        )
                    }
                }
            })
            task.mustRunAfter(mustRunAfterTask)
        }
        tasks.named(dependantTask).configure {
            it.dependsOn(proxyResourcesTask)
        }

        val cleanupConfFileTask = tasks.register("${taskName}Cleanup", Delete::class.java) {
            it.delete = setOf(confFile)
        }
        tasks.named(getTaskName(kotlinCompilation.target.name, "browser", kotlinCompilation.name)) {
            it.finalizedBy(cleanupConfFileTask)
        }

        return proxyResourcesTask
    }
}
