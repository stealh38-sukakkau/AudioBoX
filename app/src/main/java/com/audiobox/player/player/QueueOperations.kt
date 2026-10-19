package com.audiobox.player.player

/** Pure queue ordering helpers used by the active Media3 controller and unit tests. */
internal object QueueOperations {
    data class Insertion<T>(val items: List<T>, val index: Int)

    fun <T> insertAfterCurrent(items: List<T>, currentIndex: Int, item: T): Insertion<T> {
        val index = if (items.isEmpty()) 0 else {
            (currentIndex.takeIf { it in items.indices } ?: 0) + 1
        }
        return Insertion(items.toMutableList().apply { add(index, item) }, index)
    }

    fun <T> append(items: List<T>, item: T): Insertion<T> =
        Insertion(items + item, items.size)
}
