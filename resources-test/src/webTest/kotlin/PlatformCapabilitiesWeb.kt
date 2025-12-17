package com.goncalossilva.resources

private val canReadUtf16BomBytesValue: Boolean by lazy {
    runCatching {
        val bytes = Resource("charset-utf16-bom-le.txt").readBytes()
        bytes.size >= 2 &&
            (bytes[0].toInt() and 0xFF) == 0xFF &&
            (bytes[1].toInt() and 0xFF) == 0xFE
    }.getOrDefault(false)
}

internal actual fun canReadUtf16BomBytes(): Boolean = canReadUtf16BomBytesValue

