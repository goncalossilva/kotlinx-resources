package com.goncalossilva.resource

import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

actual public class Resource actual constructor(path: String) {
    private val absolutePath = NSBundle.mainBundle.pathForResource(
        path.substringBeforeLast("."),
        path.substringAfterLast(".")
    )

    actual public fun exists(): Boolean = absolutePath != null

    actual public fun readText(): String = if (absolutePath != null) {
        NSString.stringWithContentsOfFile(absolutePath, NSUTF8StringEncoding, null)!!
    } else {
        throw RuntimeException()
    }
}
