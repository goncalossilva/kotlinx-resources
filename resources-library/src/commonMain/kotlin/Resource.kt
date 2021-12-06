package com.goncalossilva.resource

expect public class Resource(path: String) {
    public fun exists(): Boolean
    public fun readText(): String
}
