package com.goncalossilva.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResourcesTest {
    @Test
    fun listReturnsAllFiles() {
        val files = Resources.list()
        assertTrue(files.isNotEmpty())
        // Verify some expected files exist
        assertTrue(files.contains("302.json"))
        assertTrue(files.contains("a/302.json"))
        assertTrue(files.contains("testdir/root.txt"))
    }

    @Test
    fun rootReturnsDirectory() {
        val root = Resources.root()
        assertEquals("", root.path)
        assertTrue(root.list().isNotEmpty())
    }

    @Test
    fun rootListContainsTopLevelEntries() {
        val entries = Resources.root().list()
        val names = entries.map { it.name }
        assertTrue(names.contains("302.json"))
        assertTrue(names.contains("a"))
        assertTrue(names.contains("testdir"))
    }

    @Test
    fun getFileReturnsResourceFile() {
        val entry = Resources.get("302.json")
        assertNotNull(entry)
        assertTrue(entry is ResourceFile)
        assertEquals("302.json", entry.path)
        assertEquals("302.json", entry.name)
    }

    @Test
    fun getDirectoryReturnsResourceDirectory() {
        val entry = Resources.get("a")
        assertNotNull(entry)
        assertTrue(entry is ResourceDirectory)
        assertEquals("a", entry.path)
        assertEquals("a", entry.name)
    }

    @Test
    fun getNestedFileReturnsResourceFile() {
        val entry = Resources.get("testdir/subdir1/file1.txt")
        assertNotNull(entry)
        assertTrue(entry is ResourceFile)
        assertEquals("testdir/subdir1/file1.txt", entry.path)
        assertEquals("file1.txt", entry.name)
    }

    @Test
    fun getNestedDirectoryReturnsResourceDirectory() {
        val entry = Resources.get("testdir/subdir2/nested")
        assertNotNull(entry)
        assertTrue(entry is ResourceDirectory)
        assertEquals("testdir/subdir2/nested", entry.path)
        assertEquals("nested", entry.name)
    }

    @Test
    fun getNonExistentReturnsNull() {
        assertNull(Resources.get("does/not/exist.txt"))
        assertNull(Resources.get("nonexistent"))
    }

    @Test
    fun getEmptyPathReturnsRootDirectory() {
        val entry = Resources.get("")
        assertNotNull(entry)
        assertTrue(entry is ResourceDirectory)
        assertEquals("", entry.path)
    }

    @Test
    fun directoryListReturnsImmediateChildren() {
        val dir = Resources.get("testdir") as? ResourceDirectory
        assertNotNull(dir)
        val entries = dir.list()
        val names = entries.map { it.name }
        assertTrue(names.contains("root.txt"))
        assertTrue(names.contains("subdir1"))
        assertTrue(names.contains("subdir2"))
        assertEquals(3, names.size)
    }

    @Test
    fun directoryListFilesReturnsOnlyFiles() {
        val dir = Resources.get("testdir") as? ResourceDirectory
        assertNotNull(dir)
        val files = dir.listFiles()
        assertEquals(1, files.size)
        assertEquals("root.txt", files[0].name)
    }

    @Test
    fun directoryListDirectoriesReturnsOnlyDirectories() {
        val dir = Resources.get("testdir") as? ResourceDirectory
        assertNotNull(dir)
        val dirs = dir.listDirectories()
        assertEquals(2, dirs.size)
        val names = dirs.map { it.name }
        assertTrue(names.contains("subdir1"))
        assertTrue(names.contains("subdir2"))
    }

    @Test
    fun nestedDirectoryList() {
        val dir = Resources.get("testdir/subdir1") as? ResourceDirectory
        assertNotNull(dir)
        val entries = dir.list()
        val names = entries.map { it.name }
        assertTrue(names.contains("file1.txt"))
        assertTrue(names.contains("file2.json"))
        assertEquals(2, names.size)
    }

    @Test
    fun deeplyNestedDirectoryList() {
        val dir = Resources.get("testdir/subdir2/nested") as? ResourceDirectory
        assertNotNull(dir)
        val files = dir.listFiles()
        assertEquals(1, files.size)
        assertEquals("deep.txt", files[0].name)
    }

    @Test
    fun findByExtensionReturnsMatchingFiles() {
        val jsonFiles = Resources.findByExtension("json")
        assertTrue(jsonFiles.isNotEmpty())
        assertTrue(jsonFiles.all { it.path.endsWith(".json") })
        assertTrue(jsonFiles.any { it.path == "302.json" })
        assertTrue(jsonFiles.any { it.path == "testdir/subdir1/file2.json" })
    }

    @Test
    fun findByExtensionWithNoMatchesReturnsEmpty() {
        val files = Resources.findByExtension("xyz123")
        assertTrue(files.isEmpty())
    }

    @Test
    fun findWithPredicateReturnsMatchingEntries() {
        val txtFiles = Resources.find { it is ResourceFile && it.path.endsWith(".txt") }
        assertTrue(txtFiles.isNotEmpty())
        assertTrue(txtFiles.all { it is ResourceFile && it.path.endsWith(".txt") })
    }

    @Test
    fun findWithAlwaysFalsePredicateReturnsEmpty() {
        val entries = Resources.find { false }
        assertTrue(entries.isEmpty())
    }

    @Test
    fun resourceFileReadText() {
        val file = Resources.get("testdir/root.txt") as? ResourceFile
        assertNotNull(file)
        assertEquals("root file content\n", file.readText())
    }

    @Test
    fun resourceFileReadTextNested() {
        val file = Resources.get("testdir/subdir2/nested/deep.txt") as? ResourceFile
        assertNotNull(file)
        assertEquals("deep file content\n", file.readText())
    }

    @Test
    fun resourceFileFromListReadText() {
        val dir = Resources.get("testdir/subdir1") as? ResourceDirectory
        assertNotNull(dir)
        val file = dir.listFiles().find { it.name == "file1.txt" }
        assertNotNull(file)
        assertEquals("file1 content\n", file.readText())
    }

    @Test
    fun resourceEntryEquality() {
        val file1 = Resources.get("302.json") as? ResourceFile
        val file2 = ResourceFile("302.json")
        assertEquals(file1, file2)
        assertEquals(file1.hashCode(), file2.hashCode())
    }

    @Test
    fun resourceEntryToString() {
        val file = Resources.get("302.json") as? ResourceFile
        assertNotNull(file)
        assertEquals("ResourceFile(302.json)", file.toString())

        val dir = Resources.get("a") as? ResourceDirectory
        assertNotNull(dir)
        assertEquals("ResourceDirectory(a)", dir.toString())
    }
}
