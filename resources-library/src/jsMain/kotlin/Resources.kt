package com.goncalossilva.resources

import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.xhr.XMLHttpRequest

private val IS_BROWSER: Boolean = js(IS_BROWSER_JS_CHECK)

private val IS_NODE: Boolean = js(IS_NODE_JS_CHECK)

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

internal actual fun loadManifest(): ResourceManifest = when {
    IS_BROWSER -> loadManifestBrowser()
    IS_NODE -> loadManifestNode()
    else -> throw UnsupportedOperationException("Unsupported JS runtime")
}

private fun loadManifestBrowser(): ResourceManifest {
    val request = XMLHttpRequest().apply {
        open("GET", ResourceManifest.FILENAME, false)
        send()
    }

    @Suppress("MagicNumber")
    return if (request.status in 200..299) {
        ResourceManifest.parse(request.responseText)
    } else {
        throw ResourceDiscoveryException(
            "Resource manifest not found. Ensure kotlinx-resources plugin is applied."
        )
    }
}

private fun loadManifestNode(): ResourceManifest {
    val fs = nodeRequire("fs")

    // Try runtime directory walking first
    return try {
        val files = walkDirectory(fs, ".")
            .filter { it != ResourceManifest.FILENAME }
            .sorted()
        ResourceManifest(files)
    } catch (_: dynamic) {
        // Fall back to manifest file
        loadManifestFromFileNode(fs)
    }
}

private fun loadManifestFromFileNode(fs: dynamic): ResourceManifest {
    return try {
        val content = fs.readFileSync(ResourceManifest.FILENAME, "utf8") as String
        ResourceManifest.parse(content)
    } catch (_: dynamic) {
        ResourceManifest.empty()
    }
}

private fun walkDirectory(fs: dynamic, dir: String): List<String> {
    val results = mutableListOf<String>()
    val entries = fs.readdirSync(dir, js("{ withFileTypes: true }")) as Array<dynamic>

    for (entry in entries) {
        val name = entry.name as String
        val path = if (dir == ".") name else "$dir/$name"

        if (entry.isDirectory() as Boolean) {
            results.addAll(walkDirectory(fs, path))
        } else if (entry.isFile() as Boolean) {
            results.add(path)
        }
    }

    return results
}

private fun nodeRequire(name: String): dynamic {
    return try {
        js("module['' + 'require']")(name)
    } catch (e: dynamic) {
        throw IllegalArgumentException("Module not found: $name", e as? Throwable)
    }
}
