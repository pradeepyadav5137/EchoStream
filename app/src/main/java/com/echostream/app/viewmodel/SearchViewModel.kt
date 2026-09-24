package com.echostream.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echostream.app.data.model.Song
import com.echostream.app.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val musicRepository: MusicRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private var searchJob: Job? = null

    private var currentPage = 1
    private var hasMore = true

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            currentPage = 1
            hasMore = true
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            _isSearching.value = true
            currentPage = 1
            musicRepository.search(newQuery, currentPage).collect { songs ->
                _searchResults.value = songs
                _isSearching.value = false
                hasMore = songs.size >= 20
            }
        }
    }

    fun loadMore() {
        if (_isSearching.value || !hasMore || _query.value.isBlank()) return

        _isSearching.value = true
        currentPage++

        viewModelScope.launch {
            musicRepository.search(_query.value, currentPage).collect { newSongs ->
                val current = _searchResults.value.toMutableList()
                current.addAll(newSongs)
                _searchResults.value = current
                _isSearching.value = false
                hasMore = newSongs.isNotEmpty()
            }
        }
    }

    fun clearQuery() {
        onQueryChange("")
    }

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            musicRepository.toggleLike(song)
        }
    }
}
