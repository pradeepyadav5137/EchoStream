package com.echostream.app.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val album: String = "",
    val albumId: String = "",
    val genre: String = "Pop",
    val duration: Int = 180, // in seconds
    val audioUrl: String,
    val artworkUrl: String = "",
    val playCount: Int = 0,
    val likeCount: Int = 0,
    val isTrending: Boolean = false,
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null
)
