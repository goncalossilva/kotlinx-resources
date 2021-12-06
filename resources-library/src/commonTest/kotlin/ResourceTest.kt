package com.goncalossilva.resource

import kotlin.test.Test
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
        assertEquals("{}\n", Resource("src/commonTest/resources/302.json").readText())
    }

    @Test
    fun readTextNested() {
        assertEquals("{}\n", Resource("src/commonTest/resources/a/302.json").readText())
        assertEquals("{}\n", Resource("src/commonTest/resources/a/folder/302.json").readText())
    }

    @Test
    fun readTextRootThrowsWhenNotFound() {
        assertFailsWith(RuntimeException::class) {
            Resource("src/commonTest/resources/404.json").readText()
        }
    }

    @Test
    fun readTextNestedThrowsWhenNotFound() {
        assertFailsWith(RuntimeException::class) {
            Resource("src/commonTest/resources/a/404.json").readText()
        }
        assertFailsWith(RuntimeException::class) {
            Resource("src/commonTest/resources/a/folder/404.json").readText()
        }
    }
}
