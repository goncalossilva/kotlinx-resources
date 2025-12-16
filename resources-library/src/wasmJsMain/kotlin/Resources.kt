@file:OptIn(ExperimentalWasmJsInterop::class)

package com.goncalossilva.resources

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsString
import kotlin.js.toJsString

private val IS_BROWSER: Boolean = js(IS_BROWSER_JS_CHECK)

private val IS_NODE: Boolean = js(IS_NODE_JS_CHECK)

private fun nodeReadFileSync(path: JsString, encoding: JsString): JsString =
    js("require('fs').readFileSync(path, encoding)")

private fun nodeReaddirSync(path: JsString): JsArray<JsDirent> =
    js("require('fs').readdirSync(path, { withFileTypes: true })")

private external class JsDirent : JsAny {
    val name: JsString
    fun isDirectory(): Boolean
    fun isFile(): Boolean
}

private external class XmlHttpRequest : JsAny {
    fun open(method: JsString, url: JsString, async: Boolean)
    fun send()
    val status: Int
    val statusText: JsString
    val responseText: JsString
}

private fun createXmlHttpRequest(): XmlHttpRequest = js("new XMLHttpRequest()")

private fun jsArrayLength(arr: JsAny?): Int = js("arr.length")

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
    val request = createXmlHttpRequest()
    request.open("GET".toJsString(), ResourceManifest.FILENAME.toJsString(), false)
    request.send()

    @Suppress("MagicNumber")
    return if (request.status in 200..299) {
        ResourceManifest.parse(request.responseText.toString())
    } else {
        throw ResourceDiscoveryException(
            "Resource manifest not found. Ensure kotlinx-resources plugin is applied."
        )
    }
}

private fun loadManifestNode(): ResourceManifest {
    // Try runtime directory walking first
    return try {
        val files = walkDirectory(".")
            .filter { it != ResourceManifest.FILENAME }
            .sorted()
        ResourceManifest(files)
    } catch (_: Throwable) {
        // Fall back to manifest file
        loadManifestFromFileNode()
    }
}

private fun loadManifestFromFileNode(): ResourceManifest {
    return try {
        val content = nodeReadFileSync(
            ResourceManifest.FILENAME.toJsString(),
            "utf8".toJsString()
        ).toString()
        ResourceManifest.parse(content)
    } catch (_: Throwable) {
        ResourceManifest.empty()
    }
}

private fun walkDirectory(dir: String): List<String> {
    val results = mutableListOf<String>()
    val entries = nodeReaddirSync(dir.toJsString())
    val length = jsArrayLength(entries)

    for (i in 0 until length) {
        val entry = entries[i]!!
        val name = entry.name.toString()
        val path = if (dir == ".") name else "$dir/$name"

        if (entry.isDirectory()) {
            results.addAll(walkDirectory(path))
        } else if (entry.isFile()) {
            results.add(path)
        }
    }

    return results
}
