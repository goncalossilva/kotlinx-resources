package com.goncalossilva.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WasmJsResourceTest {

    @Test
    fun platformResourceOverload() {
        assertTrue(Resource("platform_resource.txt").exists())
        assertEquals("wasmjs", Resource("platform_resource.txt").readText())
    }
}

