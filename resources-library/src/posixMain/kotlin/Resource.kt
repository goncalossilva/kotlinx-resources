package com.goncalossilva.resources

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import platform.posix.F_OK
import platform.posix.access
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.posix_errno
import platform.posix.strerror

public actual class Resource actual constructor(private val path: String) {
    public actual fun exists(): Boolean = access(path, F_OK) != -1

    public actual fun readText(charset: Charset): String {
        val bytes = readBytes()
        return bytes.decodeWith(charset)
    }

    public actual fun readBytes(): ByteArray = mutableListOf<Byte>().apply {
        val file = fopen(path, "rb")
            ?: throw ResourceReadException("$path: Open failed: ${strerror(posix_errno())}")
        try {
            memScoped {
                val buffer = allocArray<ByteVar>(BUFFER_SIZE)
                do {
                    val size = fread(buffer, 1u, BUFFER_SIZE.toULong(), file)
                    addAll(buffer.readBytes(size.toInt()).asIterable())
                } while (size > 0u)
            }
        } finally {
            fclose(file)
        }
    }.toByteArray()

    private companion object {
        private const val BUFFER_SIZE = 8 * 1024
    }
}

private fun ByteArray.decodeWith(charset: Charset): String = when (charset) {
    Charset.UTF_8 -> decodeToString()
    Charset.UTF_16 -> decodeUtf16()
    Charset.UTF_16BE -> decodeUtf16Be()
    Charset.UTF_16LE -> decodeUtf16Le()
    Charset.ISO_8859_1 -> {
        val chars = CharArray(size)
        for (i in indices) {
            chars[i] = (this[i].toInt() and 0xFF).toChar()
        }
        chars.concatToString()
    }
    Charset.US_ASCII -> decodeAscii()
}
