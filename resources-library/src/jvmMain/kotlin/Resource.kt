package com.goncalossilva.resources

import java.io.File

public actual class Resource actual constructor(private val path: String) {

    private val resourcePath: String?
        get() = Resource::class.java.classLoader.getResource(path)?.path

    private val resourceFile: File
        get() = File(resourcePath)

    public actual fun exists(): Boolean = resourcePath != null

    public actual fun readText(charset: Charset): String = runCatching {
        resourceFile.readText(charset.toKotlinCharset())
    }.getOrElse { cause ->
        throw ResourceReadException("$path: No such file or directory", cause)
    }

    public actual fun readBytes(): ByteArray = runCatching {
        resourceFile.readBytes()
    }.getOrElse { cause ->
        throw ResourceReadException("$path: No such file or directory", cause)
    }
}

private fun Charset.toKotlinCharset(): java.nio.charset.Charset = when (this) {
    Charset.UTF_8 -> kotlin.text.Charsets.UTF_8
    Charset.UTF_16 -> kotlin.text.Charsets.UTF_16
    Charset.UTF_16BE -> kotlin.text.Charsets.UTF_16BE
    Charset.UTF_16LE -> kotlin.text.Charsets.UTF_16LE
    Charset.ISO_8859_1 -> kotlin.text.Charsets.ISO_8859_1
    Charset.US_ASCII -> kotlin.text.Charsets.US_ASCII
}
