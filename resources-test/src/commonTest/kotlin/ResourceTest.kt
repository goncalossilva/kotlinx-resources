package com.goncalossilva.resources

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResourceTest {
    @Test
    fun existsRoot() {
        assertTrue(Resource("src/commonTest/resources/302.json").exists())
    }

    @Test
    fun existsNested() {
        assertTrue(Resource("src/commonTest/resources/a/302.json").exists())
        assertTrue(Resource("src/commonTest/resources/a/folder/302.json").exists())
    }

    @Test
    fun doesNotExistRoot() {
        assertFalse(Resource("src/commonTest/resources/404.json").exists())
    }

    @Test
    fun doesNotExistNested() {
        assertFalse(Resource("src/commonTest/resources/a/404.json").exists())
        assertFalse(Resource("src/commonTest/resources/a/folder/404.json").exists())
    }

    @Test
    fun readTextRoot() {
        assertEquals(JSON, Resource("src/commonTest/resources/302.json").readText())
    }

    @Test
    fun readTextNested() {
        assertEquals(JSON, Resource("src/commonTest/resources/a/302.json").readText())
        assertEquals(JSON, Resource("src/commonTest/resources/a/folder/302.json").readText())
    }

    @Test
    fun readTextRootThrowsWhenNotFound() {
        assertFailsWith(FileReadException::class) {
            Resource("src/commonTest/resources/404.json").readText()
        }
    }

    @Test
    fun readTextNestedThrowsWhenNotFound() {
        assertFailsWith(FileReadException::class) {
            Resource("src/commonTest/resources/a/404.json").readText()
        }
        assertFailsWith(FileReadException::class) {
            Resource("src/commonTest/resources/a/folder/404.json").readText()
        }
    }

    @Test
    fun readBytesRoot() {
        assertContentEquals(GZIP, Resource("src/commonTest/resources/302.gz").readBytes())
    }

    @Test
    fun readBytesNested() {
        assertContentEquals(GZIP, Resource("src/commonTest/resources/a/302.gz").readBytes())
        assertContentEquals(GZIP, Resource("src/commonTest/resources/a/folder/302.gz").readBytes())
    }

    @Test
    fun readBytesRootThrowsWhenNotFound() {
        assertFailsWith(FileReadException::class) {
            Resource("src/commonTest/resources/404.gz").readBytes()
        }
    }

    @Test
    fun readBytesNestedThrowsWhenNotFound() {
        assertFailsWith(FileReadException::class) {
            Resource("src/commonTest/resources/a/404.gz").readBytes()
        }
        assertFailsWith(FileReadException::class) {
            Resource("src/commonTest/resources/a/folder/404.gz").readBytes()
        }
    }

    companion object {
        const val JSON: String = "{}\n"
        val GZIP: ByteArray = byteArrayOf(
            31, -117, 8, 0, -82, -122, -31, 91, 2, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0
        )
    }
}
