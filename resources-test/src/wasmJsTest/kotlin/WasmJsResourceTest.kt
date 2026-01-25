package com.goncalossilva.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WasmJsResourceTest {

    @Test
    fun platformResourceOverride() {
        assertTrue(Resource("platform_resource.txt").exists())
        assertEquals("wasmjs", Resource("platform_resource.txt").readText())
    }

    @Test
    fun readsResourceWithSpaces() {
        assertEquals("hello", Resource("file with spaces.txt").readText().trim())
    }
}
