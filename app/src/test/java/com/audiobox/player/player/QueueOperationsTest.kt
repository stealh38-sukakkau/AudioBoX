package com.audiobox.player.player

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueOperationsTest {
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
}
