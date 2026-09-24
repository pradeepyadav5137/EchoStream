package com.echostream.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echostream.app.data.model.Playlist
import com.echostream.app.data.model.Song
import com.echostream.app.data.repository.MusicRepository
import com.echostream.app.data.repository.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    val likedSongs: StateFlow<List<Song>> = MutableStateFlow(emptyList())
    val downloadedSongs: StateFlow<List<Song>> = MutableStateFlow(emptyList())
    val playlists: StateFlow<List<Playlist>> = MutableStateFlow(emptyList())
    val recentlyPlayed: StateFlow<List<Song>> = MutableStateFlow(emptyList())

    init {
        viewModelScope.launch {
            musicRepository.getLikedSongs().collect { songs ->
                (likedSongs as MutableStateFlow).value = songs
            }
        }

        viewModelScope.launch {
            musicRepository.getDownloadedSongs().collect { songs ->
                (downloadedSongs as MutableStateFlow).value = songs
            }
        }

        viewModelScope.launch {
            playlistRepository.getPlaylists().collect { pls ->
                (playlists as MutableStateFlow).value = pls
            }
        }

        viewModelScope.launch {
            musicRepository.getRecentlyPlayed().collect { songs ->
                (recentlyPlayed as MutableStateFlow).value = songs
            }
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name, description)
        }
    }

    fun getPlaylistSongs(playlistId: String): kotlinx.coroutines.flow.Flow<List<Song>> {
        return playlistRepository.getSongsForPlaylist(playlistId)
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            musicRepository.toggleLike(song)
        }
    }
}
