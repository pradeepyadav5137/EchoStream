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

    val totalListeningTime: StateFlow<Int> = MutableStateFlow(0)
    val todayListeningTime: StateFlow<Int> = MutableStateFlow(0)
    val mostPlayedSong: StateFlow<Song?> = MutableStateFlow(null)

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
            }
                .catch { }
                .collect {}
        }
        
        viewModelScope.launch {
            musicRepository.getRecommendedSections().collect { sections ->
                val songs = mutableListOf<Song>()
                sections.forEach { section ->
                    val sectionSongs = section["songs"] as? List<Song>
                    if (sectionSongs != null) {
                        songs.addAll(sectionSongs)
                    }
                }
                (recommendedSongs as MutableStateFlow).value = songs
            }
        }

        viewModelScope.launch {
            musicRepository.getTotalListeningTime().collect { total ->
                (totalListeningTime as MutableStateFlow).value = total ?: 0
            }
        }

        viewModelScope.launch {
            val startOfDay = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            musicRepository.getTodayListeningTime(startOfDay).collect { today ->
                (todayListeningTime as MutableStateFlow).value = today ?: 0
            }
        }

        viewModelScope.launch {
            musicRepository.getMostPlayedSong().collect { entity ->
                (mostPlayedSong as MutableStateFlow).value = entity?.toSong(isLiked = false, isDownloaded = false, localFilePath = null)
            }
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
