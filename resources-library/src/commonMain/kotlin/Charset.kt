package com.goncalossilva.resources

/**
 * Represents a character encoding.
 */
public enum class Charset {
    UTF_8,
    UTF_16,
    UTF_16BE,
    UTF_16LE,
    ISO_8859_1,
    US_ASCII
}

/**
 * Provides access to commonly used [Charset] instances.
 *
 * Matches the API of Kotlin's [kotlin.text.Charsets] for familiarity.
 */
public object Charsets {
    /** Eight-bit UCS Transformation Format. */
    public val UTF_8: Charset = Charset.UTF_8

    /** Sixteen-bit UCS Transformation Format, byte order identified by optional BOM. */
    public val UTF_16: Charset = Charset.UTF_16

    /** Sixteen-bit UCS Transformation Format, big-endian byte order. */
    public val UTF_16BE: Charset = Charset.UTF_16BE

    /** Sixteen-bit UCS Transformation Format, little-endian byte order. */
    public val UTF_16LE: Charset = Charset.UTF_16LE

    /** ISO Latin Alphabet No. 1 (ISO-LATIN-1). */
    public val ISO_8859_1: Charset = Charset.ISO_8859_1

    /** Seven-bit ASCII (ISO646-US). */
    public val US_ASCII: Charset = Charset.US_ASCII
}
