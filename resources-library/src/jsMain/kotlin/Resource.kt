package com.goncalossilva.resources

import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.xhr.XMLHttpRequest

private val IS_BROWSER: Boolean = js(IS_BROWSER_JS_CHECK)

private val IS_NODE: Boolean = js(IS_NODE_JS_CHECK)

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

    /*
     * Browser-based resource implementation.
     */
    private class ResourceBrowser(private val path: String) {
        private fun request(config: (XMLHttpRequest.() -> Unit)? = null): XMLHttpRequest = runCatching {
            XMLHttpRequest().apply {
                open("GET", path, false)
                config?.invoke(this)
                send()
            }
        }.getOrElse { cause ->
            throw FileReadException("$path: Request failed", cause)
        }

        @Suppress("MagicNumber")
        private fun XMLHttpRequest.isSuccessful() = status in 200..299

        fun exists(): Boolean = try {
            request().isSuccessful()
        } catch (_: FileReadException) {
            false
        }

        fun readText(): String {
            val request = request()
            return if (request.isSuccessful()) {
                request.responseText
            } else {
                throw FileReadException("$path: Read failed (status=${request.status})")
            }
        }

        fun readBytes(): ByteArray {
            val request = request {
                // https://web.archive.org/web/20071103070418/http://mgran.blogspot.com/2006/08/downloading-binary-streams-with.html
                overrideMimeType("text/plain; charset=x-user-defined")
            }
            return if (request.isSuccessful()) {
                val response = request.responseText
                ByteArray(response.length) { response[it].code.toUByte().toByte() }
            } else {
                throw FileReadException("$path: Read failed (status=${request.status})")
            }
        }
    }

    /*
     * Node-based resource implementation.
     */
    private class ResourceNode(val path: String) {
        val fs = nodeRequire("fs")

        private fun nodeRequire(name: String): dynamic {
            // Alternative to declaring `private external fun require(name: String): dynamic` and
            // using `require("fs")` directly, since it will cause webpack to complain when running
            // on the browser, even though the code is unused.

            return try {
                js("module['' + 'require']")(name)
            } catch (e: dynamic) {
                throw IllegalArgumentException("Module not found: $name", e as? Throwable)
            }
        }

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
