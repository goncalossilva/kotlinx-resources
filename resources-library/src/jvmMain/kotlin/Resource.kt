package com.goncalossilva.resources

import java.io.File

public actual class Resource actual constructor(path: String) {
    private val file = File(path)

    public actual fun exists(): Boolean = file.exists()

    public actual fun readText(): String =
        runCatching { file.readText() }.getOrElse { throw RuntimeException(it) }
}
