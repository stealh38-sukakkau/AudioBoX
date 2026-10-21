package com.audiobox.player.player

/** Pure queue ordering helpers used by the active Media3 controller and unit tests. */
internal object QueueOperations {
    data class Insertion<T>(val items: List<T>, val index: Int)
    data class Selection<T>(val items: List<T>, val index: Int)

    fun shouldInitializeLibraryQueue(mediaItemCount: Int): Boolean = mediaItemCount == 0

    /** Selects a track within its source catalog without carrying items from another source. */
    fun <T, K, S> sourceQueueForSelection(
        candidates: List<T>,
        selected: T,
        idOf: (T) -> K,
        sourceOf: (T) -> S
    ): Selection<T> {
        val selectedId = idOf(selected)
        val sourceItems = candidates.filter { sourceOf(it) == sourceOf(selected) }
        val items = if (sourceItems.any { idOf(it) == selectedId }) sourceItems else listOf(selected)
        return Selection(items, items.indexOfFirst { idOf(it) == selectedId }.coerceAtLeast(0))
    }

    fun <T> insertAfterCurrent(items: List<T>, currentIndex: Int, item: T): Insertion<T> {
        val index = if (items.isEmpty()) 0 else {
            (currentIndex.takeIf { it in items.indices } ?: 0) + 1
        }
        return Insertion(items.toMutableList().apply { add(index, item) }, index)
    }

    fun <T> append(items: List<T>, item: T): Insertion<T> =
        Insertion(items + item, items.size)
}
