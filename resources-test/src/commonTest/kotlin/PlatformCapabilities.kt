package com.goncalossilva.resources

/**
 * Returns whether this platform can reliably read raw bytes for files that start with UTF-16 BOM bytes.
 *
 * Some browser+tooling stacks can corrupt leading BOM bytes when using a synchronous XHR-based binary read
 * (via `responseText`), which makes UTF-16 BOM detection unreliable.
 */
internal expect fun canReadUtf16BomBytes(): Boolean
