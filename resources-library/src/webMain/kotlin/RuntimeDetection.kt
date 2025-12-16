package com.goncalossilva.resources

internal const val IS_BROWSER_JS_CHECK: String =
    "typeof window !== 'undefined' && typeof window.document !== 'undefined' || " +
        "typeof self !== 'undefined' && typeof self.location !== 'undefined'"

internal const val IS_NODE_JS_CHECK: String =
    "typeof process !== 'undefined' && process.versions != null && process.versions.node != null"
