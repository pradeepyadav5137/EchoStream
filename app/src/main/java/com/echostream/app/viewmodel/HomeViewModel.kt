package com.echostream.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echostream.app.data.model.Song
import com.echostream.app.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HomeViewModel(private val musicRepository: MusicRepository) : ViewModel() {

    val allSongs: StateFlow<List<Song>> = MutableStateFlow(emptyList())
    val trendingSongs: StateFlow<List<Song>> = MutableStateFlow(emptyList())
    val recommendedSongs: StateFlow<List<Song>> = MutableStateFlow(emptyList())
    val recentlyPlayed: StateFlow<List<Song>> = MutableStateFlow(emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                musicRepository.refreshSongs()
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }

        viewModelScope.launch {
            combine(
                musicRepository.getSongs(),
                musicRepository.getLikedSongs(),
                musicRepository.getRecentlyPlayed()
            ) { songs, likes, history ->
                (allSongs as MutableStateFlow).value = songs
                (trendingSongs as MutableStateFlow).value = songs.filter { it.isTrending || it.likeCount > 3000 }
                (recentlyPlayed as MutableStateFlow).value = history
                (recommendedSongs as MutableStateFlow).value = musicRepository.getRecommendations(songs, likes, history)
            }
                .catch { }
                .collect {}
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                musicRepository.refreshSongs()
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            try {
                musicRepository.toggleLike(song)
            } catch (_: Exception) {
            }
        }
    }
}
