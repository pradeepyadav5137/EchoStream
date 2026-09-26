package com.echostream.app.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.echostream.app.data.api.NetworkClient
import com.echostream.app.data.model.Song
import com.echostream.app.utils.Constants
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(UnstableApi::class)
class PlayerManager private constructor(private val context: Context) {

    val player: ExoPlayer
    @Volatile
    private var currentBaseUrl: String = Constants.DEFAULT_BASE_URL

    fun updateBaseUrl(newUrl: String) {
        if (newUrl.isNotBlank()) {
            currentBaseUrl = newUrl
        }
    }

    init {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        
        val resolvingDataSourceFactory = androidx.media3.datasource.ResolvingDataSource.Factory(
            dataSourceFactory,
            object : androidx.media3.datasource.ResolvingDataSource.Resolver {
                override fun resolveDataSpec(dataSpec: androidx.media3.datasource.DataSpec): androidx.media3.datasource.DataSpec {
                    var uri = dataSpec.uri
                    val uriString = uri.toString()
                    if (uriString.startsWith("resolve://")) {
                        val songId = uriString.removePrefix("resolve://")
                        try {
                            Log.d("PlayerManager", "Fetching stream for songId: $songId using baseUrl: $currentBaseUrl")
                            val service = NetworkClient.getEchoStreamService(context, currentBaseUrl)
                            val response = runBlocking { service.getStreamUrl(songId) }
                            if (response.isSuccessful) {
                                val url = response.body()?.get("url") as? String
                                if (!url.isNullOrEmpty()) {
                                    Log.d("PlayerManager", "Successfully resolved stream URL: $url")
                                    uri = Uri.parse(url)
                                } else {
                                    Log.e("PlayerManager", "Stream response contained no URL for songId: $songId")
                                }
                            } else {
                                Log.e("PlayerManager", "Failed to resolve stream. HTTP Code: ${response.code()}")
                            }
                        } catch (e: Exception) {
                            Log.e("PlayerManager", "Error resolving stream URL for songId: $songId", e)
                        }

                        if (uri.toString().startsWith("resolve://")) {
                            Log.w("PlayerManager", "Stream resolution fallback triggered for $songId")
                            uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
                        }
                    }
                    return dataSpec.withUri(uri)
                }
            }
        )
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context).setDataSourceFactory(resolvingDataSourceFactory)

        player = ExoPlayer.Builder(context.applicationContext)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(audioAttributes, true)
        }
    }

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private val _showFullPlayer = MutableStateFlow(false)
    val showFullPlayer: StateFlow<Boolean> = _showFullPlayer.asStateFlow()

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private var controllerFuture: ListenableFuture<MediaController>? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    progressJob?.cancel()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0L)
                } else if (playbackState == Player.STATE_ENDED) {
                    playNext()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                if (index in _playlist.value.indices) {
                    _currentIndex.value = index
                    _currentSong.value = _playlist.value[index]
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // Safely update state without infinite recursive calls
                _isPlaying.value = false
                progressJob?.cancel()
            }
        })

        // Bind to PlaybackService using MediaController to ensure MediaSessionService behaves correctly
        val sessionToken = SessionToken(context, android.content.ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            // Bound successfully
        }, MoreExecutors.directExecutor())
    }

    fun playSong(song: Song, playlist: List<Song> = listOf(song)) {
        scope.launch {
            try {
                _playlist.value = playlist
                val index = playlist.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                _currentIndex.value = index
                _currentSong.value = playlist[index]

                player.clearMediaItems()
                val mediaItems = mutableListOf<MediaItem>()

                for (s in playlist) {
                    val audioUriString = if (s.isDownloaded && !s.localFilePath.isNullOrEmpty()) {
                        s.localFilePath
                    } else {
                        "resolve://${s.id}"
                    }

                    val metadata = MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album)
                        .setArtworkUri(Uri.parse(s.artworkUrl.ifEmpty { "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600" }))
                        .build()

                    mediaItems.add(
                        MediaItem.Builder()
                            .setMediaId(s.id)
                            .setUri(Uri.parse(audioUriString))
                            .setMediaMetadata(metadata)
                            .build()
                    )
                }

                player.setMediaItems(mediaItems, index, 0L)
                player.prepare()
                
                // Start the PlaybackService just before playing
                try {
                    val serviceIntent = android.content.Intent(context, PlaybackService::class.java)
                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                player.play()
            } catch (_: Exception) {
                _isPlaying.value = false
            }
        }
    }

    fun togglePlayPause() {
        try {
            if (player.isPlaying) {
                player.pause()
            } else {
                try {
                    val serviceIntent = android.content.Intent(context, PlaybackService::class.java)
                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {}
                player.play()
            }
        } catch (_: Exception) {
        }
    }

    fun playNext() {
        try {
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                try {
                    val serviceIntent = android.content.Intent(context, PlaybackService::class.java)
                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {}
                player.play()
            } else if (_playlist.value.isNotEmpty()) {
                player.seekTo(0, 0L)
                try {
                    val serviceIntent = android.content.Intent(context, PlaybackService::class.java)
                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {}
                player.play()
            }
        } catch (_: Exception) {
        }
    }

    fun playPrevious() {
        try {
            if (player.currentPosition > 3000L) {
                player.seekTo(0L)
            } else if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
                try {
                    val serviceIntent = android.content.Intent(context, PlaybackService::class.java)
                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {}
                player.play()
            }
        } catch (_: Exception) {
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            player.seekTo(positionMs)
            _currentPosition.value = positionMs
        } catch (_: Exception) {
        }
    }

    fun toggleShuffle() {
        val next = !_isShuffle.value
        _isShuffle.value = next
        player.shuffleModeEnabled = next
    }

    fun toggleRepeat() {
        val next = !_isRepeat.value
        _isRepeat.value = next
        player.repeatMode = if (next) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun setFullPlayerVisible(visible: Boolean) {
        _showFullPlayer.value = visible
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && player.isPlaying) {
                _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                _duration.value = player.duration.coerceAtLeast(0L)
                delay(500)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: PlayerManager? = null

        fun getInstance(context: Context): PlayerManager {
            return instance ?: synchronized(this) {
                instance ?: PlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
