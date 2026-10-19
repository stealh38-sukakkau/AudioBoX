package com.audiobox.player.data

import androidx.compose.ui.graphics.Color
import com.audiobox.player.model.AlbumColorPalette
import com.audiobox.player.model.LyricLine
import com.audiobox.player.model.Song

object DummyMusicDataSource {
    val songs = listOf(
        Song(
            id = "song_1",
            title = "Midnight Horizon",
            artist = "Neon Mirage",
            album = "Cyber Echoes",
            durationMs = 195000L, // 3:15
            artworkUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            colorPalette = AlbumColorPalette(
                primary = Color(0xFF6C3CE9),
                secondary = Color(0xFFE93CA8),
                background = Color(0xFF140D2B)
            ),
            lyrics = listOf(
                LyricLine(0L, "Lights flicker in the midnight rain"),
                LyricLine(5000L, "Reflections of memories we can't sustain"),
                LyricLine(12000L, "The city sleeps under neon haze"),
                LyricLine(19000L, "Lost inside an endless maze"),
                LyricLine(26000L, "Carry me back to where we started"),
                LyricLine(34000L, "Before the dreams had all departed"),
                LyricLine(42000L, "Hear the pulse through the concrete floor"),
                LyricLine(50000L, "Waiting at midnight horizon's door"),
                LyricLine(62000L, "Midnight horizon, calling your name"),
                LyricLine(70000L, "Nothing will ever be the same"),
                LyricLine(85000L, "Electric signals fade away"),
                LyricLine(98000L, "Waiting for the break of day"),
                LyricLine(115000L, "Hold the frequency, don't let go"),
                LyricLine(135000L, "Through the digital vertigo"),
                LyricLine(160000L, "Midnight horizon, glow and fade"),
                LyricLine(180000L, "In the sanctuary that we made")
            )
        ),
        Song(
            id = "song_2",
            title = "Golden Hour Reverie",
            artist = "Sienna Bloom",
            album = "Solitude & Waves",
            durationMs = 210000L, // 3:30
            artworkUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            colorPalette = AlbumColorPalette(
                primary = Color(0xFFE58C33),
                secondary = Color(0xFFBE3455),
                background = Color(0xFF26120D)
            ),
            lyrics = listOf(
                LyricLine(0L, "Sunlight pours across the shore"),
                LyricLine(6000L, "I don't think of you no more"),
                LyricLine(14000L, "Golden amber on my skin"),
                LyricLine(22000L, "Where the ocean breeze begins"),
                LyricLine(31000L, "Time moves slow, the tides align"),
                LyricLine(40000L, "Underneath the warm sunshine"),
                LyricLine(52000L, "Let the reverie take over me"),
                LyricLine(65000L, "Floating out into the sea"),
                LyricLine(80000L, "Waves of amber, skies of gold"),
                LyricLine(100000L, "Stories that were left untold"),
                LyricLine(130000L, "Just a summer fading fast"),
                LyricLine(160000L, "Making every heartbeat last"),
                LyricLine(190000L, "Golden hour, stay a while")
            )
        ),
        Song(
            id = "song_3",
            title = "Velvet Eclipse",
            artist = "Kaelen Ross",
            album = "Astral Shadows",
            durationMs = 175000L, // 2:55
            artworkUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&q=80",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            colorPalette = AlbumColorPalette(
                primary = Color(0xFF1E88E5),
                secondary = Color(0xFF00E676),
                background = Color(0xFF0A1826)
            ),
            lyrics = listOf(
                LyricLine(0L, "Shadows lengthen in the room"),
                LyricLine(8000L, "Silhouettes under the moon"),
                LyricLine(17000L, "Velvet darkness taking hold"),
                LyricLine(25000L, "Whispering secrets never told"),
                LyricLine(36000L, "Catch the orbit, bend the light"),
                LyricLine(47000L, "Drifting in celestial night"),
                LyricLine(60000L, "Eclipse the doubt inside your mind"),
                LyricLine(75000L, "Leave the gravity behind"),
                LyricLine(95000L, "Velvet skies and cobalt dreams"),
                LyricLine(120000L, "Nothing is quite what it seems"),
                LyricLine(150000L, "Until the dawn brings us home")
            )
        )
    )
}
