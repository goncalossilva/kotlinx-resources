package com.goncalossilva.resources

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException

public actual class Resource actual constructor(private val path: String) {

    public actual fun exists(): Boolean {
        return tryInstrumentedTest { context ->
            try {
                context.assets.open(path).close()
                true
            } catch (_: IOException) {
                false
            }
        } ?: (classloaderResourcePath != null)
    }

    public actual fun readText(): String {
        return tryInstrumentedTest { context ->
            runCatching {
                context.assets.open(path).bufferedReader().use { it.readText() }
            }.getOrElse { cause ->
                throw FileReadException("$path: No such file or directory", cause)
            }
        } ?: runCatching {
            File(classloaderResourcePath!!).readText()
        }.getOrElse { cause ->
            throw FileReadException("$path: No such file or directory", cause)
        }
    }

    public actual fun readBytes(): ByteArray {
        return tryInstrumentedTest { context ->
            runCatching {
                context.assets.open(path).use { it.readBytes() }
            }.getOrElse { cause ->
                throw FileReadException("$path: No such file or directory", cause)
            }
        } ?: runCatching {
            File(classloaderResourcePath!!).readBytes()
        }.getOrElse { cause ->
            throw FileReadException("$path: No such file or directory", cause)
        }
    }

    private val classloaderResourcePath: String?
        get() = Resource::class.java.classLoader?.getResource(path)?.path

    private inline fun <T> tryInstrumentedTest(block: (android.content.Context) -> T): T? {
        return try {
            block(InstrumentationRegistry.getInstrumentation().context)
        } catch (_: IllegalStateException) {
            // Not running in instrumented test environment
            null
        }
    }
}
