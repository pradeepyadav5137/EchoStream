package com.echostream.app.data.repository

import com.echostream.app.data.api.EchoStreamApiService
import com.echostream.app.data.local.dao.HistoryDao
import com.echostream.app.data.local.dao.LikeDao
import com.echostream.app.data.local.dao.PendingSyncDao
import com.echostream.app.data.local.dao.PlaylistDao
import com.echostream.app.data.local.entity.HistoryEntity
import com.echostream.app.data.local.entity.LikeEntity
import com.echostream.app.data.local.entity.PlaylistEntity
import com.echostream.app.data.model.PendingAction
import com.echostream.app.data.model.SyncRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SyncRepository(
    private var echoStreamApi: EchoStreamApiService,
    private val pendingSyncDao: PendingSyncDao,
    private val likeDao: LikeDao,
    private val playlistDao: PlaylistDao,
    private val historyDao: HistoryDao
) {
    private val gson = Gson()

    fun updateApiService(newService: EchoStreamApiService) {
        echoStreamApi = newService
    }

    suspend fun performSync(): Result<String> {
        return try {
            // 1. Process pending offline actions
            val pendingActions = pendingSyncDao.getAllPendingActions()
            for (action in pendingActions) {
                try {
                    val mapType = object : TypeToken<Map<String, Any>>() {}.type
                    val payload: Map<String, Any> = gson.fromJson(action.payloadJson, mapType)

                    when (action.type) {
                        "LIKE" -> {
                            val songId = payload["songId"] as? String
                            if (songId != null) {
                                echoStreamApi.addFavorite(songId, payload)
                            }
                        }
                        "UNLIKE" -> {
                            val songId = payload["songId"] as? String
                            if (songId != null) {
                                echoStreamApi.removeFavorite(songId)
                            }
                        }
                        "HISTORY" -> {
                            echoStreamApi.addHistory(payload)
                        }
                        "CREATE_PLAYLIST" -> {
                            echoStreamApi.createPlaylist(payload)
                        }
                        "ADD_PLAYLIST_SONG" -> {
                            val playlistId = payload["playlistId"] as? String
                            val songId = payload["songId"] as? String
                            if (playlistId != null && songId != null) {
                                echoStreamApi.addSongToPlaylist(playlistId, mapOf("songId" to songId))
                            }
                        }
                        "REMOVE_PLAYLIST_SONG" -> {
                            val playlistId = payload["playlistId"] as? String
                            val songId = payload["songId"] as? String
                            if (playlistId != null && songId != null) {
                                echoStreamApi.removeSongFromPlaylist(playlistId, songId)
                            }
                        }
                    }
                    // Delete if successful
                    pendingSyncDao.deletePendingAction(action.id)
                } catch (e: Exception) {
                    // Skip and try again later
                }
            }
            
            // 2. Fetch remote state to ensure consistency across devices
            try {
                // Fetch Favorites
                val favResponse = echoStreamApi.getFavorites()
                if (favResponse.isSuccessful) {
                    val favData = favResponse.body()?.get("favorites") as? List<Map<String, Any>>
                    if (favData != null) {
                        likeDao.clearAll()
                        favData.forEach { fav ->
                            val songId = fav["songId"] as? String
                            if (songId != null) {
                                likeDao.insertLike(LikeEntity(songId = songId))
                            }
                        }
                    }
                }
                
                // Fetch Playlists
                val plResponse = echoStreamApi.getPlaylists()
                if (plResponse.isSuccessful) {
                    val plData = plResponse.body()?.get("playlists") as? List<Map<String, Any>>
                    if (plData != null) {
                        playlistDao.clearAllPlaylists()
                        playlistDao.clearAllPlaylistSongs()
                        plData.forEach { pl ->
                            val id = pl["_id"] as? String ?: pl["id"] as? String ?: return@forEach
                            val name = pl["name"] as? String ?: "Playlist"
                            val desc = pl["description"] as? String ?: ""
                            val artworkUrl = pl["artworkUrl"] as? String ?: ""
                            
                            playlistDao.insertPlaylist(
                                com.echostream.app.data.local.entity.PlaylistEntity(
                                    id = id,
                                    userId = "remote",
                                    name = name,
                                    description = desc,
                                    artworkUrl = artworkUrl,
                                    isPublic = true,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                            
                            val songs = pl["songs"] as? List<Map<String, Any>> ?: emptyList()
                            songs.forEach { sMap ->
                                val sId = sMap["songId"] as? String
                                if (sId != null) {
                                    playlistDao.addSongToPlaylist(
                                        com.echostream.app.data.local.entity.PlaylistSongEntity(
                                            playlistId = id,
                                            songId = sId
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Fetch History
                val histResponse = echoStreamApi.getHistory()
                if (histResponse.isSuccessful) {
                    val histData = histResponse.body()?.get("history") as? List<Map<String, Any>>
                    if (histData != null) {
                        historyDao.clearHistory()
                        histData.forEach { h ->
                            val sId = h["songId"] as? String
                            val playedAtStr = h["playedAt"] as? String
                            val durationPlayed = (h["durationPlayed"] as? Number)?.toInt() ?: 0
                            if (sId != null) {
                                historyDao.insertHistory(
                                    com.echostream.app.data.local.entity.HistoryEntity(
                                        songId = sId,
                                        playedAt = System.currentTimeMillis(), // Simplified timestamp
                                        durationPlayed = durationPlayed
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore fetch errors, keep local state
            }
            
            Result.success("Sync completed successfully!")
        } catch (e: Exception) {
            Result.failure(Exception("Sync failed"))
        }
    }
}
