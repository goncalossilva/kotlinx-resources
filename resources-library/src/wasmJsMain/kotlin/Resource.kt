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

private external class NodeBuffer : JsAny {
    val length: Int
    operator fun get(index: Int): Byte
}

private fun nodeReadFileSyncBytes(path: JsString): NodeBuffer = js("require('fs').readFileSync(path)")

private fun NodeBuffer.toByteArray(): ByteArray = ByteArray(length) { this[it] }

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
    public actual fun exists(): Boolean = when {
        IS_BROWSER -> existsBrowser()
        IS_NODE -> existsNode()
        else -> throw UnsupportedOperationException("Unsupported Wasm/JS runtime")
    }

    public actual fun readText(): String = when {
        IS_BROWSER -> readTextBrowser()
        IS_NODE -> readTextNode()
        else -> throw UnsupportedOperationException("Unsupported Wasm/JS runtime")
    }

    public actual fun readBytes(): ByteArray = when {
        IS_BROWSER -> readBytesBrowser()
        IS_NODE -> readBytesNode()
        else -> throw UnsupportedOperationException("Unsupported Wasm/JS runtime")
    }

    private fun createRequest(config: (XMLHttpRequest.() -> Unit)? = null) = createXMLHttpRequest().apply {
        open("GET".toJsString(), path.toJsString(), false)
        config?.invoke(this)
        send()
    }

    @Suppress("MagicNumber")
    private fun XMLHttpRequest.isSuccessful() = status in 200..299

    private fun existsBrowser(): Boolean = createRequest().isSuccessful()

    private fun readTextBrowser(): String {
        val request = createRequest()
        return if (request.isSuccessful()) {
            request.responseText.toString()
        } else {
            throw FileReadException("$path: Read failed: ${request.statusText}")
        }
    }

    private fun readBytesBrowser(): ByteArray {
        val request = createRequest {
            overrideMimeType("text/plain; charset=x-user-defined".toJsString())
        }
        return if (request.isSuccessful()) {
            val response = request.responseText.toString()
            ByteArray(response.length) { response[it].code.toUByte().toByte() }
        } else {
            throw FileReadException("$path: Read failed: ${request.statusText}")
        }
    }

    private fun existsNode(): Boolean = nodeExistsSync(path.toJsString())

    private fun readTextNode(): String = runCatching {
        nodeReadFileSync(path.toJsString(), "utf8".toJsString()).toString()
    }.getOrElse { cause ->
        throw FileReadException("$path: Read failed", cause)
    }

    private fun readBytesNode(): ByteArray = runCatching {
        nodeReadFileSyncBytes(path.toJsString()).toByteArray()
    }.getOrElse { cause ->
        throw FileReadException("$path: Read failed", cause)
    }
}
