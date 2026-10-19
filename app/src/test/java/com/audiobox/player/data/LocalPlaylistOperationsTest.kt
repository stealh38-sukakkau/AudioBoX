package com.audiobox.player.data

import com.audiobox.player.model.LocalPlaylist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPlaylistOperationsTest {
    @Test
    fun createTrimsNameAndRejectsBlankOrDuplicateNames() {
        val created = LocalPlaylistOperations.create(emptyList(), "  Road Trip  ", "playlist-1")

        assertEquals(LocalPlaylist("playlist-1", "Road Trip", emptyList()), created?.single())
        assertNull(LocalPlaylistOperations.create(created.orEmpty(), "road trip", "playlist-2"))
        assertNull(LocalPlaylistOperations.create(created.orEmpty(), "   ", "playlist-2"))
    }

    @Test
    fun renameAndDeleteOnlyChangePlaylistRecords() {
        val playlist = LocalPlaylist("playlist-1", "Favorites", listOf("mediastore_1", "jellyfin_item-2"))
        val renamed = LocalPlaylistOperations.rename(listOf(playlist), playlist.id, " Road Mix ")

        assertEquals("Road Mix", renamed?.single()?.name)
        assertEquals(playlist.songIds, renamed?.single()?.songIds)
        assertNull(LocalPlaylistOperations.rename(renamed.orEmpty(), "missing", "Other"))
        assertFalse(LocalPlaylistOperations.delete(renamed.orEmpty(), playlist.id).any { it.id == playlist.id })
    }

    @Test
    fun addingIsIdempotentAndRemovalKeepsPlaylistAndOtherSongs() {
        val playlist = LocalPlaylist("playlist-1", "Favorites", listOf("local-1", "jellyfin_2"))
        val added = LocalPlaylistOperations.addSong(listOf(playlist), playlist.id, "jellyfin_2")
        val updated = LocalPlaylistOperations.addSong(added, playlist.id, "local-2")
        val removed = LocalPlaylistOperations.removeSong(updated, playlist.id, "local-1")

        assertEquals(listOf("local-1", "jellyfin_2"), added.single().songIds)
        assertEquals(listOf("local-1", "jellyfin_2", "local-2"), updated.single().songIds)
        assertEquals(listOf("jellyfin_2", "local-2"), removed.single().songIds)
        assertTrue(removed.single().id == playlist.id)
    }
}
