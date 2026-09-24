package com.echostream.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val songId: String,
    val localAudioPath: String,
    val localArtworkPath: String = "",
    val downloadedAt: Long = System.currentTimeMillis()
)
