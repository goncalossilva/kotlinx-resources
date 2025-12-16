package com.goncalossilva.resources

/**
 * Represents an entry in the resources directory structure.
 * Can be either a [ResourceFile] or a [ResourceDirectory].
 */
public sealed interface ResourceEntry {
    /**
     * Path relative to resources root, using forward slashes as separators.
     */
    public val path: String

    /**
     * Name of this entry (last component of path).
     */
    public val name: String
        get() = path.substringAfterLast('/')
}

/**
 * A file resource that can be read.
 */
public class ResourceFile(override val path: String) : ResourceEntry {
    /**
     * Returns the file's content as a UTF-8 string.
     *
     * @throws FileReadException when the resource can't be read.
     */
    public fun readText(): String = Resource(path).readText()

    /**
     * Returns the file's content as a byte array.
     *
     * @throws FileReadException when the resource can't be read.
     */
    public fun readBytes(): ByteArray = Resource(path).readBytes()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResourceFile) return false
        return path == other.path
    }

    override fun hashCode(): Int = path.hashCode()

    override fun toString(): String = "ResourceFile($path)"
}

/**
 * A directory containing other resources.
 */
public class ResourceDirectory internal constructor(
    override val path: String,
    private val manifest: ResourceManifest
) : ResourceEntry {
    /**
     * Lists immediate children (files and directories).
     */
    public fun list(): List<ResourceEntry> = manifest.listDirectory(path)

    /**
     * Lists immediate file children only.
     */
    public fun listFiles(): List<ResourceFile> =
        list().filterIsInstance<ResourceFile>()

    /**
     * Lists immediate directory children only.
     */
    public fun listDirectories(): List<ResourceDirectory> =
        list().filterIsInstance<ResourceDirectory>()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResourceDirectory) return false
        return path == other.path
    }

    override fun hashCode(): Int = path.hashCode()

    override fun toString(): String = "ResourceDirectory($path)"
}
