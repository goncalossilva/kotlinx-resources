package com.goncalossilva.resources

import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.xhr.XMLHttpRequest

private fun hasNodeApi(): Boolean = js("""{
    (typeof process !== 'undefined' 
        && process.versions != null 
        && process.versions.node != null) ||
    (typeof window !== 'undefined' 
        && typeof window.process !== 'undefined' 
        && window.process.versions != null 
        && window.process.versions.node != null)
}"""
)

private fun readFileSync(path: String, options: String): String = js("require('fs').readFileSync(path, options)")

private fun readFileSync(path: String): Uint8Array = js("require('fs').readFileSync(path)")

private fun existsSync(path: String): Boolean = js("require('fs').existsSync(path)")

/*
 * It's impossible to separate browser/node JS runtimes, as they can't be published separately.
 * See: https://youtrack.jetbrains.com/issue/KT-47038
 *
 * Workaround inspired by Ktor: https://github.com/ktorio/ktor/blob/b8f18e40baabf9756a16843d6cbd80bff6f006c6/ktor-utils/js/src/io/ktor/util/PlatformUtilsJs.kt#L9-L15
 */
public actual class Resource actual constructor(path: String) {
    private val resourceBrowser: ResourceBrowser by lazy { ResourceBrowser(path) }
    private val resourceNode: ResourceNode by lazy { ResourceNode(path) }

    public actual fun exists(): Boolean = if (HAS_NODE_API) {
        resourceNode.exists()
    } else {
        resourceBrowser.exists()
    }

    public actual fun readText(): String = if (HAS_NODE_API) {
        resourceNode.readText()
    } else {
        resourceBrowser.readText()
    }

    public actual fun readBytes(): ByteArray = if (HAS_NODE_API) {
        resourceNode.readBytes()
    } else {
        resourceBrowser.readBytes()
    }

    private companion object {
        @Suppress("MaxLineLength")
        private val HAS_NODE_API: Boolean = hasNodeApi()
    }

    /*
     * Browser-based resource implementation.
     */
    private class ResourceBrowser(private val path: String) {
        private fun request(config: (XMLHttpRequest.() -> Unit)? = null) = XMLHttpRequest().apply {
            open("GET", path, false)
            config?.invoke(this)
            send()
        }

        @Suppress("MagicNumber")
        fun exists(): Boolean = request().status in 200..299

        fun readText(): String = request().let { request ->
            if (exists()) {
                request.responseText
            } else {
                throw FileReadException("$path: Read failed: ${request.statusText}")
            }
        }

        fun readBytes(): ByteArray = request {
            // https://web.archive.org/web/20071103070418/http://mgran.blogspot.com/2006/08/downloading-binary-streams-with.html
            overrideMimeType("text/plain; charset=x-user-defined")
        }.let { request ->
            if (exists()) {
                val response = request.responseText
                ByteArray(response.length) { response[it].code.toUByte().toByte() }
            } else {
                throw FileReadException("$path: Read failed: ${request.statusText}")
            }
        }
    }

    /*
     * Node-based resource implementation.
     */
    private class ResourceNode(val path: String) {
        fun exists(): Boolean = existsSync(path)

        fun readText(): String = runCatching {
            readFileSync(path, "utf8")
        }.getOrElse { cause ->
            throw FileReadException("$path: Read failed", cause)
        }

        fun readBytes(): ByteArray = runCatching {
            val buffer = readFileSync(path)
            Int8Array(buffer.buffer, buffer.byteOffset, buffer.length) as ByteArray
        }.getOrElse { cause ->
            throw FileReadException("$path: Read failed", cause)
        }
    }
}
