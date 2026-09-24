package com.echostream.app.data.model

data class LyricsLine(
    val timeMs: Long,
    val text: String
)

data class Lyrics(
    val songId: String,
    val plainLyrics: String = "",
    val syncedLyrics: List<LyricsLine> = emptyList()
)
