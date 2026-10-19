package com.audiobox.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFileSizeFilterTest {
    @Test
    fun defaultThresholdIsFourMib() {
        assertEquals(4 * 1024L * 1024L, AudioFileSizeFilter.minimumBytes(AudioFileSizeFilter.DEFAULT_MINIMUM_MIB))
    }

    @Test
    fun includesFilesAtThresholdAndExcludesSmallerFiles() {
        val threshold = AudioFileSizeFilter.minimumBytes(4)

        assertFalse(AudioFileSizeFilter.includes(threshold - 1L, threshold))
        assertTrue(AudioFileSizeFilter.includes(threshold, threshold))
        assertTrue(AudioFileSizeFilter.includes(threshold + 1L, threshold))
    }

    @Test
    fun zeroDisablesFilteringAndNegativeValuesAreClampedToZero() {
        assertEquals(0L, AudioFileSizeFilter.minimumBytes(0))
        assertEquals(0L, AudioFileSizeFilter.minimumBytes(-1))
        assertTrue(AudioFileSizeFilter.includes(0L, AudioFileSizeFilter.minimumBytes(0)))
    }
}
