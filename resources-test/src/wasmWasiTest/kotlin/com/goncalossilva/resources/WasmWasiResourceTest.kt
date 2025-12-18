package com.goncalossilva.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WasmWasiResourceTest {

    @Test
    fun platformResourceOverload() {
        assertTrue(Resource("platform_resource.txt").exists())
        assertEquals("wasmwasi", Resource("platform_resource.txt").readText())
    }
}
