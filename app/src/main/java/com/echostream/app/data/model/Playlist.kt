package com.echostream.app.data.model

data class Playlist(
    val id: String,
    val userId: String = "",
    val name: String,
    val description: String = "",
    val artworkUrl: String = "",
    val songs: List<Song> = emptyList(),
    val songIds: List<String> = emptyList(),
    val isPublic: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
