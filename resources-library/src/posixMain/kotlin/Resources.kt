package com.goncalossilva.resources

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.S_IFDIR
import platform.posix.S_IFMT
import platform.posix.closedir
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.opendir
import platform.posix.readdir
import platform.posix.stat

public actual object Resources {
    private val manifest: ResourceManifest by lazy { loadManifest() }

    public actual fun root(): ResourceDirectory = ResourceDirectory("", manifest)

    public actual fun get(path: String): ResourceEntry? = manifest.get(path)

    public actual fun find(predicate: (ResourceEntry) -> Boolean): List<ResourceEntry> =
        manifest.allFiles()
            .map { ResourceFile(it) }
            .filter(predicate)

    public actual fun findByExtension(extension: String): List<ResourceFile> =
        manifest.allFiles()
            .filter { it.endsWith(".$extension") }
            .map { ResourceFile(it) }

    public actual fun list(): List<String> = manifest.allFiles()
}

internal actual fun loadManifest(): ResourceManifest {
    // Try runtime directory walking first
    return try {
        val files = walkDirectory(".")
            .filter { it != ResourceManifest.FILENAME }
            .sorted()
        if (files.isNotEmpty()) {
            ResourceManifest(files)
        } else {
            loadManifestFromFile()
        }
    } catch (_: Exception) {
        loadManifestFromFile()
    }
}

private fun loadManifestFromFile(): ResourceManifest {
    val file = fopen(ResourceManifest.FILENAME, "r") ?: return ResourceManifest.empty()

    return try {
        val content = buildString {
            memScoped {
                val buffer = allocArray<ByteVar>(BUFFER_SIZE)
                do {
                    val line = fgets(buffer, BUFFER_SIZE, file)?.also { append(it.toKString()) }
                } while (line != null)
            }
        }
        ResourceManifest.parse(content)
    } finally {
        fclose(file)
    }
}

private fun walkDirectory(path: String): List<String> {
    val results = mutableListOf<String>()
    val dir = opendir(path) ?: return results

    try {
        memScoped {
            val statBuf = alloc<stat>()

            while (true) {
                val entry = readdir(dir) ?: break
                val name = entry.pointed.d_name.toKString()

                // Skip . and ..
                if (name == "." || name == "..") continue

                val fullPath = if (path == ".") name else "$path/$name"

                // Use stat to determine if it's a directory or file
                if (stat(fullPath, statBuf.ptr) == 0) {
                    val isDir = (statBuf.st_mode.toInt() and S_IFMT) == S_IFDIR
                    if (isDir) {
                        results.addAll(walkDirectory(fullPath))
                    } else {
                        results.add(fullPath)
                    }
                }
            }
        }
    } finally {
        closedir(dir)
    }

    return results
}

private const val BUFFER_SIZE = 8 * 1024
