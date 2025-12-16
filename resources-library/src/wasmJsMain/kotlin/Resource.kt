@file:OptIn(ExperimentalWasmJsInterop::class)

package com.goncalossilva.resources

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.toJsString

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

    private class ResourceBrowser(path: String) {
        private val jsPath: JsString = path.toJsString()
        private val errorPrefix: String = path

        fun exists(): Boolean = request().isSuccessful()

        fun readText(): String {
            val request = request()
            return if (request.isSuccessful()) {
                request.responseText.toString()
            } else {
                throw FileReadException("$errorPrefix: Read failed: ${request.statusText.toString()}")
            }
        }

        fun readBytes(): ByteArray {
            val request = request {
                overrideMimeType("text/plain; charset=x-user-defined".toJsString())
            }
            return if (request.isSuccessful()) {
                val response = request.responseText.toString()
                ByteArray(response.length) { response[it].code.toUByte().toByte() }
            } else {
                throw FileReadException("$errorPrefix: Read failed: ${request.statusText.toString()}")
            }
        }

        private fun request(config: (XMLHttpRequest.() -> Unit)? = null): XMLHttpRequest = createXMLHttpRequest().apply {
            open("GET".toJsString(), jsPath, false)
            config?.invoke(this)
            send()
        }

        @Suppress("MagicNumber")
        private fun XMLHttpRequest.isSuccessful() = status in 200..299
    }

    private class ResourceNode(path: String) {
        private val jsPath: JsString = path.toJsString()
        private val errorPrefix: String = path

        fun exists(): Boolean = nodeExistsSync(jsPath)

        fun readText(): String = runCatching {
            nodeReadFileSync(jsPath, "utf8".toJsString()).toString()
        }.getOrElse { cause ->
            throw FileReadException("$errorPrefix: Read failed", cause)
        }

        fun readBytes(): ByteArray = runCatching {
            nodeReadFileSyncBytes(jsPath).toByteArray()
        }.getOrElse { cause ->
            throw FileReadException("$errorPrefix: Read failed", cause)
        }

        private fun NodeBuffer.toByteArray(): ByteArray = ByteArray(length) { this[it] }
    }
}
