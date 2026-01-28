package com.goncalossilva.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidResourceTest {
    @Test
    fun commonTestResourceExists() {
        assertTrue(Resource("302.json").exists())
        assertEquals("{}", Resource("302.json").readText().trim())
    }

    @Test
    fun platformResourceOverride() {
        assertTrue(Resource("platform_resource.txt").exists())
        assertEquals("android", Resource("platform_resource.txt").readText().trim())
    }
}
