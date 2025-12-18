package com.goncalossilva.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppleResourceTest {

    @Test
    fun platformResourceOverride() {
        assertTrue(Resource("platform_resource.txt").exists())
        assertEquals("apple", Resource("platform_resource.txt").readText())
    }
}