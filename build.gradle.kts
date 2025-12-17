plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.useanybrowser) apply false
    alias(libs.plugins.detekt) apply false
}

repositories {
    mavenCentral()
}

// Install git hooks automatically.
gradle.taskGraph.whenReady {
    val from = rootProject.file("config/detekt/pre-commit")
    val hooksDir = runCatching {
        val process = ProcessBuilder("git", "rev-parse", "--git-path", "hooks")
            .directory(rootProject.rootDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        val exitCode = process.waitFor()
        output.takeIf { exitCode == 0 && it.isNotBlank() }?.let(rootProject::file)
    }.getOrNull() ?: return@whenReady
    val to = hooksDir.resolve("pre-commit")
    to.parentFile.mkdirs()
    from.copyTo(to, overwrite = true)
    to.setExecutable(true)
}
