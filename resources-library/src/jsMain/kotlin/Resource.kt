package com.goncalossilva.resources

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
        private val request = XMLHttpRequest().apply {
            open("GET", path, false)
            send()
        }

        @Suppress("MagicNumber")
        fun exists(): Boolean = request.status in 200..299

        fun readText(): String = if (exists()) {
            request.responseText
        } else {
            throw FileReadException("$path: No such file or directory")
        }
    }

    /*
     * Node-based resource implementation.
     */
    private class ResourceNode(val path: String) {
        val fs = require("fs")

        fun exists(): Boolean = fs.existsSync(path) as Boolean

        fun readText(): String = runCatching {
            fs.readFileSync(path, "utf8")
        }.getOrElse { cause ->
            throw FileReadException("$path: No such file or directory", cause)
        } as String
    }
}
