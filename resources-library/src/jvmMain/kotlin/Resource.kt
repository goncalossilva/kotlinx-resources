package com.goncalossilva.resources

import java.io.File

public actual class Resource actual constructor(private val path: String) {

    private val resourcePath: String?
        get() = Resource::class.java.classLoader.getResource(path)?.path

    private val resourceFile: File
        get() = File(resourcePath)

    public actual fun exists(): Boolean = resourcePath != null

    public actual fun readText(): String = runCatching {
        resourceFile.readText()
    }.getOrElse { cause ->
        throw FileReadException("$path: No such file or directory", cause)
    }

    public actual fun readBytes(): ByteArray = runCatching {
        resourceFile.readBytes()
    }.getOrElse { cause ->
        throw FileReadException("$path: No such file or directory", cause)
    }
}
