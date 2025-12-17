package com.goncalossilva.resources

import androidx.test.platform.app.InstrumentationRegistry
import android.content.Context
import java.io.IOException

public actual class Resource actual constructor(private val path: String) {

    public actual fun exists(): Boolean {
        val existsInAssets = instrumentedContextOrNull()
            ?.let { assetsEntryExists(it, path) }
            ?: false
        val existsInClassLoader = classLoaderResourceExists(path)
        return existsInAssets || existsInClassLoader
    }

    public actual fun readText(): String {
        var assetFailure: IOException? = null
        val fromAssets = instrumentedContextOrNull()
            ?.let { readTextFromAssetsOrNull(it, path) { assetFailure = it } }
        if (fromAssets != null) return fromAssets

        readTextFromClassLoaderOrNull(path)?.let { return it }

        throw FileReadException("$path: No such file or directory", assetFailure)
    }

    public actual fun readBytes(): ByteArray {
        var assetFailure: IOException? = null
        val fromAssets = instrumentedContextOrNull()
            ?.let { readBytesFromAssetsOrNull(it, path) { assetFailure = it } }
        if (fromAssets != null) return fromAssets

        readBytesFromClassLoaderOrNull(path)?.let { return it }

        throw FileReadException("$path: No such file or directory", assetFailure)
    }

    private fun instrumentedContextOrNull(): Context? {
        return try {
            InstrumentationRegistry.getInstrumentation().context
        } catch (_: IllegalStateException) {
            null
        } catch (_: NoClassDefFoundError) {
            null
        }
    }

    private fun assetsEntryExists(context: Context, path: String): Boolean {
        val normalizedPath = path.trimStart('/')
        val name = normalizedPath.substringAfterLast('/', missingDelimiterValue = normalizedPath)
        if (name.isBlank()) return false

        val parent = normalizedPath.substringBeforeLast('/', missingDelimiterValue = "")
        return try {
            context.assets.list(parent)?.contains(name) == true
        } catch (_: IOException) {
            false
        }
    }

    private fun classLoaderResourceExists(path: String): Boolean {
        return Resource::class.java.classLoader?.getResource(path) != null
    }

    private fun readTextFromAssetsOrNull(
        context: Context,
        path: String,
        onFailure: (IOException) -> Unit
    ): String? {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (cause: IOException) {
            onFailure(cause)
            null
        }
    }

    private fun readTextFromClassLoaderOrNull(path: String): String? {
        val stream = Resource::class.java.classLoader?.getResourceAsStream(path) ?: return null
        return stream.bufferedReader().use { it.readText() }
    }

    private fun readBytesFromAssetsOrNull(
        context: Context,
        path: String,
        onFailure: (IOException) -> Unit
    ): ByteArray? {
        return try {
            context.assets.open(path).use { it.readBytes() }
        } catch (cause: IOException) {
            onFailure(cause)
            null
        }
    }

    private fun readBytesFromClassLoaderOrNull(path: String): ByteArray? {
        val stream = Resource::class.java.classLoader?.getResourceAsStream(path) ?: return null
        return stream.use { it.readBytes() }
    }
}
