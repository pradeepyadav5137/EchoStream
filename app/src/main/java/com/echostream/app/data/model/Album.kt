package com.echostream.app.data.model

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val artworkUrl: String = "",
    val releaseYear: Int = 2024,
    val genre: String = "Pop",
    val songs: List<Song> = emptyList()
)
