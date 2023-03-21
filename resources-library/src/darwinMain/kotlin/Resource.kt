package com.goncalossilva.resources

import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSBundle
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

public actual class Resource actual constructor(private val path: String) {
    private val absolutePath = NSBundle.mainBundle.pathForResource(
        path.substringBeforeLast("."),
        path.substringAfterLast(".")
    )

    public actual fun exists(): Boolean = absolutePath != null

    public actual fun readText(): String = memScoped {
        if (absolutePath == null) {
            throw FileReadException("$path: No such file or directory")
        }
        val error = alloc<ObjCObjectVar<NSError?>>()
        NSString.stringWithContentsOfFile(absolutePath, NSUTF8StringEncoding, error.ptr)
            ?: throw FileReadException("$path: Read failed: ${error.value}")
    }
}
