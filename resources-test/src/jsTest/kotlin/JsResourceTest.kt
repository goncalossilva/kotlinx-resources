package com.goncalossilva.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsResourceTest {

    @Test
    fun platformResourceOverride() {
        assertTrue(Resource("platform_resource.txt").exists())
        assertEquals("js", Resource("platform_resource.txt").readText())
    }

    @Test
    fun readsResourceWithSpaces() {
        assertEquals("hello", Resource("file with spaces.txt").readText().trim())
    }
}
