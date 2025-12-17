package com.goncalossilva.resources

import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException

public actual class Resource actual constructor(private val path: String) {

    public actual fun exists(): Boolean {
        val existsInAssets = tryInstrumentedTest { context ->
            try {
                context.assets.open(path).close()
                true
            } catch (_: IOException) {
                false
            }
        }

        val existsInClassLoader = Resource::class.java.classLoader?.getResource(path) != null

        return when (existsInAssets) {
            null -> existsInClassLoader
            else -> existsInAssets || existsInClassLoader
        }
    }

    public actual fun readText(): String {
        var assetFailure: IOException? = null
        val fromAssets = tryInstrumentedTest { context ->
            try {
                context.assets.open(path).bufferedReader().use { it.readText() }
            } catch (cause: IOException) {
                assetFailure = cause
                null
            }
        }
        if (fromAssets != null) return fromAssets

        val stream = Resource::class.java.classLoader?.getResourceAsStream(path)
        if (stream != null) {
            return stream.bufferedReader().use { it.readText() }
        }

        throw FileReadException("$path: No such file or directory", assetFailure)
    }

    public actual fun readBytes(): ByteArray {
        var assetFailure: IOException? = null
        val fromAssets = tryInstrumentedTest { context ->
            try {
                context.assets.open(path).use { it.readBytes() }
            } catch (cause: IOException) {
                assetFailure = cause
                null
            }
        }
        if (fromAssets != null) return fromAssets

        val stream = Resource::class.java.classLoader?.getResourceAsStream(path)
        if (stream != null) {
            return stream.use { it.readBytes() }
        }

        throw FileReadException("$path: No such file or directory", assetFailure)
    }

    private inline fun <T> tryInstrumentedTest(block: (android.content.Context) -> T): T? {
        return try {
            block(InstrumentationRegistry.getInstrumentation().context)
        } catch (_: IllegalStateException) {
            // Not running in instrumented test environment
            null
        }
    }
}
