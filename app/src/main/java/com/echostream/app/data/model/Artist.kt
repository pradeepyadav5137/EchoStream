package com.echostream.app.data.model

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String = "",
    val bio: String = "",
    val followersCount: Int = 0,
    val genres: List<String> = emptyList(),
    val isFollowed: Boolean = false
)
