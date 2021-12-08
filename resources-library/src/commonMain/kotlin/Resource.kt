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
     * Returns the resource's content as a string.
     *
     * @throws RuntimeException when the resource doesn't exist or can't be read.
     */
    public fun readText(): String
}
