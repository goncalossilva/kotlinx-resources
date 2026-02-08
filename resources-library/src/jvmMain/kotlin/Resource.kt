package com.goncalossilva.resources

public actual class Resource actual constructor(private val path: String) {

    public actual fun exists(): Boolean =
        Resource::class.java.classLoader.getResource(path) != null

    public actual fun readText(charset: Charset): String = runCatching {
        Resource::class.java.classLoader.getResourceAsStream(path)!!
            .use { it.readBytes().toString(charset.toJavaCharset()) }
    }.getOrElse { cause ->
        throw ResourceReadException("$path: No such file or directory", cause)
    }

    public actual fun readBytes(): ByteArray = runCatching {
        Resource::class.java.classLoader.getResourceAsStream(path)!!
            .use { it.readBytes() }
    }.getOrElse { cause ->
        throw ResourceReadException("$path: No such file or directory", cause)
    }
}

private fun Charset.toJavaCharset(): java.nio.charset.Charset = when (this) {
    Charset.UTF_8 -> kotlin.text.Charsets.UTF_8
    Charset.UTF_16 -> kotlin.text.Charsets.UTF_16
    Charset.UTF_16BE -> kotlin.text.Charsets.UTF_16BE
    Charset.UTF_16LE -> kotlin.text.Charsets.UTF_16LE
    Charset.ISO_8859_1 -> kotlin.text.Charsets.ISO_8859_1
    Charset.US_ASCII -> kotlin.text.Charsets.US_ASCII
}
