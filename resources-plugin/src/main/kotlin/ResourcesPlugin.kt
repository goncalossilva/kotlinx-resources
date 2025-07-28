package com.goncalossilva.resources

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.internal.provider.ValueSupplier.ValueProducer.task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.internal.builtins.StandardNames.FqNames.target
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.konan.target.Family
import java.io.File
import kotlin.contracts.contract
import kotlin.jvm.java

@Suppress("TooManyFunctions")
class ResourcesPlugin : KotlinCompilerPluginSupportPlugin {
    override fun getCompilerPluginId() = BuildConfig.PLUGIN_ID

    override fun getPluginArtifact() = SubpluginArtifact(
        groupId = BuildConfig.GROUP_ID,
        artifactId = BuildConfig.ARTIFACT_ID,
        version = BuildConfig.VERSION
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val isTesting = isCompiledForTesting(kotlinCompilation)
        val isNative = isNativeCompilation(kotlinCompilation)
        val isJsNode = isJsNodeCompilation(kotlinCompilation)
        val isJsBrowser = isJsBrowserCompilation(kotlinCompilation)
        return isTesting && (isNative || isJsNode || isJsBrowser)
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        /*
         * For native platforms, copy resources into the binary's output directory.
         *
         * Apple platforms support resources via `NSBundle.mainBundle` and related APIs.
         *
         * Other native platforms don't use native support for resources. Instead,
         * ensure the test task's working directory is the binary's output directory.
         */
        if (isNativeCompilation(kotlinCompilation)) {
            val target = kotlinCompilation.target
            val binary = target.binaries.first { it.outputKind == NativeOutputKind.TEST }
            val copyResourcesTask = setupCopyResourcesTask(
                kotlinCompilation = kotlinCompilation,
                taskName = getTaskName("copyResources", binary.name, target.targetName),
                outputDir = project.provider { binary.outputDirectory },
                mustRunAfterTasks = listOf(kotlinCompilation.processResourcesTaskName),
                dependantTasks = listOf(binary.linkTaskName)
            )

            if (!isAppleCompilation(kotlinCompilation)) {
                project.tasks.withType(KotlinNativeTest::class.java) { testTask ->
                    testTask.workingDir = binary.outputDirectory.absolutePath
                    testTask.dependsOn(copyResourcesTask)
                }
            }
        }

        /*
         * For Node and the browser, copy resources into the script's output directory.
         */
        if (isJsNodeCompilation(kotlinCompilation) || isJsBrowserCompilation(kotlinCompilation)) {
            setupCopyResourcesTask(
                kotlinCompilation = kotlinCompilation,
                taskName = getTaskName("copyResources", kotlinCompilation.target.targetName),
                outputDir = kotlinCompilation.npmProject.dir.map(Directory::getAsFile),
                mustRunAfterTasks = mutableListOf(kotlinCompilation.processResourcesTaskName).apply {
                    kotlinCompilation.npmProject.nodeJsRoot.npmInstallTaskProvider.let {
                        add(":${it.name}")
                    }
                },
                dependantTasks = listOf(kotlinCompilation.compileKotlinTaskName)
            )
        }

        /*
         * For the browser, copy resources into the script's output directory and leverage Karma's
         * proxy functionality to load them from the filesystem.
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

        // Do not copy resource duplicates, due to we rely on our copyResources task
        if (isJsBrowserCompilation(kotlinCompilation)) {
            project.tasks.named("jsTestProcessResources", ProcessResources::class.java) { task ->
                task.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }
        }

        return project.provider { emptyList() }
    }

    private fun isCompiledForTesting(kotlinCompilation: KotlinCompilation<*>) =
        kotlinCompilation.compilationName == KotlinCompilation.TEST_COMPILATION_NAME

    private fun isNativeCompilation(kotlinCompilation: KotlinCompilation<*>): Boolean {
        contract {
            returns(true) implies (kotlinCompilation is KotlinNativeCompilation)
        }
        return kotlinCompilation is KotlinNativeCompilation
    }

    private fun isAppleCompilation(kotlinCompilation: KotlinNativeCompilation): Boolean {
        return kotlinCompilation.konanTarget.family.isAppleFamily
    }

    private fun isJsNodeCompilation(kotlinCompilation: KotlinCompilation<*>): Boolean {
        contract {
            returns(true) implies (kotlinCompilation is KotlinJsIrCompilation)
        }
        return kotlinCompilation is KotlinJsIrCompilation && kotlinCompilation.target.isNodejsConfigured
    }

    private fun isJsBrowserCompilation(kotlinCompilation: KotlinCompilation<*>): Boolean {
        contract {
            returns(true) implies (kotlinCompilation is KotlinJsIrCompilation)
        }
        return kotlinCompilation is KotlinJsIrCompilation && kotlinCompilation.target.isBrowserConfigured
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
        outputDir: Provider<File>,
        mustRunAfterTasks: List<String>,
        dependantTasks: List<String>
    ): TaskProvider<Copy>? {
        val project = kotlinCompilation.target.project
        val tasks = project.tasks

        val copyResourcesTask = tasks.register(taskName, Copy::class.java) { task ->
            task.from(getResourceDirs(kotlinCompilation))
            task.include("*/**")
            task.into(outputDir)
            task.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
                        confWriter.println(
                            """
                                |config.files.push({
                                |   pattern: __dirname + "/**",
                                |   watched: false,
                                |   included: false,
                                |   served: true,
                                |   nocache: false
                                |});
                                """.trimMargin()
                        )
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
