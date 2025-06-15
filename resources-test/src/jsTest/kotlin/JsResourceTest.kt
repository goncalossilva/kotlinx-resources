package com.goncalossilva.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsResourceTest {

    @Test
    fun platformResourceOverload() {
        assertTrue(Resource("platform_resource.txt").exists())
        assertEquals("js", Resource("platform_resource.txt").readText())
    }
}