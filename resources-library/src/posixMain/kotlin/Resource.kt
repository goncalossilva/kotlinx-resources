package com.goncalossilva.resources

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.F_OK
import platform.posix.access
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.posix_errno
import platform.posix.strerror

public actual class Resource actual constructor(private val path: String) {
    public actual fun exists(): Boolean = access(path, F_OK) != -1

    public actual fun readText(): String = buildString {
        val file = fopen(path, "r")
            ?: throw FileReadException("$path: Open failed: ${strerror(posix_errno())}")
        try {
            memScoped {
                val buffer = allocArray<ByteVar>(BUFFER_SIZE)
                do {
                    val line = fgets(buffer, BUFFER_SIZE, file)?.also { append(it.toKString()) }
                } while (line != null)
            }
        } finally {
            fclose(file)
        }
    }

    private companion object {
        private const val BUFFER_SIZE = 8 * 1024
    }
}
