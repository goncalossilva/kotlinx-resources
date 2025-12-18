package com.goncalossilva.resources

import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.MemoryAllocator
import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.withScopedMemoryAllocator

// WASI Preview 1 types
private typealias Fd = Int
private typealias Size = Int
private typealias Errno = Int

// WASI Preview 1 constants
private const val ERRNO_SUCCESS: Errno = 0

// Filetype constants from WASI filestat
private const val FILETYPE_REGULAR_FILE: Byte = 4

// File descriptor flags for path_open
private const val OFLAGS_NONE: Short = 0

// Rights needed for reading files
private const val RIGHTS_FD_READ: Long = 1L shl 1
private const val RIGHTS_FD_SEEK: Long = 1L shl 2

// Pre-opened directory file descriptor (typically starts at 3).
private const val PREOPENED_FD: Fd = 3

// WASI Preview 1 imports
@WasmImport("wasi_snapshot_preview1", "path_open")
private external fun wasiPathOpen(
    fd: Fd,
    dirflags: Int,
    path: Int,
    pathLen: Size,
    oflags: Short,
    fsRightsBase: Long,
    fsRightsInheriting: Long,
    fdflags: Short,
    resultFd: Int
): Errno

@WasmImport("wasi_snapshot_preview1", "fd_read")
private external fun wasiFdRead(
    fd: Fd,
    iovs: Int,
    iovsLen: Size,
    resultNread: Int
): Errno

@WasmImport("wasi_snapshot_preview1", "fd_close")
private external fun wasiFdClose(fd: Fd): Errno

@WasmImport("wasi_snapshot_preview1", "path_filestat_get")
private external fun wasiPathFilestatGet(
    fd: Fd,
    flags: Int,
    path: Int,
    pathLen: Size,
    resultBuf: Int
): Errno

// Buffer size balances memory usage with I/O efficiency for typical resource files.
private const val BUFFER_SIZE = 8 * 1024

public actual class Resource actual constructor(private val path: String) {
    public actual fun exists(): Boolean = withScopedMemoryAllocator { allocator ->
        val pathBytes = path.encodeToByteArray()
        val pathPtr = allocator.writeBytes(pathBytes)
        val filestatBuf = allocator.allocate(64) // filestat is 64 bytes

        val errno = wasiPathFilestatGet(
            PREOPENED_FD,
            0, // flags: no symlink follow
            pathPtr.address.toInt(),
            pathBytes.size,
            filestatBuf.address.toInt()
        )
        if (errno != ERRNO_SUCCESS) return@withScopedMemoryAllocator false

        // WASI filestat struct layout: dev(u64):0, ino(u64):8, filetype(u8):16, nlink(u64):24, size(u64):32.
        val filetype = (filestatBuf + 16).loadByte()
        filetype == FILETYPE_REGULAR_FILE
    }

    public actual fun readText(charset: Charset): String {
        val bytes = readBytes()
        return when (charset) {
            Charset.UTF_8 -> bytes.decodeToString()
            Charset.UTF_16 -> bytes.decodeUtf16()
            Charset.UTF_16BE -> bytes.decodeUtf16Be()
            Charset.UTF_16LE -> bytes.decodeUtf16Le()
            Charset.ISO_8859_1 -> bytes.decodeIso8859()
            Charset.US_ASCII -> bytes.decodeAscii()
        }
    }

    public actual fun readBytes(): ByteArray = withScopedMemoryAllocator { allocator ->
        val fd = openFile(allocator, path)
        try {
            readFileContent(allocator, fd)
        } finally {
            wasiFdClose(fd)
        }
    }

    private fun openFile(allocator: MemoryAllocator, filePath: String): Fd {
        val pathBytes = filePath.encodeToByteArray()
        val pathPtr = allocator.writeBytes(pathBytes)
        val resultFdPtr = allocator.allocate(4)

        val errno = wasiPathOpen(
            PREOPENED_FD,
            0, // dirflags: no symlink follow
            pathPtr.address.toInt(),
            pathBytes.size,
            OFLAGS_NONE,
            RIGHTS_FD_READ or RIGHTS_FD_SEEK,
            0L, // no inheriting rights
            0, // fdflags
            resultFdPtr.address.toInt()
        )

        if (errno != ERRNO_SUCCESS) {
            throw ResourceReadException("$filePath: Open failed (errno=$errno)")
        }

        return resultFdPtr.loadInt()
    }

    private fun readFileContent(allocator: MemoryAllocator, fd: Fd): ByteArray {
        val iovec = allocator.allocate(8) // iovec struct: buf pointer (4) + buf_len (4)
        val nreadPtr = allocator.allocate(4)
        val chunks = mutableListOf<ByteArray>()
        val buffer = allocator.allocate(BUFFER_SIZE)
        iovec.storeInt(buffer.address.toInt())
        (iovec + 4).storeInt(BUFFER_SIZE)

        while (true) {
            val errno = wasiFdRead(
                fd,
                iovec.address.toInt(),
                1,
                nreadPtr.address.toInt()
            )
            if (errno != ERRNO_SUCCESS) {
                throw ResourceReadException("$path: Read failed (errno=$errno)")
            }
            val nread = nreadPtr.loadInt()
            if (nread == 0) break

            val chunk = ByteArray(nread)
            for (i in 0 until nread) {
                chunk[i] = (buffer + i).loadByte()
            }
            chunks.add(chunk)
        }

        val totalSize = chunks.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(result, offset)
            offset += chunk.size
        }
        return result
    }

    private fun MemoryAllocator.writeBytes(bytes: ByteArray): Pointer {
        val ptr = allocate(bytes.size)
        for (i in bytes.indices) {
            (ptr + i).storeByte(bytes[i])
        }
        return ptr
    }
}
