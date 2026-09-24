package com.echostream.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val artworkUrl: String,
    val isPublic: Boolean,
    val updatedAt: Long
)
