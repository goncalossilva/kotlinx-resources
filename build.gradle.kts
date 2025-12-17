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
