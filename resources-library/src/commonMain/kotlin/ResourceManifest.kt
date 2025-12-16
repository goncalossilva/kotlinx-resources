package com.goncalossilva.resources

/**
 * Internal manifest that tracks all resource files and provides
 * directory listing and lookup capabilities.
 */
internal class ResourceManifest(private val files: List<String>) {
    // Set of all file paths for O(1) lookup
    private val fileSet: Set<String> = files.toSet()

    // Set of all directory paths (inferred from file paths)
    private val directorySet: Set<String> by lazy {
        files.flatMap { path ->
            generateSequence(path.substringBeforeLast('/')) { parent ->
                if (parent.contains('/')) parent.substringBeforeLast('/') else null
            }
        }.toSet()
    }

    /**
     * Lists immediate children of the given directory path.
     * Empty path represents the root directory.
     */
    fun listDirectory(path: String): List<ResourceEntry> {
        val prefix = if (path.isEmpty()) "" else "$path/"
        val children = mutableSetOf<String>()

        for (file in files) {
            if (path.isEmpty() || file.startsWith(prefix)) {
                val relativePath = if (path.isEmpty()) file else file.removePrefix(prefix)
                val childName = relativePath.substringBefore('/')
                if (childName.isNotEmpty()) {
                    children.add(childName)
                }
            }
        }

        return children.sorted().map { childName ->
            val childPath = if (path.isEmpty()) childName else "$path/$childName"
            if (isFile(childPath)) {
                ResourceFile(childPath)
            } else {
                ResourceDirectory(childPath, this)
            }
        }
    }

    /**
     * Returns true if the path refers to a file.
     */
    fun isFile(path: String): Boolean = path in fileSet

    /**
     * Returns true if the path refers to a directory.
     */
    fun isDirectory(path: String): Boolean = path in directorySet || path.isEmpty()

    /**
     * Returns all file paths.
     */
    fun allFiles(): List<String> = files

    /**
     * Returns a ResourceEntry for the given path, or null if not found.
     */
    fun get(path: String): ResourceEntry? = when {
        path.isEmpty() -> ResourceDirectory("", this)
        isFile(path) -> ResourceFile(path)
        isDirectory(path) -> ResourceDirectory(path, this)
        else -> null
    }

    companion object {
        /**
         * Manifest filename used by the plugin.
         */
        const val FILENAME = "__resources__.json"

        /**
         * Parses the manifest JSON content (simple array of strings).
         */
        fun parse(json: String): ResourceManifest {
            val files = parseJsonArray(json)
            return ResourceManifest(files)
        }

        /**
         * Creates an empty manifest.
         */
        fun empty(): ResourceManifest = ResourceManifest(emptyList())

        /**
         * Simple JSON array parser that handles array of strings.
         * No external dependencies required.
         */
        private fun parseJsonArray(json: String): List<String> {
            val trimmed = json.trim()
            if (trimmed.isEmpty() || trimmed == "[]") return emptyList()
            if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()

            val content = trimmed.drop(1).dropLast(1).trim()
            if (content.isEmpty()) return emptyList()

            return content.split(",").mapNotNull { entry ->
                val trimmedEntry = entry.trim()
                if (trimmedEntry.startsWith("\"") && trimmedEntry.endsWith("\"")) {
                    trimmedEntry.drop(1).dropLast(1)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .replace("\\/", "/")
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\t", "\t")
                } else {
                    null
                }
            }.filter { it.isNotEmpty() }
        }
    }
}

/**
 * Exception thrown when resource discovery fails.
 */
public class ResourceDiscoveryException(message: String) : RuntimeException(message)

/**
 * Platform-specific function to load the resource manifest.
 */
internal expect fun loadManifest(): ResourceManifest
