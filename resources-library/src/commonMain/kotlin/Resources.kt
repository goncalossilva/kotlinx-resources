package com.goncalossilva.resources

/**
 * Entry point for discovering and traversing resources.
 *
 * Unlike [Resource] which accesses individual files by path, this object
 * provides directory listing and traversal capabilities.
 *
 * Example usage:
 * ```kotlin
 * // List all resources at root
 * val entries = Resources.root().list()
 *
 * // Get a specific directory and list its files
 * val dir = Resources.get("my/directory") as? ResourceDirectory
 * dir?.listFiles()?.forEach { file ->
 *     println(file.readText())
 * }
 *
 * // Find all JSON files
 * val jsonFiles = Resources.findByExtension("json")
 * ```
 */
public expect object Resources {
    /**
     * Returns the root directory containing all resources.
     */
    public fun root(): ResourceDirectory

    /**
     * Returns a [ResourceEntry] at the given path, or null if not found.
     *
     * @param path Path relative to resources root, using forward slashes.
     *             Empty string returns the root directory.
     */
    public fun get(path: String): ResourceEntry?

    /**
     * Returns all entries matching the given predicate.
     *
     * @param predicate Function to test each entry.
     */
    public fun find(predicate: (ResourceEntry) -> Boolean): List<ResourceEntry>

    /**
     * Returns all files with the given extension.
     *
     * @param extension File extension without the dot (e.g., "json", "txt").
     */
    public fun findByExtension(extension: String): List<ResourceFile>

    /**
     * Lists all resource file paths.
     *
     * @return Sorted list of all file paths.
     */
    public fun list(): List<String>
}
