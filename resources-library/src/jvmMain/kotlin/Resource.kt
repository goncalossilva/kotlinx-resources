package com.goncalossilva.resources

import java.io.File

public actual class Resource actual constructor(private val path: String) {
    private val file = File(path)

    public actual fun exists(): Boolean = file.exists()

    public actual fun readText(): String = runCatching {
        file.readText()
    }.getOrElse { cause ->
        throw FileReadException("$path: No such file or directory", cause)
    }

    public actual fun readBytes(): ByteArray = runCatching {
        file.readBytes()
    }.getOrElse { cause ->
        throw FileReadException("$path: No such file or directory", cause)
    }
}
