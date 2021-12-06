package com.goncalossilva.resource

import java.io.File

actual public class Resource actual constructor(path: String) {
    private val file = File(path)

    actual public fun exists(): Boolean = file.exists()

    actual public fun readText(): String =
        runCatching { file.readText() }.getOrElse { throw RuntimeException(it) }
}
