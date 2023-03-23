package com.goncalossilva.resources

import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.xhr.XMLHttpRequest

private external fun require(name: String): dynamic

/*
 * It's impossible to separate browser/node JS runtimes, as they can't be published separately.
 * See: https://youtrack.jetbrains.com/issue/KT-47038
 *
 * Workaround inspired by Ktor: https://github.com/ktorio/ktor/blob/b8f18e40baabf9756a16843d6cbd80bff6f006c6/ktor-utils/js/src/io/ktor/util/PlatformUtilsJs.kt#L9-L15
 */
public actual class Resource actual constructor(path: String) {
    private val resourceBrowser: ResourceBrowser by lazy { ResourceBrowser(path) }
    private val resourceNode: ResourceNode by lazy { ResourceNode(path) }

    public actual fun exists(): Boolean = when {
        IS_BROWSER -> resourceBrowser.exists()
        IS_NODE -> resourceNode.exists()
        else -> throw UnsupportedOperationException("Unsupported JS runtime")
    }

    public actual fun readText(): String = when {
        IS_BROWSER -> resourceBrowser.readText()
        IS_NODE -> resourceNode.readText()
        else -> throw UnsupportedOperationException("Unsupported JS runtime")
    }

    public actual fun readBytes(): ByteArray = when {
        IS_BROWSER -> resourceBrowser.readBytes()
        IS_NODE -> resourceNode.readBytes()
        else -> throw UnsupportedOperationException("Unsupported JS runtime")
    }

    private companion object {
        @Suppress("MaxLineLength")
        private val IS_BROWSER: Boolean = js(
            "typeof window !== 'undefined' && typeof window.document !== 'undefined' || typeof self !== 'undefined' && typeof self.location !== 'undefined'"
        ) as Boolean
        private val IS_NODE: Boolean = js(
            "typeof process !== 'undefined' && process.versions != null && process.versions.node != null"
        ) as Boolean
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
        val fs = require("fs")

        fun exists(): Boolean = fs.existsSync(path) as Boolean

        fun readText(): String = runCatching {
            fs.readFileSync(path, "utf8") as String
        }.getOrElse { cause ->
            throw FileReadException("$path: Read failed", cause)
        }

        fun readBytes(): ByteArray = runCatching {
            val buffer = fs.readFileSync(path).unsafeCast<Uint8Array>()
            Int8Array(buffer.buffer, buffer.byteOffset, buffer.length).unsafeCast<ByteArray>()
        }.getOrElse { cause ->
            throw FileReadException("$path: Read failed", cause)
        }
    }
}
