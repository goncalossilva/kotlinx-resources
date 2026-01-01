package com.goncalossilva.resources

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
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
import java.io.PrintWriter
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

    private val karmaResourcesFilesConfig = """
        |config.files.push({
        |   pattern: __dirname + "/**",
        |   watched: false,
        |   included: false,
        |   served: true,
        |   nocache: false
        |});
        """.trimMargin()

    private val karmaProxyResourcesConfig = """
        |(function () {
        |    const path = require("path");
        |    const fs = require("fs");
        |
        |    const baseDir = path.resolve(config.basePath || "");
        |    const decodePath = (input) => {
        |        try {
        |            return decodeURIComponent(input);
        |        } catch (error) {
        |            return input;
        |        }
        |    };
        |    const stripQuery = (input) => input.split("?")[0].split("#")[0];
        |    const normalizeRoot = (root) => {
        |        let normalized = root || "/";
        |        if (!normalized.startsWith("/")) {
        |            normalized = "/" + normalized;
        |        }
        |        if (!normalized.endsWith("/")) {
        |            normalized += "/";
        |        }
        |        return normalized;
        |    };
        |    const resolveBasePath = (url) => {
        |        const pathPart = stripQuery(url || "");
        |        const urlRoot = normalizeRoot(config.urlRoot || "/");
        |        const basePrefixes = ["/base/", urlRoot + "base/"];
        |        for (const basePrefix of basePrefixes) {
        |            if (pathPart == basePrefix.slice(0, -1)) {
        |                return "";
        |            }
        |            if (pathPart.startsWith(basePrefix)) {
        |                return decodePath(pathPart.slice(basePrefix.length));
        |            }
        |        }
        |        return null;
        |    };
        |    const resolveUrlRootPath = (req) => {
        |        const pathPart = stripQuery(req.url || "");
        |        const urlRoot = normalizeRoot(config.urlRoot || "/");
        |        if (!pathPart.startsWith(urlRoot)) {
        |            return null;
        |        }
        |        const method = (req.method || "").toUpperCase();
        |        if (method && method !== "GET" && method !== "HEAD") {
        |            return null;
        |        }
        |        const relativePath = decodePath(pathPart.slice(urlRoot.length));
        |        const lower = relativePath.toLowerCase();
        |        if (lower === "" ||
        |            lower.startsWith("context.") ||
        |            lower.startsWith("debug.") ||
        |            lower.startsWith("karma.") ||
        |            lower.startsWith("adapter.") ||
        |            lower === "favicon.ico"
        |        ) {
        |            return null;
        |        }
        |        return relativePath;
        |    };
        |
        |    const resource404 = function () {
        |        return function resource404Middleware(req, res, next) {
        |            let relativePath = resolveBasePath(req.url);
        |            if (relativePath == null) {
        |                const urlRootPath = resolveUrlRootPath(req);
        |                if (urlRootPath != null) {
        |                    relativePath = urlRootPath;
        |                    req.url = "/base/" + urlRootPath;
        |                }
        |            }
        |            if (relativePath == null) {
        |                return next();
        |            }
        |
        |            const fullPath = path.resolve(baseDir, relativePath);
        |            const relative = path.relative(baseDir, fullPath);
        |            if (relative.startsWith("..") || path.isAbsolute(relative)) {
        |                res.statusCode = 404;
        |                res.setHeader("Content-Type", "text/plain; charset=utf-8");
        |                res.end("Not Found");
        |                return;
        |            }
        |
        |            let stat;
        |            try {
        |                stat = fs.statSync(fullPath);
        |                if (!stat.isFile()) {
        |                    res.statusCode = 404;
        |                    res.setHeader("Content-Type", "text/plain; charset=utf-8");
        |                    res.end("Not Found");
        |                    return;
        |                }
        |            } catch (error) {
        |                res.statusCode = 404;
        |                res.setHeader("Content-Type", "text/plain; charset=utf-8");
        |                res.end("Not Found");
        |                return;
        |            }
        |
        |            if ((req.method || "").toUpperCase() === "HEAD") {
        |                res.statusCode = 200;
        |                res.setHeader("Content-Length", stat.size);
        |                res.end();
        |                return;
        |            }
        |
        |            return next();
        |        };
        |    };
        |    const hasResourceMiddleware = () => {
        |        const plugins = config.plugins || [];
        |        return plugins.some((plugin) => plugin && plugin["middleware:resource404"]);
        |    };
        |    const ensureResourceMiddleware = () => {
        |        if (!hasResourceMiddleware()) {
        |            config.plugins = (config.plugins || []).concat([{
        |                "middleware:resource404": ["factory", resource404]
        |            }]);
        |        }
        |        const middleware = (config.middleware || []).filter(
        |            (name) => name != "resource404"
        |        );
        |        config.middleware = ["resource404"].concat(middleware);
        |    };
        |    const originalSet = config.set.bind(config);
        |    config.set = function (newConfig) {
        |        originalSet(newConfig);
        |        ensureResourceMiddleware();
        |    };
        |
        |    config.set({
        |        "proxies": {
        |           "/__karma__/": "/base/"
        |        },
        |        "urlRoot": "/__karma__/",
        |        "hostname": "127.0.0.1",
        |        "listenAddress": "127.0.0.1",
        |        "plugins": config.plugins,
        |        "middleware": config.middleware
        |    });
        |    ensureResourceMiddleware();
        |})();
        """.trimMargin()

    private fun writeKarmaProxyResourcesConfig(confWriter: PrintWriter) {
        confWriter.println(karmaResourcesFilesConfig)
        confWriter.println(karmaProxyResourcesConfig)
    }

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
                        writeKarmaProxyResourcesConfig(confWriter)
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
     * Wires KMP `androidDeviceTest` resources into AGP's androidTest assets.
     *
     * Must be called early in the build lifecycle, before AGP finalizes its variants.
     *
     * KMP puts device test resources under `src/androidDeviceTest/resources/`, but AGP expects
     * files to be packaged as assets for device tests. This hooks into AGP's variant API and adds
     * those resource directories as static asset sources for the device test component.
     */
    private fun configureAndroidInstrumentedTestAssets(project: Project) {
        project.plugins.withId("com.android.kotlin.multiplatform.library") {
            AndroidAssetsConfigurer.configure(project)
        }
        // Legacy plugin, to remove when AGP 9.0 is widely adopted.
        project.plugins.withId("com.android.library") {
            AndroidAssetsConfigurer.configure(project)
        }
        // Legacy plugin, to remove when AGP 9.0 is widely adopted.
        project.plugins.withId("com.android.application") {
            AndroidAssetsConfigurer.configure(project)
        }
    }

    /**
     * Inner object for Android SDK integration.
     *
     * Kept as a separate object to enable lazy class loading, avoiding ClassNotFoundException
     * when the plugin is applied to non-Android projects.
     */
    private object AndroidAssetsConfigurer {
        fun configure(project: Project) {
            val kotlinExt = project.extensions
                .findByType(org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension::class.java) ?: return
            val androidComponents = project.extensions
                .findByType(com.android.build.api.variant.AndroidComponentsExtension::class.java) ?: return

            androidComponents.onVariants { variant ->
                val androidTest = (variant as? com.android.build.api.variant.HasAndroidTest)
                    ?.androidTest ?: return@onVariants
                val assets = androidTest.sources.assets ?: return@onVariants

                val variantSuffix = variant.name.replaceFirstChar { it.uppercaseChar() }
                val targetSourceSetNames = setOf(
                    "androidDeviceTest",
                    "androidDeviceTest$variantSuffix",
                    // Legacy source set names, to remove when AGP 9.0 is widely adopted.
                    "androidInstrumentedTest",
                    "androidInstrumentedTest$variantSuffix",
                )

                kotlinExt.sourceSets
                    .asSequence()
                    .filter { it.name in targetSourceSetNames }
                    .flatMap { it.resources.srcDirs.asSequence() }
                    .filter { it.exists() }
                    .map { it.absolutePath }
                    .distinct()
                    .forEach(assets::addStaticSourceDirectory)
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
