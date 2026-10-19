package com.audiobox.player.data

/** Shared MediaStore size threshold. One MiB is exactly 1,048,576 bytes. */
internal object AudioFileSizeFilter {
    const val DEFAULT_MINIMUM_MIB = 4
    const val BYTES_PER_MIB = 1024L * 1024L

    fun minimumBytes(minimumMiB: Int): Long =
        minimumMiB.coerceAtLeast(0).toLong() * BYTES_PER_MIB

    fun includes(fileSizeBytes: Long, minimumBytes: Long): Boolean =
        fileSizeBytes >= minimumBytes.coerceAtLeast(0L)
}
