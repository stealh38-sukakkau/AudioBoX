package com.audiobox.player.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.audiobox.player.model.AudioStreamInfo
import com.audiobox.player.model.LyricLine
import com.audiobox.player.model.LyricsDisplayMode
import com.audiobox.player.model.NowPlayingUiState
import com.audiobox.player.model.RepeatMode
import com.audiobox.player.model.Song
import com.audiobox.player.service.AudioboxMediaService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Controller pemutaran Audiobox yang terhubung ke AudioboxMediaService via AndroidX Media3 MediaController.
 * Mengontrol pemutaran audio riil (streaming & lokal), sinkronisasi posisi audio aktual,
 * inspeksi format audio riil (codec, sample rate, bit depth), dan integrasi USB DAC bit-perfect routing.
 */
class AudioboxPlayerController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(NowPlayingUiState())
    val state: StateFlow<NowPlayingUiState> = _state.asStateFlow()

    private val audioOutputManager = AudioOutputManager(context)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var tickerJob: Job? = null
    private var pendingInitialQueue: List<Song>? = null
    private var pendingInitialSongId: String? = null
    private var pendingPlayWhenControllerReady = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startPositionTracker()
            } else {
                stopPositionTracker()
                updatePositionFromPlayer()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { it.copy(isBuffering = playbackState == Player.STATE_BUFFERING) }
            when (playbackState) {
                Player.STATE_READY -> {
                    _state.update { it.copy(playbackError = null) }
                    updatePositionFromPlayer()
                }
                Player.STATE_ENDED -> {
                    stopPositionTracker()
                    _state.update { it.copy(isPlaying = false, currentPositionMs = it.durationMs, progress = 1f) }
                }
                Player.STATE_BUFFERING, Player.STATE_IDLE -> {
                    // Buffering atau Idle
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val responseCode = generateSequence(error as Throwable?) { it.cause }
                .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
                .firstOrNull()?.responseCode
            val message = when {
                responseCode == 401 -> "Jellyfin session expired. Reconnect to continue."
                responseCode == 404 -> "This Jellyfin track is no longer available."
                responseCode == 403 -> "Jellyfin denied access to this track."
                error.message.orEmpty().contains("session expired", ignoreCase = true) -> "Jellyfin session expired. Reconnect to continue."
                error.message.orEmpty().contains("unavailable", ignoreCase = true) -> "This Jellyfin track is no longer available."
                else -> "Playback failed. Check your network or try this track again."
            }
            _state.update { it.copy(isPlaying = false, isBuffering = false, playbackError = message) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem == null) return
            val song = _state.value.queue.find { it.id == mediaItem.mediaId } ?: return
            _state.update {
                it.copy(
                    currentSong = song,
                    durationMs = song.durationMs,
                    currentPositionMs = 0L,
                    progress = 0f,
                    currentLyricIndex = calculateActiveLyricIndex(song.lyrics, 0L),
                    isFavorite = it.favoriteSongIds.contains(song.id),
                    isLyricsSynced = song.lyrics.isNotEmpty(),
                    isLyricsLoading = false,
                    lyricsError = null,
                    plainLyricsText = null,
                    playbackError = null
                )
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            for (group in tracks.groups) {
                if (group.type == C.TRACK_TYPE_AUDIO && group.isSelected) {
                    for (i in 0 until group.length) {
                        if (group.isTrackSelected(i)) {
                            val format = group.getTrackFormat(i)
                            val rawMime = format.sampleMimeType ?: ""
                            val codec = when {
                                rawMime.contains("flac", ignoreCase = true) -> "FLAC"
                                rawMime.contains("mpeg", ignoreCase = true) || rawMime.contains("mp3", ignoreCase = true) -> "MP3"
                                rawMime.contains("mp4a", ignoreCase = true) || rawMime.contains("aac", ignoreCase = true) -> "AAC"
                                rawMime.contains("opus", ignoreCase = true) -> "OPUS"
                                rawMime.contains("vorbis", ignoreCase = true) -> "VORBIS"
                                rawMime.contains("wav", ignoreCase = true) || rawMime.contains("raw", ignoreCase = true) -> "WAV/PCM"
                                else -> rawMime.substringAfter("/").uppercase().ifBlank { "AUDIO" }
                            }
                            val sampleRate = if (format.sampleRate > 0) format.sampleRate else 44100
                            val channelCount = if (format.channelCount > 0) format.channelCount else 2
                            val bitDepth = when (format.pcmEncoding) {
                                C.ENCODING_PCM_24BIT -> "24-bit"
                                C.ENCODING_PCM_32BIT -> "32-bit"
                                C.ENCODING_PCM_FLOAT -> "32-bit float"
                                else -> if (codec == "FLAC" && sampleRate >= 88200) "24-bit" else "16-bit"
                            }
                            val bitrate = (format.bitrate / 1000).coerceAtLeast(0)

                            val streamInfo = AudioStreamInfo(
                                codecName = codec,
                                sampleRate = sampleRate,
                                bitDepth = bitDepth,
                                channelCount = channelCount,
                                bitrateKbps = bitrate,
                                mimeType = rawMime
                            )

                            _state.update { it.copy(audioStreamInfo = streamInfo) }
                            audioOutputManager.updateStreamInfo(streamInfo)
                            break
                        }
                    }
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            updatePositionFromPlayer()
        }
    }

    init {
        initializeMediaController()

        // Amati status audio output (USB DAC / bit-perfect)
        scope.launch(Dispatchers.Main) {
            audioOutputManager.outputState.collect { outputState ->
                _state.update { it.copy(audioOutputState = outputState) }
            }
        }
    }

    private fun initializeMediaController() {
        val sessionToken = SessionToken(context, ComponentName(context, AudioboxMediaService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future

        future.addListener({
            try {
                val controller = future.get()
                mediaController = controller
                controller.addListener(playerListener)

                pendingInitialQueue?.let { songs ->
                    val shouldStartPlayback = pendingPlayWhenControllerReady
                    setQueueInternal(controller, songs, pendingInitialSongId)
                    pendingInitialQueue = null
                    pendingInitialSongId = null
                    pendingPlayWhenControllerReady = false
                    if (shouldStartPlayback) controller.play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setQueue(songs: List<Song>, initialSongId: String? = null, autoPlay: Boolean = false) {
        val controller = mediaController
        if (controller == null) {
            pendingInitialQueue = songs
            pendingInitialSongId = initialSongId
            pendingPlayWhenControllerReady = autoPlay
            val initialSong = songs.find { it.id == initialSongId } ?: songs.firstOrNull()
            _state.update {
                it.copy(
                    queue = songs,
                    currentSong = initialSong,
                    durationMs = initialSong?.durationMs ?: 0L,
                    currentLyricIndex = calculateActiveLyricIndex(initialSong?.lyrics, 0L)
                )
            }
            return
        }
        setQueueInternal(controller, songs, initialSongId)
        if (autoPlay) controller.play()
    }

    private fun setQueueInternal(controller: MediaController, songs: List<Song>, initialSongId: String?) {
        val targetIndex = if (initialSongId != null) {
            songs.indexOfFirst { it.id == initialSongId }.coerceAtLeast(0)
        } else {
            0
        }
        val initialSong = if (songs.isNotEmpty()) songs[targetIndex] else null

        _state.update {
            it.copy(
                queue = songs,
                currentSong = initialSong,
                durationMs = initialSong?.durationMs ?: 0L,
                currentPositionMs = 0L,
                progress = 0f,
                currentLyricIndex = calculateActiveLyricIndex(initialSong?.lyrics, 0L)
            )
        }

        val mediaItems = songs.map { it.toMediaItem() }
        controller.setMediaItems(mediaItems, targetIndex, 0L)
        controller.prepare()
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val currentSong = _state.value.currentSong ?: return
        val clamped = positionMs.coerceIn(0L, _state.value.durationMs.coerceAtLeast(currentSong.durationMs))
        controller.seekTo(clamped)
        updatePositionFromPlayer()
    }

    fun seekToProgress(progressFraction: Float) {
        val currentSong = _state.value.currentSong ?: return
        val totalDuration = if (_state.value.durationMs > 0L) _state.value.durationMs else currentSong.durationMs
        val targetMs = (totalDuration * progressFraction.coerceIn(0f, 1f)).toLong()
        seekTo(targetMs)
    }

    fun skipNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else {
            if (_state.value.repeatMode == RepeatMode.ALL && _state.value.queue.isNotEmpty()) {
                controller.seekTo(0, 0L)
            }
        }
    }

    fun skipPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newShuffle = !_state.value.isShuffle
        controller.shuffleModeEnabled = newShuffle
        _state.update { it.copy(isShuffle = newShuffle) }
    }

    fun toggleRepeat() {
        val controller = mediaController ?: return
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }

        val exoRepeatMode = when (nextMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        controller.repeatMode = exoRepeatMode
        _state.update { it.copy(repeatMode = nextMode) }
    }

    fun loadSong(song: Song, autoPlay: Boolean = true) {
        val controller = mediaController
        val currentQueue = _state.value.queue

        val indexInQueue = currentQueue.indexOfFirst { it.id == song.id }
        if (controller == null) {
            val queue = if (indexInQueue != -1) currentQueue else currentQueue + song
            pendingInitialQueue = queue
            pendingInitialSongId = song.id
            pendingPlayWhenControllerReady = autoPlay
            _state.update {
                it.copy(
                    queue = queue,
                    currentSong = song,
                    durationMs = song.durationMs,
                    currentPositionMs = 0L,
                    progress = 0f,
                    currentLyricIndex = calculateActiveLyricIndex(song.lyrics, 0L),
                    isFavorite = it.favoriteSongIds.contains(song.id),
                    isLyricsSynced = song.lyrics.isNotEmpty(),
                    isLyricsLoading = false,
                    lyricsError = null,
                    plainLyricsText = null,
                    playbackError = null
                )
            }
            return
        }

        if (indexInQueue != -1) {
            controller.seekToDefaultPosition(indexInQueue)
        } else {
            val newQueue = currentQueue + song
            _state.update { it.copy(queue = newQueue) }
            controller.addMediaItem(song.toMediaItem())
            controller.seekToDefaultPosition(newQueue.lastIndex)
        }

        _state.update {
            it.copy(
                currentSong = song,
                durationMs = song.durationMs,
                currentPositionMs = 0L,
                progress = 0f,
                currentLyricIndex = calculateActiveLyricIndex(song.lyrics, 0L),
                isFavorite = it.favoriteSongIds.contains(song.id),
                isLyricsSynced = song.lyrics.isNotEmpty(),
                isLyricsLoading = false,
                lyricsError = null,
                plainLyricsText = null,
                playbackError = null
            )
        }

        if (autoPlay) {
            controller.play()
        }
    }

    fun playNext(song: Song) {
        val controller = mediaController
        val current = _state.value.currentSong
        if (current == null) {
            loadSong(song, autoPlay = true)
            return
        }
        val queue = _state.value.queue
        val reportedIndex = controller?.currentMediaItemIndex ?: -1
        val currentIndex = if (reportedIndex in queue.indices) reportedIndex else queue.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        val insertion = QueueOperations.insertAfterCurrent(queue, currentIndex, song)
        _state.update { it.copy(queue = insertion.items) }
        if (controller == null) {
            pendingInitialQueue = insertion.items
            pendingInitialSongId = current.id
        } else {
            controller.addMediaItem(insertion.index, song.toMediaItem())
        }
    }

    fun setLyricsState(
        syncedLines: List<LyricLine>,
        isSynced: Boolean,
        plainText: String?,
        isLoading: Boolean,
        error: String?
    ) {
        val current = _state.value.currentSong ?: return
        val updatedSong = current.copy(lyrics = syncedLines)
        val activeIndex = calculateActiveLyricIndex(syncedLines, _state.value.currentPositionMs)
        _state.update {
            it.copy(
                currentSong = updatedSong,
                currentLyricIndex = activeIndex,
                isLyricsSynced = isSynced,
                plainLyricsText = plainText,
                isLyricsLoading = isLoading,
                lyricsError = error
            )
        }
    }

    fun addSongAndPlay(song: Song) {
        loadSong(song, autoPlay = true)
        setNowPlayingExpanded(true)
    }

    fun toggleFavorite(songId: String? = null) {
        val targetId = songId ?: _state.value.currentSong?.id ?: return
        val currentFavorites = _state.value.favoriteSongIds.toMutableSet()
        if (currentFavorites.contains(targetId)) {
            currentFavorites.remove(targetId)
        } else {
            currentFavorites.add(targetId)
        }
        val isCurrentFav = _state.value.currentSong?.id?.let { currentFavorites.contains(it) } ?: false
        _state.update {
            it.copy(
                favoriteSongIds = currentFavorites,
                isFavorite = isCurrentFav
            )
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val currentQueue = _state.value.queue.toMutableList()
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices) return
        val item = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, item)
        _state.update { it.copy(queue = currentQueue) }
        mediaController?.moveMediaItem(fromIndex, toIndex)
    }

    fun removeQueueItem(index: Int) {
        val currentQueue = _state.value.queue.toMutableList()
        if (index !in currentQueue.indices) return
        val removedSong = currentQueue.removeAt(index)
        _state.update {
            val nextSong = if (it.currentSong?.id == removedSong.id) {
                currentQueue.firstOrNull()
            } else {
                it.currentSong
            }
            it.copy(
                queue = currentQueue,
                currentSong = nextSong
            )
        }
        mediaController?.removeMediaItem(index)
    }

    fun addToQueue(song: Song) {
        val currentQueue = _state.value.queue
        val insertion = QueueOperations.append(currentQueue, song)
        _state.update { it.copy(queue = insertion.items) }
        val controller = mediaController
        if (controller == null) {
            pendingInitialQueue = insertion.items
            pendingInitialSongId = _state.value.currentSong?.id ?: pendingInitialSongId
        } else {
            controller.addMediaItem(insertion.index, song.toMediaItem())
        }
    }

    fun setLyricsMode(mode: LyricsDisplayMode) {
        _state.update { it.copy(lyricsMode = mode) }
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _state.update { it.copy(isNowPlayingExpanded = expanded) }
    }

    fun setBitPerfectRequested(enabled: Boolean) {
        audioOutputManager.setBitPerfectRequested(enabled)
    }

    private fun startPositionTracker() {
        stopPositionTracker()
        tickerJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                updatePositionFromPlayer()
                delay(200L)
            }
        }
    }

    private fun stopPositionTracker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updatePositionFromPlayer() {
        val controller = mediaController ?: return
        val currentSong = _state.value.currentSong ?: return

        val playerPosition = controller.currentPosition.coerceAtLeast(0L)
        val playerDuration = controller.duration
        val effectiveDuration = if (playerDuration > 0L) playerDuration else currentSong.durationMs
        val progress = if (effectiveDuration > 0L) {
            (playerPosition.toFloat() / effectiveDuration).coerceIn(0f, 1f)
        } else {
            0f
        }
        val lyricIndex = calculateActiveLyricIndex(currentSong.lyrics, playerPosition)

        _state.update {
            it.copy(
                currentPositionMs = playerPosition,
                durationMs = effectiveDuration,
                progress = progress,
                currentLyricIndex = lyricIndex
            )
        }
    }

    private fun calculateActiveLyricIndex(lyrics: List<LyricLine>?, positionMs: Long): Int {
        if (lyrics.isNullOrEmpty()) return -1
        var activeIndex = -1
        for (i in lyrics.indices) {
            if (positionMs >= lyrics[i].timestampMs) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
    }

    fun release() {
        stopPositionTracker()
        audioOutputManager.release()
        mediaController?.let {
            it.removeListener(playerListener)
            it.release()
        }
        mediaController = null
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controllerFuture = null
    }
}
