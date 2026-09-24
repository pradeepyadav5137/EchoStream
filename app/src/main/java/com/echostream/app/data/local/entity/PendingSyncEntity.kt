package com.echostream.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // LIKE, UNLIKE, HISTORY, CREATE_PLAYLIST
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
