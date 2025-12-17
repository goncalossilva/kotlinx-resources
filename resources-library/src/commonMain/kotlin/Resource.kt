package com.goncalossilva.resources

/**
 * Provides access to resource on [path].
 *
 * The path should be relative to the project's directory, such as
 * `src/commonTest/resources/some/optional/folders/file.txt`.
 */
public expect class Resource(path: String) {
    /**
     * Returns true when the resource exists, false when it doesn't.
     */
    public fun exists(): Boolean

    /**
     * Returns the resource's content as a string decoded using the specified [charset].
     *
     * @param charset The character encoding to use. Defaults to [Charsets.UTF_8].
     * @throws FileReadException when the resource doesn't exist or can't be read.
     */
    public fun readText(charset: Charset = Charsets.UTF_8): String

    /**
     * Returns the resource's content as a byte array.
     *
     * @throws FileReadException when the resource doesn't exist or can't be read.
     */
    public fun readBytes(): ByteArray
}
