package com.goncalossilva.resource

import __dirname
import __filename
import process

actual public class Resource actual constructor(private val path: String) {
    actual public fun exists(): Boolean = fs.existsSync(path)

    actual public fun readText(): String =
        runCatching { fs.readFileSync(path, "utf8") }.getOrElse { throw RuntimeException(it) }
}
