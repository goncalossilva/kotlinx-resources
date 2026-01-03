package com.goncalossilva.resources

import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.xhr.XMLHttpRequest
import kotlin.js.definedExternally

private val IS_BROWSER: Boolean = js(IS_BROWSER_JS_CHECK)

private val IS_NODE: Boolean = js(IS_NODE_JS_CHECK)

private external class TextDecoder(encoding: String = definedExternally) {
    fun decode(input: Uint8Array): String
}

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

    public actual fun readText(charset: Charset): String = when {
        IS_BROWSER -> resourceBrowser.readText(charset)
        IS_NODE -> resourceNode.readText(charset)
        else -> throw UnsupportedOperationException("Unsupported JS runtime")
    }

    public actual fun readBytes(): ByteArray = when {
        IS_BROWSER -> resourceBrowser.readBytes()
        IS_NODE -> resourceNode.readBytes()
        else -> throw UnsupportedOperationException("Unsupported JS runtime")
    }

    /**
     * Resource access via XMLHttpRequest (for browser environments).
     */
    private class ResourceBrowser(private val path: String) {
        private fun request(
            method: String = "GET",
            config: (XMLHttpRequest.() -> Unit)? = null,
        ): XMLHttpRequest = runCatching {
            XMLHttpRequest().apply {
                open(method, path, false)
                config?.invoke(this)
                send()
            }
        }.getOrElse { cause ->
            throw ResourceReadException("$path: Request failed", cause)
        }

        private fun XMLHttpRequest.isSuccessful() = status in 200..299

        fun exists(): Boolean = runCatching {
            request(method = "HEAD").isSuccessful()
        }.getOrDefault(false)

        fun readText(charset: Charset): String {
            val bytes = readBytes()
            return bytes.decodeWith(charset)
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
                throw ResourceReadException("$path: Read failed (status=${request.status})")
            }
        }

        private fun ByteArray.decodeWith(charset: Charset): String = when (charset) {
            Charset.UTF_8 -> decodeWithTextDecoder("utf-8")
            Charset.UTF_16 -> decodeUtf16()
            Charset.UTF_16BE -> decodeWithTextDecoder("utf-16be")
            Charset.UTF_16LE -> decodeWithTextDecoder("utf-16le")
            // TextDecoder doesn't support iso-8859-1 directly. We use windows-1252, which is a
            // superset: it matches iso-8859-1 for 0x00-0x7F and 0xA0-0xFF, differing only in 0x80-0x9F.
            Charset.ISO_8859_1 -> decodeWithTextDecoder("windows-1252")
            Charset.US_ASCII -> decodeAscii()
        }

        private fun ByteArray.decodeWithTextDecoder(encoding: String): String {
            val decoder = TextDecoder(encoding)
            return decoder.decode(toUint8Array())
        }

        private fun ByteArray.toUint8Array(): Uint8Array {
            val array = Uint8Array(size)
            val dynamicArray = array.asDynamic()
            for (i in indices) {
                dynamicArray[i] = this[i].toInt() and 0xFF
            }
            return array
        }
    }

    /**
     * Resource access via Node.js fs module (for Node.js environments).
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

        fun readText(charset: Charset): String {
            val nodeEncoding = charset.toNodeEncoding()
            return if (nodeEncoding != null) {
                runCatching {
                    fs.readFileSync(path, nodeEncoding) as String
                }.getOrElse { cause ->
                    throw ResourceReadException("$path: Read failed", cause)
                }
            } else {
                // Node doesn't support this encoding natively, decode manually.
                val bytes = runCatching {
                    val buffer = fs.readFileSync(path).unsafeCast<Uint8Array>()
                    Int8Array(buffer.buffer, buffer.byteOffset, buffer.length).unsafeCast<ByteArray>()
                }.getOrElse { cause ->
                    throw ResourceReadException("$path: Read failed", cause)
                }
                when (charset) {
                    Charset.UTF_16 -> bytes.decodeUtf16()
                    Charset.UTF_16BE -> bytes.decodeUtf16Be()
                    else -> throw IllegalArgumentException("Unsupported charset: $charset")
                }
            }
        }

        fun readBytes(): ByteArray = runCatching {
            val buffer = fs.readFileSync(path).unsafeCast<Uint8Array>()
            Int8Array(buffer.buffer, buffer.byteOffset, buffer.length).unsafeCast<ByteArray>()
        }.getOrElse { cause ->
            throw ResourceReadException("$path: Read failed", cause)
        }
    }
}

private fun Charset.toNodeEncoding(): String? = when (this) {
    Charset.UTF_8 -> "utf8"
    Charset.UTF_16LE -> "utf16le"
    Charset.ISO_8859_1 -> "latin1"
    Charset.US_ASCII -> "ascii"
    // Node doesn't support UTF-16 with BOM or UTF-16BE natively.
    Charset.UTF_16 -> null
    Charset.UTF_16BE -> null
}
