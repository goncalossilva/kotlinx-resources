package com.goncalossilva.resources

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasAndroidTest
import com.android.build.api.variant.TestComponent
import com.android.build.api.variant.Variant
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.util.suffixIfNot
import java.io.File
import javax.inject.Inject
import kotlin.contracts.contract

@Suppress("TooManyFunctions")
class ResourcesPlugin : KotlinCompilerPluginSupportPlugin {
    // Hardcoded because Kotlin Gradle Plugin doesn't expose a typed API for WASI compilation tasks.
    private val wasmWasiCompileTaskName = "compileTestDevelopmentExecutableKotlinWasmWasi"

    override fun apply(target: Project) {
        super.apply(target)

        configureAndroidInstrumentedTestAssets(target)
    }

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
        val isWasmWasi = isWasmWasiCompilation(kotlinCompilation)
        return isTesting && (isNative || isJsNode || isJsBrowser || isWasmWasi)
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        if (isNativeCompilation(kotlinCompilation)) {
            setupNativeResources(kotlinCompilation)
        }
        if ((isJsNodeCompilation(kotlinCompilation) || isJsBrowserCompilation(kotlinCompilation)) &&
            !isWasmWasiCompilation(kotlinCompilation)
        ) {
            setupJsResources(kotlinCompilation)
        }
        if (isWasmWasiCompilation(kotlinCompilation)) {
            setupWasmWasiResources(kotlinCompilation)
        }
        return kotlinCompilation.target.project.provider { emptyList() }
    }

    /**
     * Sets up resources for native platforms by copying them into the binary's output directory.
     *
     * Apple platforms support resources via `NSBundle.mainBundle` and related APIs.
     *
     * Other native platforms don't use native support for resources. Instead,
     * the test task's working directory is set to the binary's output directory.
     */
    private fun setupNativeResources(kotlinCompilation: KotlinCompilation<*>) {
        val compilation = kotlinCompilation as KotlinNativeCompilation
        val project = compilation.target.project
        val target = compilation.target
        val testBinaries = target.binaries.filter { it.outputKind == NativeOutputKind.TEST }

        for (binary in testBinaries) {
            val copyResourcesTask = setupCopyResourcesTask(
                kotlinCompilation = compilation,
                taskName = getTaskName(target.targetName, binary.name, "copyResources"),
                outputDir = project.provider { binary.outputDirectory },
                mustRunAfterTasks = listOf(compilation.processResourcesTaskName),
                dependantTasks = listOf(binary.linkTaskName)
            )

            if (isIosCompilation(compilation)) {
                // HACK: Avoid task dependency conflicts with Compose Multiplatform on iOS.
                val composeResourceTasks = project.tasks.matching { task ->
                    task.name.startsWith("assemble") &&
                        task.name.contains(target.targetName) &&
                        task.name.endsWith("TestResources")
                }
                copyResourcesTask.configure { it.mustRunAfter(composeResourceTasks) }
            } else if (!isAppleCompilation(compilation)) {
                project.tasks.withType(KotlinNativeTest::class.java) { testTask ->
                    testTask.workingDir = binary.outputDirectory.absolutePath
                    testTask.dependsOn(copyResourcesTask)
                }
            }
        }
    }

    /**
     * Sets up resources for JS platforms by copying them into the script's output directory.
     *
     * In the browser, Karma's proxy functionality is leveraged to load resources from the filesystem.
     */
    private fun setupJsResources(kotlinCompilation: KotlinCompilation<*>) {
        val compilation = kotlinCompilation as KotlinJsIrCompilation
        // Unlike others, JS targets don't end with "Test". Add it so matching names is easier.
        val targetName = compilation.target.targetName.suffixIfNot("Test")
        setupCopyResourcesTask(
            kotlinCompilation = compilation,
            taskName = getTaskName(targetName, "copyResources"),
            outputDir = compilation.npmProject.dir.map(Directory::getAsFile),
            mustRunAfterTasks = mutableListOf(compilation.processResourcesTaskName).apply {
                compilation.npmProject.nodeJsRoot.npmInstallTaskProvider.let {
                    add(":${it.name}")
                }
            },
            dependantTasks = listOf(compilation.compileKotlinTaskName)
        )

        if (isJsBrowserCompilation(compilation)) {
            setupProxyResourcesTask(
                kotlinCompilation = compilation,
                taskName = getTaskName(targetName, "proxyResources"),
                // Task where karma.conf.js is created, in KotlinKarma.createTestExecutionSpec.
                mustRunAfterTask = compilation.processResourcesTaskName,
            )
        }
    }

    /**
     * Sets up resources for WASI by copying resources into the output directory and patching the
     * generated `.mjs` file to configure WASI preopens.
     *
     * WASI preopens are a security mechanism that maps host directories into the WebAssembly
     * module's accessible filesystem. Here, the current directory (`.`) is mapped to the resources
     * location so that file operations work correctly.
     */
    private fun setupWasmWasiResources(kotlinCompilation: KotlinCompilation<*>) {
        val project = kotlinCompilation.target.project
        val tasks = project.tasks

        // Derive output directory from the compile task's outputs.
        val outputDir = tasks.named(wasmWasiCompileTaskName).map { task ->
            // Find the directory containing .mjs files from task outputs.
            task.outputs.files.files
                .firstOrNull { it.extension == "mjs" }?.parentFile
                ?: task.outputs.files.files.first().let { file ->
                    if (file.isDirectory) file else file.parentFile
                }
        }

        val copyTask = setupWasmWasiCopyResourcesTask(
            kotlinCompilation = kotlinCompilation,
            taskName = "wasmWasiTestCopyResources",
            outputDir = outputDir
        )

        setupWasmWasiPatchMjsTask(
            kotlinCompilation = kotlinCompilation,
            taskName = "wasmWasiTestPatchMjs",
            outputDir = outputDir,
            dependsOnTask = copyTask
        )
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

    private fun isIosCompilation(kotlinCompilation: KotlinNativeCompilation): Boolean {
        return kotlinCompilation.konanTarget.family == Family.IOS
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

    private fun isWasmWasiCompilation(kotlinCompilation: KotlinCompilation<*>): Boolean {
        // String-based detection because Kotlin Gradle Plugin doesn't expose a KotlinWasmTarget type.
        return kotlinCompilation.target.targetName == "wasmWasi"
    }

    private fun getResourceDirs(kotlinCompilation: KotlinCompilation<*>): List<File> {
        return kotlinCompilation.allKotlinSourceSets.flatMap { sourceSet ->
            sourceSet.resources.srcDirs.filter { it.exists() }
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
    ): TaskProvider<Copy> {
        val project = kotlinCompilation.target.project
        val tasks = project.tasks

        val copyResourcesTask = tasks.register(taskName, Copy::class.java) { task ->
            task.from(getResourceDirs(kotlinCompilation))
            task.include("*/**")
            task.into(outputDir)
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
    ): TaskProvider<Task> {
        val project = kotlinCompilation.target.project
        val tasks = project.tasks
        val confFile = project.projectDir
            .resolve("karma.config.d")
            .apply { mkdirs() }
            // Avoid cleanup races between multiple browser targets (e.g., js/wasmJs).
            .resolve("resources-$taskName.js")

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
                            |       "/__karma__/": "/base/"
                            |    },
                            |    "urlRoot": "/__karma__/",
                            |    "hostname": "127.0.0.1",
                            |    "listenAddress": "127.0.0.1"
                            |});
                            """.trimMargin()
                            )
                    }
                }
            })
            task.mustRunAfter(mustRunAfterTask)
        }

        val cleanupConfFileTask = tasks.register("${taskName}Cleanup", Delete::class.java) {
            it.delete = setOf(confFile)
        }
        tasks.named(getTaskName(kotlinCompilation.target.name, "browser", kotlinCompilation.name)) { browserTestTask ->
            browserTestTask.dependsOn(proxyResourcesTask)
            browserTestTask.finalizedBy(cleanupConfFileTask)
        }

        return proxyResourcesTask
    }

    /**
     * Wires KMP `androidDeviceTest` resources into the Android device/instrumented test APK.
     *
     * KMP places test resources under `src/<sourceSet>/resources`, but Android device/instrumented
     * tests read resources from the APK assets. This hooks into AGP's Variant API and transforms
     * the merged assets artifact to include those resource directories (including transitive
     * `dependsOn` source sets like `commonTest`).
     */
    private fun configureAndroidInstrumentedTestAssets(project: Project) {
        listOf(
            "com.android.kotlin.multiplatform.library",
            "com.android.kotlin.multiplatform.application",
            "com.android.library",
            "com.android.application",
        ).forEach { id ->
            project.plugins.withId(id) {
                project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
                    AndroidAssetsConfigurer.configureWhenReady(project)
                }
            }
        }
    }

    /**
     * Configures Android test variants to package KMP test resources as assets.
     *
     * Kept as a separate object so AGP classes are only loaded when an Android plugin is applied,
     * avoiding `ClassNotFoundException` for non-Android builds.
     */
    private object AndroidAssetsConfigurer {
        private const val configuredMarker = "com.goncalossilva.resources.androidAssetsConfigurer.configured"

        fun configureWhenReady(project: Project) {
            if (project.extensions.extraProperties.has(configuredMarker)) return

            val kotlinExt = project.extensions
                .findByType(KotlinMultiplatformExtension::class.java)
            val androidComponents = project.extensions
                .findByType(AndroidComponentsExtension::class.java)
            if (kotlinExt == null || androidComponents == null) return

            project.extensions.extraProperties.set(configuredMarker, true)

            androidComponents.onVariants { variant -> configureVariant(project, kotlinExt, variant) }
        }

        private fun configureVariant(
            project: Project,
            kotlinExt: KotlinMultiplatformExtension,
            variant: Variant
        ) {
            val testComponents = buildList<TestComponent> {
                (variant as? HasAndroidTest)?.androidTest?.let(::add)
                variant.components.filterIsInstance<TestComponent>()
                    .filter { it.name == "androidTest" || it.name.startsWith("androidDeviceTest") }
                    .let(::addAll)
            }.distinctBy { it.name }
            if (testComponents.isEmpty()) return

            val variantSuffix = variant.name.replaceFirstChar { it.uppercaseChar() }
            for (testComponent in testComponents) {
                val sourceSetPrefix = if (testComponent.name.startsWith("androidDeviceTest")) {
                    "androidDeviceTest"
                } else {
                    // Classic `com.android.*` projects use `androidInstrumentedTest*` for instrumented tests.
                    "androidInstrumentedTest"
                }

                val resourceDirsInOrder = resourceDirsInOrder(kotlinExt, sourceSetPrefix, variantSuffix)
                val existingResourceDirsInOrder = resourceDirsInOrder.filter(File::exists)
                if (existingResourceDirsInOrder.isNotEmpty()) {
                    // KMP places resources under `src/<sourceSet>/resources`, but Android test components
                    // load assets from the APK. Transform the merged assets to include those directories.
                    val taskName = "kotlinxResources" + listOf(variant.name, testComponent.name, "mergeAssets")
                        .joinToString("") { it.replaceFirstChar(Char::titlecase) }
                    val taskProvider = project.tasks.register(
                        taskName,
                        MergeKotlinResourcesIntoAssetsTask::class.java
                    ) { it.additionalAssetDirs.set(existingResourceDirsInOrder) }

                    testComponent.artifacts
                        .use(taskProvider)
                        .wiredWithDirectories(
                            MergeKotlinResourcesIntoAssetsTask::inputAssetsDir,
                            MergeKotlinResourcesIntoAssetsTask::outputAssetsDir
                        )
                        .toTransform(SingleArtifact.ASSETS)
                }
            }
        }

        private fun resourceDirsInOrder(
            kotlinExt: KotlinMultiplatformExtension,
            sourceSetPrefix: String,
            variantSuffix: String
        ): List<File> {
            val targetSourceSets = listOf(
                sourceSetPrefix,
                "$sourceSetPrefix$variantSuffix"
            ).mapNotNull(kotlinExt.sourceSets::findByName)
            if (targetSourceSets.isEmpty()) return emptyList()

            // Expand transitive dependsOn relations so commonTest resources are included.
            val sourceSetsForAssets = LinkedHashSet<KotlinSourceSet>()
            val pendingSourceSets = ArrayDeque<KotlinSourceSet>().apply { addAll(targetSourceSets) }

            while (pendingSourceSets.isNotEmpty()) {
                val sourceSet = pendingSourceSets.removeLast()
                if (sourceSetsForAssets.add(sourceSet)) {
                    pendingSourceSets.addAll(sourceSet.dependsOn)
                }
            }

            return sourceSetsForAssets
                .asSequence()
                .sortedWith(
                    // Sort common* source sets first so platform-specific resources override them.
                    compareBy<KotlinSourceSet> { !it.name.startsWith("common") }.thenBy { it.name }
                )
                .flatMap { it.resources.srcDirs.asSequence() }
                .distinctBy { it.absolutePath }
                .toList()
        }
    }

    abstract class MergeKotlinResourcesIntoAssetsTask : DefaultTask() {
        @get:InputDirectory
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val inputAssetsDir: DirectoryProperty

        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val additionalAssetDirs: ListProperty<File>

        @get:OutputDirectory
        abstract val outputAssetsDir: DirectoryProperty

        @get:Inject
        abstract val fileSystemOperations: FileSystemOperations

        @TaskAction
        fun mergeAssets() {
            fileSystemOperations.sync { spec ->
                // Allow later sources to override earlier ones (e.g., androidDeviceTest overrides commonTest).
                spec.duplicatesStrategy = DuplicatesStrategy.INCLUDE
                spec.into(outputAssetsDir)
                spec.from(inputAssetsDir)
                additionalAssetDirs.get().forEach(spec::from)
            }
        }
    }

    private fun setupWasmWasiCopyResourcesTask(
        kotlinCompilation: KotlinCompilation<*>,
        taskName: String,
        outputDir: Provider<File>
    ): TaskProvider<Task> {
        val project = kotlinCompilation.target.project
        val tasks = project.tasks
        val resourceDirs = getResourceDirs(kotlinCompilation)

        val copyTask = tasks.register(taskName) { task ->
            task.inputs.files(resourceDirs)
            task.dependsOn(wasmWasiCompileTaskName)

            @Suppress("ObjectLiteralToLambda")
            task.doLast(object : Action<Task> {
                override fun execute(task: Task) {
                    // Sort so common* source sets (e.g., commonTest) are processed before
                    // platform-specific ones (e.g., wasmWasiTest), allowing overrides.
                    val sortedDirs = resourceDirs.sortedWith(
                        compareBy { resourceDir: File ->
                            val sourceSetName = resourceDir.parentFile?.name ?: ""
                            if (sourceSetName.startsWith("common")) 0 else 1
                        }
                    )
                    val dir = outputDir.get()
                    for (resourceDir in sortedDirs) {
                        resourceDir.copyRecursively(dir, overwrite = true)
                    }
                }
            })
        }

        return copyTask
    }

    private fun setupWasmWasiPatchMjsTask(
        kotlinCompilation: KotlinCompilation<*>,
        taskName: String,
        outputDir: Provider<File>,
        dependsOnTask: TaskProvider<Task>
    ): TaskProvider<Task> {
        val project = kotlinCompilation.target.project
        val tasks = project.tasks

        val patchTask = tasks.register(taskName) { task ->
            task.dependsOn(dependsOnTask)

            @Suppress("ObjectLiteralToLambda")
            task.doLast(object : Action<Task> {
                override fun execute(task: Task) {
                    val dir = outputDir.get()
                    val mjsFiles = dir.listFiles { file -> file.extension == "mjs" }
                    if (mjsFiles.isNullOrEmpty()) {
                        project.logger.warn("No .mjs files found in $dir for WASI preopens patching")
                        return
                    }

                    val resourcesDir = dir.absolutePath
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                    // Pattern to check if '.' mapping already exists in preopens.
                    val dotMappingPattern = Regex("""preopens\s*:\s*\{[^}]*['"]\.['"]""")
                    // Pattern to find existing preopens and merge into it.
                    val existingPreopensPattern = Regex("""(preopens\s*:\s*\{)([^}]*)(\})""")
                    // Pattern to add preopens to WASI constructor if none exists.
                    val wasiPattern = Regex(
                        pattern = """new\s+WASI\s*\(\s*\{(.*?)}\s*\)""",
                        option = RegexOption.DOT_MATCHES_ALL
                    )

                    for (mjsFile in mjsFiles) {
                        val content = mjsFile.readText()
                        if (dotMappingPattern.containsMatchIn(content)) continue // Already has '.' mapping

                        val patched = if (existingPreopensPattern.containsMatchIn(content)) {
                            // Merge '.' mapping into existing preopens object.
                            existingPreopensPattern.replace(content) { match ->
                                val prefix = match.groupValues[1]
                                val existing = match.groupValues[2].trim().trimEnd(',')
                                val suffix = match.groupValues[3]
                                if (existing.isEmpty()) {
                                    "$prefix '.': '$resourcesDir' $suffix"
                                } else {
                                    "$prefix$existing, '.': '$resourcesDir' $suffix"
                                }
                            }
                        } else {
                            // Add new preopens property to WASI options.
                            wasiPattern.replace(content) { match ->
                                val existingOptions = match.groupValues[1].trim().trimEnd(',').trim()
                                val optionsWithPreopens = if (existingOptions.isEmpty()) {
                                    "preopens: { '.': '$resourcesDir' }"
                                } else {
                                    "$existingOptions, preopens: { '.': '$resourcesDir' }"
                                }
                                "new WASI({ $optionsWithPreopens })"
                            }
                        }
                        if (patched == content) {
                            project.logger.warn("WASI constructor pattern not found in ${mjsFile.name}")
                        }
                        mjsFile.writeText(patched)
                    }
                }
            })
        }

        // Name-based matching because Kotlin Gradle Plugin doesn't expose a public task class.
        val testTaskName = "${kotlinCompilation.target.targetName}NodeTest"
        tasks.matching { it.name == testTaskName }.configureEach { testTask ->
            testTask.dependsOn(patchTask)
        }

        return patchTask
    }
}
