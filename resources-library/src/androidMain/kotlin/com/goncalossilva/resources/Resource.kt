package com.goncalossilva.resources

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException

/**
 * Android resource implementation supporting two execution contexts:
 * - Device/instrumented tests: Resources are packaged as assets and read via [ResourceAssets]
 * - Unit tests: Resources are on the classpath and read via [ResourceClassLoader]
 *
 * Assets are tried first (when running in an instrumentation context), falling back to ClassLoader.
 */
public actual class Resource actual constructor(private val path: String) {
    /**
     * Normalized path with leading '/' stripped.
     *
     * Required because [android.content.res.AssetManager] does not accept paths with leading
     * slashes, and [ClassLoader.getResource] on Android behaves inconsistently with them.
     * This differs from JVM where ClassLoader typically handles leading slashes gracefully.
     */
    private val normalizedPath = path.trimStart('/')

    private val resourceAssets: ResourceAssets? by lazy {
        instrumentedContextOrNull()?.let { ResourceAssets(it, normalizedPath) }
    }

    private val resourceClassLoader: ResourceClassLoader by lazy {
        ResourceClassLoader(normalizedPath)
    }

    public actual fun exists(): Boolean {
        return resourceAssets?.exists() == true || resourceClassLoader.exists()
    }

    public actual fun readText(charset: Charset): String {
        val kotlinCharset = charset.toKotlinCharset()

        resourceAssets?.readTextOrNull(kotlinCharset)?.let { return it }
        resourceClassLoader.readTextOrNull(kotlinCharset)?.let { return it }

        throw ResourceReadException("$path: No such file or directory")
    }

    public actual fun readBytes(): ByteArray {
        resourceAssets?.readBytesOrNull()?.let { return it }
        resourceClassLoader.readBytesOrNull()?.let { return it }

        throw ResourceReadException("$path: No such file or directory")
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

    /**
     * Resource access via Android assets (for device/instrumented tests).
     */
    private class ResourceAssets(
        private val context: Context,
        private val path: String
    ) {
        fun exists(): Boolean {
            val name = path.substringAfterLast('/', missingDelimiterValue = path)
            if (name.isBlank()) return false

            val parent = path.substringBeforeLast('/', missingDelimiterValue = "")
            return try {
                context.assets.list(parent)?.contains(name) == true
            } catch (_: IOException) {
                false
            }
        }

        fun readTextOrNull(charset: java.nio.charset.Charset): String? {
            return try {
                context.assets.open(path).bufferedReader(charset).use { it.readText() }
            } catch (_: IOException) {
                null
            }
        }

        fun readBytesOrNull(): ByteArray? {
            return try {
                context.assets.open(path).use { it.readBytes() }
            } catch (_: IOException) {
                null
            }
        }
    }

    /**
     * Resource access via classloader (for unit tests).
     */
    private class ResourceClassLoader(private val path: String) {
        private val classLoader: ClassLoader?
            get() = Resource::class.java.classLoader

        fun exists(): Boolean {
            return classLoader?.getResource(path) != null
        }

        fun readTextOrNull(charset: java.nio.charset.Charset): String? {
            return try {
                val stream = classLoader?.getResourceAsStream(path) ?: return null
                stream.bufferedReader(charset).use { it.readText() }
            } catch (_: IOException) {
                null
            }
        }

        fun readBytesOrNull(): ByteArray? {
            return try {
                val stream = classLoader?.getResourceAsStream(path) ?: return null
                stream.use { it.readBytes() }
            } catch (_: IOException) {
                null
            }
        }
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
