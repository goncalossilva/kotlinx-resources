package com.goncalossilva.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmResourceTest {

    @Test
    fun platformResourceOverride() {
        assertTrue(Resource("platform_resource.txt").exists())
        assertEquals("jvm", Resource("platform_resource.txt").readText())
    }

}