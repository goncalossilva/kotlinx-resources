@file:OptIn(ExperimentalWasmJsInterop::class)

package com.goncalossilva.resources

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.toJsString

/*
 * It's impossible to separate browser/node JS runtimes, as they can't be published separately.
 * See: https://youtrack.jetbrains.com/issue/KT-47038
 */
public actual class Resource actual constructor(private val path: String) {
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
    private class ResourceBrowser(path: String) {
        private val jsPath: JsString = path.toJsString()
        private val jsBrowserPath: JsString = "/base/${path.removePrefix("/")}".toJsString()
        private val errorPrefix: String = path

        fun exists(): Boolean = runCatching {
            request(method = "HEAD").isSuccessful()
        }.getOrDefault(false)

        fun readText(charset: Charset): String {
            val bytes = readBytes()
            return bytes.decodeWith(charset)
        }

        fun readBytes(): ByteArray {
            val request = request {
                overrideMimeType("text/plain; charset=x-user-defined".toJsString())
            }
            return if (request.isSuccessful()) {
                val response = request.responseText
                val length = jsStringLength(response)
                ByteArray(length) { (jsCharCodeAt(response, it) and 0xFF).toByte() }
            } else {
                throw ResourceReadException("$errorPrefix: Read failed (status=${request.status})")
            }
        }

        private fun request(
            method: String = "GET",
            config: (XMLHttpRequest.() -> Unit)? = null,
        ): XMLHttpRequest = runCatching {
            createXMLHttpRequest().apply {
                open(method.toJsString(), jsBrowserPath, false)
                config?.invoke(this)
                send()
            }
        }.getOrElse { cause ->
            throw ResourceReadException("$errorPrefix: Request failed", cause)
        }

        private fun XMLHttpRequest.isSuccessful() = status in 200..299

        private fun ByteArray.decodeWith(charset: Charset): String = when (charset) {
            Charset.UTF_8 -> decodeWithTextDecoder("utf-8")
            Charset.UTF_16 -> decodeUtf16()
            Charset.UTF_16BE -> decodeWithTextDecoder("utf-16be")
            Charset.UTF_16LE -> decodeWithTextDecoder("utf-16le")
            // TextDecoder doesn't support iso-8859-1 directly. windows-1252 is a superset:
            // 0x00-0x7F and 0xA0-0xFF map identically, and only 0x80-0x9F differ (control vs printable).
            Charset.ISO_8859_1 -> decodeWithTextDecoder("windows-1252")
            Charset.US_ASCII -> decodeAscii()
        }

        private fun ByteArray.decodeWithTextDecoder(encoding: String): String {
            val decoder = createTextDecoder(encoding.toJsString())
            return decoder.decode(toUint8Array()).toString()
        }
    }

    /**
     * Resource access via Node.js fs module (for Node.js environments).
     */
    private class ResourceNode(path: String) {
        private val jsPath: JsString = path.toJsString()
        private val errorPrefix: String = path

        fun exists(): Boolean = nodeExistsSync(jsPath)

        fun readText(charset: Charset): String {
            val nodeEncoding = charset.toNodeEncoding()
            return if (nodeEncoding != null) {
                runCatching {
                    nodeReadFileSync(jsPath, nodeEncoding.toJsString()).toString()
                }.getOrElse { cause ->
                    throw ResourceReadException("$errorPrefix: Read failed", cause)
                }
            } else {
                // Node doesn't support this encoding natively, decode manually.
                val bytes = runCatching {
                    nodeReadFileSyncBytes(jsPath).toByteArray()
                }.getOrElse { cause ->
                    throw ResourceReadException("$errorPrefix: Read failed", cause)
                }
                when (charset) {
                    Charset.UTF_16 -> bytes.decodeUtf16()
                    Charset.UTF_16BE -> bytes.decodeUtf16Be()
                    else -> throw IllegalArgumentException("Unsupported charset: $charset")
                }
            }
        }

        fun readBytes(): ByteArray = runCatching {
            nodeReadFileSyncBytes(jsPath).toByteArray()
        }.getOrElse { cause ->
            throw ResourceReadException("$errorPrefix: Read failed", cause)
        }

        private fun NodeBuffer.toByteArray(): ByteArray = ByteArray(length) { this[it] }
    }
}

private val IS_BROWSER: Boolean = js(IS_BROWSER_JS_CHECK)

private val IS_NODE: Boolean = js(IS_NODE_JS_CHECK)

private fun nodeExistsSync(path: JsString): Boolean = js("require('fs').existsSync(path)")

private fun nodeReadFileSync(path: JsString, encoding: JsString): JsString =
    js("require('fs').readFileSync(path, encoding)")

private fun nodeReadFileSyncBytes(path: JsString): NodeBuffer =
    js("require('fs').readFileSync(path)")

private external class NodeBuffer : JsAny {
    val length: Int
    operator fun get(index: Int): Byte
}

private external class XMLHttpRequest : JsAny {
    fun open(method: JsString, url: JsString, async: Boolean)
    fun send()
    fun overrideMimeType(mimeType: JsString)
    val status: Int
    val statusText: JsString
    val responseText: JsString
}

private fun createXMLHttpRequest(): XMLHttpRequest = js("new XMLHttpRequest()")

private external class TextDecoder : JsAny {
    fun decode(input: Uint8Array): JsString
}

private fun createTextDecoder(encoding: JsString): TextDecoder = js("new TextDecoder(encoding)")

private external class Uint8Array : JsAny {
    operator fun set(index: Int, value: Int)
}

private fun createUint8Array(length: Int): Uint8Array = js("new Uint8Array(length)")

private fun jsStringLength(str: JsString): Int = js("str.length")

private fun jsCharCodeAt(str: JsString, index: Int): Int = js("str.charCodeAt(index)")

private fun ByteArray.toUint8Array(): Uint8Array {
    val array = createUint8Array(size)
    for (i in indices) {
        array[i] = this[i].toInt() and 0xFF
    }
    return array
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
