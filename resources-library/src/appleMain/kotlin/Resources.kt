package com.goncalossilva.resources

import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSDataReadingUncached
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.stringWithContentsOfFile

@OptIn(UnsafeNumber::class)
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

@OptIn(UnsafeNumber::class)
internal actual fun loadManifest(): ResourceManifest {
    // Try runtime directory walking first using NSBundle's resource path
    val resourcePath = NSBundle.mainBundle.resourcePath
    if (resourcePath != null) {
        return try {
            val files = walkDirectory(resourcePath, resourcePath)
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

    return loadManifestFromFile()
}

@OptIn(UnsafeNumber::class)
private fun loadManifestFromFile(): ResourceManifest {
    val manifestPath = NSBundle.mainBundle.pathForResource(
        ResourceManifest.FILENAME.substringBeforeLast("."),
        ResourceManifest.FILENAME.substringAfterLast(".")
    ) ?: return ResourceManifest.empty()

    return memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val content = NSString.stringWithContentsOfFile(manifestPath, NSUTF8StringEncoding, error.ptr)
            ?: return@memScoped ResourceManifest.empty()
        ResourceManifest.parse(content)
    }
}

@OptIn(UnsafeNumber::class)
@Suppress("UNCHECKED_CAST")
private fun walkDirectory(path: String, basePath: String): List<String> {
    val fileManager = NSFileManager.defaultManager
    val results = mutableListOf<String>()

    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val contents = fileManager.contentsOfDirectoryAtPath(path, error.ptr)
            ?: return results

        for (item in contents) {
            val name = item as? String ?: continue
            val fullPath = "$path/$name"
            val relativePath = fullPath.removePrefix("$basePath/")

            val isDir = alloc<kotlinx.cinterop.BooleanVar>()
            val exists = fileManager.fileExistsAtPath(fullPath, isDir.ptr)

            if (exists) {
                if (isDir.value) {
                    results.addAll(walkDirectory(fullPath, basePath))
                } else {
                    results.add(relativePath)
                }
            }
        }
    }

    return results
}
