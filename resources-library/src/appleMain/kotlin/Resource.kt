package com.goncalossilva.resources

import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.value
import platform.Foundation.NSASCIIStringEncoding
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSDataReadingUncached
import platform.Foundation.NSError
import platform.Foundation.NSISOLatin1StringEncoding
import platform.Foundation.NSString
import platform.Foundation.NSStringEncoding
import platform.Foundation.NSUTF16LittleEndianStringEncoding
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.stringWithContentsOfFile

@OptIn(UnsafeNumber::class)
public actual class Resource actual constructor(private val path: String) {
    private val absolutePath = NSBundle.mainBundle.pathForResource(
        path.substringBeforeLast("."),
        path.substringAfterLast(".")
    )

    public actual fun exists(): Boolean = absolutePath != null

    public actual fun readText(charset: Charset): String = memScoped {
        if (absolutePath == null) {
            throw FileReadException("$path: No such file or directory")
        }
        val error = alloc<ObjCObjectVar<NSError?>>()
        NSString.stringWithContentsOfFile(absolutePath, charset.toNSStringEncoding(), error.ptr)
            ?: throw FileReadException("$path: Read failed: ${error.value}")
    }

    private fun Charset.toNSStringEncoding(): NSStringEncoding = when (this) {
        Charset.UTF_8 -> NSUTF8StringEncoding
        Charset.UTF_16LE -> NSUTF16LittleEndianStringEncoding
        Charset.ISO_8859_1 -> NSISOLatin1StringEncoding
        Charset.US_ASCII -> NSASCIIStringEncoding
    }

    public actual fun readBytes(): ByteArray = memScoped {
        if (absolutePath == null) {
            throw FileReadException("$path: No such file or directory")
        }
        val error = alloc<ObjCObjectVar<NSError?>>()
        val data = NSData.dataWithContentsOfFile(absolutePath, NSDataReadingUncached, error.ptr)
        val bytes = data?.bytes ?: throw FileReadException("$path: Read failed: ${error.value}")
        bytes.readBytes(data.length.toInt())
    }
}
