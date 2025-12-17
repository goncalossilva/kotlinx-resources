package com.goncalossilva.resources

/**
 * Decodes this byte array as ASCII.
 * Each byte is masked to 7 bits to ensure valid ASCII range.
 */
internal fun ByteArray.decodeAscii(): String {
    val chars = CharArray(size)
    for (i in indices) {
        chars[i] = (this[i].toInt() and 0x7F).toChar()
    }
    return chars.concatToString()
}

/**
 * Decodes this byte array as UTF-16 big-endian.
 */
internal fun ByteArray.decodeUtf16Be(): String {
    require(size % 2 == 0) { "UTF-16 data must have even number of bytes, got $size" }
    val chars = CharArray(size / 2)
    for (i in chars.indices) {
        val hi = this[i * 2].toInt() and 0xFF
        val lo = this[i * 2 + 1].toInt() and 0xFF
        chars[i] = ((hi shl 8) or lo).toChar()
    }
    return chars.concatToString()
}

/**
 * Decodes this byte array as UTF-16 little-endian.
 */
internal fun ByteArray.decodeUtf16Le(): String {
    require(size % 2 == 0) { "UTF-16 data must have even number of bytes, got $size" }
    val chars = CharArray(size / 2)
    for (i in chars.indices) {
        val lo = this[i * 2].toInt() and 0xFF
        val hi = this[i * 2 + 1].toInt() and 0xFF
        chars[i] = ((hi shl 8) or lo).toChar()
    }
    return chars.concatToString()
}

/**
 * Decodes this byte array as UTF-16 with BOM detection.
 * If BOM is present, uses it to determine byte order.
 * If no BOM, defaults to big-endian per the Unicode standard.
 */
internal fun ByteArray.decodeUtf16(): String {
    require(size % 2 == 0) { "UTF-16 data must have even number of bytes, got $size" }
    if (size < 2) return ""

    // Check for BOM.
    val first = this[0].toInt() and 0xFF
    val second = this[1].toInt() and 0xFF

    return when {
        first == 0xFE && second == 0xFF -> {
            // Big-endian BOM, skip it.
            sliceArray(2 until size).decodeUtf16Be()
        }
        first == 0xFF && second == 0xFE -> {
            // Little-endian BOM, skip it.
            sliceArray(2 until size).decodeUtf16Le()
        }
        else -> {
            // No BOM, default to big-endian.
            decodeUtf16Be()
        }
    }
}

