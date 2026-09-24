package com.echostream.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "likes")
data class LikeEntity(
    @PrimaryKey val songId: String,
    val likedAt: Long = System.currentTimeMillis()
)
