package com.echostream.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echostream.app.data.model.Comment
import com.echostream.app.data.model.Lyrics
import com.echostream.app.data.model.Playlist
import com.echostream.app.data.model.Song
import com.echostream.app.data.repository.DownloadRepository
import com.echostream.app.data.repository.MusicRepository
import com.echostream.app.data.repository.PlaylistRepository
import com.echostream.app.player.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    val playerManager: PlayerManager,
    private val musicRepository: MusicRepository,
    private val downloadRepository: DownloadRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val duration: StateFlow<Long> = playerManager.duration
    val isShuffle: StateFlow<Boolean> = playerManager.isShuffle
    val isRepeat: StateFlow<Boolean> = playerManager.isRepeat
    val showFullPlayer: StateFlow<Boolean> = playerManager.showFullPlayer

    private val _lyrics = MutableStateFlow<Lyrics?>(null)
    val lyrics: StateFlow<Lyrics?> = _lyrics

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private val _showCommentsSheet = MutableStateFlow(false)
    val showCommentsSheet: StateFlow<Boolean> = _showCommentsSheet

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress

    private val _showPlaylistSheet = MutableStateFlow(false)
    val showPlaylistSheet: StateFlow<Boolean> = _showPlaylistSheet

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    init {
        viewModelScope.launch {
            playerManager.currentSong.collect { song ->
                if (song != null) {
                    musicRepository.recordHistory(song)
                    loadLyrics(song.id)
                    loadComments(song.id)
                }
            }
        }
        viewModelScope.launch {
            playlistRepository.getPlaylists().collect { pls ->
                _playlists.value = pls
            }
        }
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        playerManager.playSong(song, queue)
    }

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun playNext() = playerManager.playNext()
    fun playPrevious() = playerManager.playPrevious()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun toggleRepeat() = playerManager.toggleRepeat()
    fun setFullPlayerVisible(visible: Boolean) = playerManager.setFullPlayerVisible(visible)

    fun toggleLike() {
        val song = currentSong.value ?: return
        viewModelScope.launch {
            musicRepository.toggleLike(song)
        }
    }

    fun downloadSong() {
        val song = currentSong.value ?: return
        viewModelScope.launch {
            if (song.isDownloaded) {
                downloadRepository.deleteDownload(song.id)
            } else {
                downloadRepository.downloadSong(song) { progress ->
                    _downloadProgress.value = progress
                }
            }
        }
    }

    private fun loadLyrics(songId: String) {
        viewModelScope.launch {
            _lyrics.value = musicRepository.getLyrics(songId)
        }
    }

    private fun loadComments(songId: String) {
        viewModelScope.launch {
            // Comments list handled via music repository or default list
            _comments.value = listOf(
                Comment(
                    id = "c1",
                    userId = "u1",
                    username = "BeatLover",
                    songId = songId,
                    text = "This bass drop is unreal! 🔥",
                    createdAt = "2 hours ago"
                ),
                Comment(
                    id = "c2",
                    userId = "u2",
                    username = "VibeMaster",
                    songId = songId,
                    text = "EchoStream always plays the best tracks!",
                    createdAt = "1 hour ago"
                )
            )
        }
    }

    fun addComment(text: String) {
        val song = currentSong.value ?: return
        val newComment = Comment(
            id = "cmt_" + System.currentTimeMillis(),
            userId = "usr_me",
            username = "You",
            songId = song.id,
            text = text,
            createdAt = "Just now"
        )
        _comments.value = listOf(newComment) + _comments.value
    }

    fun setShowCommentsSheet(show: Boolean) {
        _showCommentsSheet.value = show
    }

    fun setShowPlaylistSheet(show: Boolean) {
        _showPlaylistSheet.value = show
    }

    fun addSongToPlaylist(playlistId: String) {
        val song = currentSong.value ?: return
        viewModelScope.launch {
            musicRepository.insertSongLocally(song)
            playlistRepository.addSongToPlaylist(playlistId, song.id)
            setShowPlaylistSheet(false)
        }
    }
}
