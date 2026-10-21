package com.audiobox.player.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueOperationsTest {
    private data class Track(val id: String, val source: String)

    @Test
    fun libraryQueueInitializationOnlyAppliesToAnEmptyMediaSession() {
        assertTrue(QueueOperations.shouldInitializeLibraryQueue(0))
        assertFalse(QueueOperations.shouldInitializeLibraryQueue(1))
        assertFalse(QueueOperations.shouldInitializeLibraryQueue(4))
    }

    @Test
    fun playNextInsertsImmediatelyAfterCurrentAndPreservesRemainingOrder() {
        val queue = listOf("current", "later-1", "later-2")

        val result = QueueOperations.insertAfterCurrent(queue, currentIndex = 0, item = "next")

        assertEquals(1, result.index)
        assertEquals(listOf("current", "next", "later-1", "later-2"), result.items)
        assertEquals(queue, listOf("current", "later-1", "later-2"))
    }

    @Test
    fun playNextWithInvalidIndexUsesFrontOfQueueWithoutReorderingExistingItems() {
        val result = QueueOperations.insertAfterCurrent(listOf("first", "second"), currentIndex = -1, item = "next")

        assertEquals(1, result.index)
        assertEquals(listOf("first", "next", "second"), result.items)
    }

    @Test
    fun addToQueueAppendsWithoutMovingExistingItems() {
        val result = QueueOperations.append(listOf("current", "next", "later"), "queued")

        assertEquals(3, result.index)
        assertEquals(listOf("current", "next", "later", "queued"), result.items)
    }

    @Test
    fun jellyfinSelectionQueuesAllAvailableJellyfinTracksInCatalogOrder() {
        val local = Track("local-1", "LOCAL")
        val first = Track("jf-1", "JELLYFIN")
        val selected = Track("jf-2", "JELLYFIN")
        val last = Track("jf-3", "JELLYFIN")

        val result = QueueOperations.sourceQueueForSelection(
            candidates = listOf(local, first, selected, last),
            selected = selected,
            idOf = Track::id,
            sourceOf = Track::source
        )

        assertEquals(listOf(first, selected, last), result.items)
        assertEquals(1, result.index)
    }

    @Test
    fun exhaustedJellyfinCatalogKeepsOnlySelectedJellyfinTrackAndNeverFallsBackToLocal() {
        val local = Track("local-1", "LOCAL")
        val selected = Track("jf-only", "JELLYFIN")

        val result = QueueOperations.sourceQueueForSelection(
            candidates = listOf(local),
            selected = selected,
            idOf = Track::id,
            sourceOf = Track::source
        )

        assertEquals(listOf(selected), result.items)
        assertEquals(0, result.index)
    }

    @Test
    fun localSelectionDoesNotCarryTracksFromJellyfinQueue() {
        val selected = Track("local-2", "LOCAL")
        val jellyfin = Track("jf-1", "JELLYFIN")
        val local = Track("local-1", "LOCAL")

        val result = QueueOperations.sourceQueueForSelection(
            candidates = listOf(jellyfin, local, selected),
            selected = selected,
            idOf = Track::id,
            sourceOf = Track::source
        )

        assertEquals(listOf(local, selected), result.items)
        assertEquals(1, result.index)
    }
}
