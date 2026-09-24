package com.echostream.app.data.model

data class Comment(
    val id: String,
    val userId: String,
    val username: String,
    val avatarUrl: String = "",
    val songId: String,
    val text: String,
    val createdAt: String = ""
)
