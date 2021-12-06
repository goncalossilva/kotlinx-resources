package com.goncalossilva.resource

import org.w3c.xhr.XMLHttpRequest

actual public class Resource actual constructor(path: String) {
    private val request: XMLHttpRequest by lazy {
        XMLHttpRequest().apply {
            open("GET", path, false)
            send()
        }
    }

    actual public fun exists(): Boolean = request.status in 200..299

    actual public fun readText(): String = if (exists()) {
        request.responseText
    } else {
        throw RuntimeException()
    }
}
