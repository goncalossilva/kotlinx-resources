package com.goncalossilva.resources.test

import com.goncalossilva.resources.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidResourceTest {
    @Test
    fun existsRoot() {
        assertTrue(Resource("resource.txt").exists())
    }

    @Test
    fun existsNested() {
        assertTrue(Resource("nested/resource.txt").exists())
    }

    @Test
    fun doesNotExist() {
        assertFalse(Resource("nonexistent.txt").exists())
    }

    @Test
    fun readTextRoot() {
        assertEquals("root", Resource("resource.txt").readText().trim())
    }

    @Test
    fun readTextNested() {
        assertEquals("nested", Resource("nested/resource.txt").readText().trim())
    }

    @Test
    fun readBytesRoot() {
        val bytes = Resource("resource.txt").readBytes()
        assertEquals("root", bytes.decodeToString().trim())
    }

    @Test
    fun readBytesNested() {
        val bytes = Resource("nested/resource.txt").readBytes()
        assertEquals("nested", bytes.decodeToString().trim())
    }
}
