package com.goncalossilva.resources

import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

public actual class Resource actual constructor(path: String) {
    private val absolutePath = NSBundle.mainBundle.pathForResource(
        path.substringBeforeLast("."),
        path.substringAfterLast(".")
    )

    public actual fun exists(): Boolean = absolutePath != null

    public actual fun readText(): String = if (absolutePath != null) {
        NSString.stringWithContentsOfFile(absolutePath, NSUTF8StringEncoding, null)!!
    } else {
        throw RuntimeException()
    }
}
