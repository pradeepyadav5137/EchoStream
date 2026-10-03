package com.echostream.app.data.repository

import android.util.Log
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
    private val historyDao: HistoryDao,
    private val songDao: com.echostream.app.data.local.dao.SongDao
) {
    private val gson = Gson()
    private val TAG = "SyncRepository"

    fun updateApiService(newService: EchoStreamApiService) {
        echoStreamApi = newService
    }

    /**
     * Force uploads ALL local state (likes, history, playlists) to the backend.
     * This is the "nuclear option" button on the Profile page.
     */
    suspend fun forceUploadState(): Result<String> {
        var likesUploaded = 0
        var historyUploaded = 0
        var playlistsUploaded = 0
        var errors = 0
        var lastError = ""

        return try {
            // 1. Upload all favorites
            val allLikes = likeDao.getLikedSongsSync()
            Log.d(TAG, "forceUpload: Found ${allLikes.size} liked songs to upload")
            for (song in allLikes) {
                val payload = mapOf<String, String>(
                    "title" to song.title,
                    "artist" to song.artist,
                    "album" to song.album,
                    "thumbnail" to song.artworkUrl,
                    "duration" to song.duration.toString(),
                    "source" to "youtube"
                )
                try {
                    val response = echoStreamApi.addFavorite(song.id, payload)
                    if (response.isSuccessful) {
                        likesUploaded++
                        Log.d(TAG, "forceUpload: Uploaded favorite '${song.title}'")
                    } else {
                        val code = response.code()
                        val body = response.errorBody()?.string() ?: ""
                        // 400 "Already favorited" is fine — treat as success
                        if (code == 400 && body.contains("Already")) {
                            likesUploaded++
                            Log.d(TAG, "forceUpload: Favorite '${song.title}' already exists on server (OK)")
                        } else {
                            errors++
                            lastError = "Like HTTP $code: $body"
                            Log.e(TAG, "forceUpload: Failed favorite '${song.title}', $lastError")
                        }
                    }
                } catch (e: Exception) {
                    errors++
                    lastError = "Like exception: ${e.message}"
                    Log.e(TAG, "forceUpload: Exception uploading favorite '${song.title}'", e)
                }
            }

            // 2. Upload all history
            val allHistory = historyDao.getAllHistorySync()
            Log.d(TAG, "forceUpload: Found ${allHistory.size} history entries to upload")
            for (hist in allHistory) {
                val song = songDao.getSongById(hist.songId)
                if (song != null) {
                    val payload = mapOf<String, String>(
                        "songId" to song.id,
                        "title" to song.title,
                        "artist" to song.artist,
                        "album" to song.album,
                        "thumbnail" to song.artworkUrl,
                        "duration" to song.duration.toString(),
                        "durationPlayed" to hist.durationPlayed.toString(),
                        "playedAt" to hist.playedAt.toString()
                    )
                    try {
                        val response = echoStreamApi.addHistory(payload)
                        if (response.isSuccessful) {
                            historyUploaded++
                            Log.d(TAG, "forceUpload: Uploaded history '${song.title}'")
                        } else {
                            // History endpoint replaces duplicates, so no 400 issue
                            errors++
                            lastError = "History HTTP ${response.code()}: ${response.errorBody()?.string()}"
                            Log.e(TAG, "forceUpload: Failed history '${song.title}', $lastError")
                        }
                    } catch (e: Exception) {
                        errors++
                        lastError = "History exception: ${e.message}"
                        Log.e(TAG, "forceUpload: Exception uploading history '${song.title}'", e)
                    }
                } else {
                    Log.w(TAG, "forceUpload: Skipping history for songId=${hist.songId} — song not in local DB")
                }
            }

            // 3. Upload playlists
            val playlists = playlistDao.getAllPlaylistsSync()
            Log.d(TAG, "forceUpload: Found ${playlists.size} playlists to upload")
            for (pl in playlists) {
                val plPayload = mapOf<String, String>(
                    "name" to pl.name,
                    "description" to pl.description,
                    "isPublic" to "true",
                    "artworkUrl" to pl.artworkUrl
                )
                try {
                    val response = echoStreamApi.createPlaylist(plPayload)
                    if (response.isSuccessful) {
                        playlistsUploaded++
                        Log.d(TAG, "forceUpload: Uploaded playlist '${pl.name}'")
                        
                        // Add songs to the playlist
                        val serverId = (response.body()?.get("playlist") as? Map<*, *>)?.get("_id") as? String
                        if (serverId != null) {
                            val songs = playlistDao.getSongsForPlaylistSync(pl.id)
                            for (song in songs) {
                                try {
                                    val songPayload = mapOf<String, String>(
                                        "songId" to song.id,
                                        "title" to song.title,
                                        "artist" to song.artist,
                                        "album" to song.album,
                                        "thumbnail" to song.artworkUrl,
                                        "duration" to song.duration.toString(),
                                        "source" to "youtube"
                                    )
                                    echoStreamApi.addSongToPlaylist(serverId, songPayload)
                                } catch (e: Exception) {
                                    Log.e(TAG, "forceUpload: Failed to add song '${song.title}' to playlist '${pl.name}'", e)
                                }
                            }
                        }
                    } else {
                        errors++
                        lastError = "Playlist HTTP ${response.code()}: ${response.errorBody()?.string()}"
                        Log.e(TAG, "forceUpload: Failed playlist '${pl.name}', $lastError")
                    }
                } catch (e: Exception) {
                    errors++
                    lastError = "Playlist exception: ${e.message}"
                    Log.e(TAG, "forceUpload: Exception uploading playlist '${pl.name}'", e)
                }
            }

            val summary = if (errors > 0) {
                "Uploaded: $likesUploaded likes, $historyUploaded history, $playlistsUploaded playlists. Errors: $errors ($lastError)"
            } else {
                "Uploaded: $likesUploaded likes, $historyUploaded history, $playlistsUploaded playlists!"
            }
            Log.d(TAG, "forceUpload: DONE — $summary")
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "forceUpload: Fatal error", e)
            Result.failure(Exception("Upload failed: ${e.message}"))
        }
    }

    /**
     * Performs a bi-directional sync:
     * Step 1: Push all pending offline actions to the server
     * Step 2: Push ALL local state to the server (upload-first strategy)
     * Step 3: Pull remote state and MERGE (don't blindly clear)
     */
    suspend fun performSync(): Result<String> {
        return try {
            Log.d(TAG, "performSync: Starting...")

            // ===== STEP 1: Process pending offline actions =====
            val pendingActions = pendingSyncDao.getAllPendingActions()
            Log.d(TAG, "performSync: ${pendingActions.size} pending actions to process")
            for (action in pendingActions) {
                try {
                    val mapType = object : TypeToken<Map<String, Any>>() {}.type
                    val payload: Map<String, Any> = gson.fromJson(action.payloadJson, mapType)

                    when (action.type) {
                        "LIKE" -> {
                            val songId = payload["songId"] as? String
                            if (songId != null) {
                                val response = echoStreamApi.addFavorite(songId, payload.mapValues { it.value.toString() })
                                if (!response.isSuccessful && response.code() != 400) {
                                    Log.e(TAG, "performSync: LIKE failed for $songId, HTTP ${response.code()}")
                                    throw Exception("API error")
                                }
                            }
                        }
                        "UNLIKE" -> {
                            val songId = payload["songId"] as? String
                            if (songId != null) {
                                val response = echoStreamApi.removeFavorite(songId)
                                if (!response.isSuccessful) throw Exception("API error")
                            }
                        }
                        "HISTORY" -> {
                            val response = echoStreamApi.addHistory(payload.mapValues { it.value.toString() })
                            if (!response.isSuccessful) throw Exception("API error")
                        }
                        "CREATE_PLAYLIST" -> {
                            val response = echoStreamApi.createPlaylist(payload.mapValues { it.value.toString() })
                            if (!response.isSuccessful) throw Exception("API error")
                        }
                        "ADD_PLAYLIST_SONG" -> {
                            val playlistId = payload["playlistId"] as? String
                            val songId = payload["songId"] as? String
                            if (playlistId != null && songId != null) {
                                val song = songDao.getSongById(songId)
                                if (song != null) {
                                    val songPayload = mapOf<String, String>(
                                        "songId" to song.id,
                                        "title" to song.title,
                                        "artist" to song.artist,
                                        "album" to song.album,
                                        "thumbnail" to song.artworkUrl,
                                        "duration" to song.duration.toString(),
                                        "source" to "youtube"
                                    )
                                    val response = echoStreamApi.addSongToPlaylist(playlistId, songPayload)
                                    if (!response.isSuccessful) throw Exception("API error")
                                } else {
                                    val response = echoStreamApi.addSongToPlaylist(playlistId, mapOf("songId" to songId))
                                    if (!response.isSuccessful) throw Exception("API error")
                                }
                            }
                        }
                        "REMOVE_PLAYLIST_SONG" -> {
                            val playlistId = payload["playlistId"] as? String
                            val songId = payload["songId"] as? String
                            if (playlistId != null && songId != null) {
                                val response = echoStreamApi.removeSongFromPlaylist(playlistId, songId)
                                if (!response.isSuccessful) throw Exception("API error")
                            }
                        }
                    }
                    // Delete only if successful
                    pendingSyncDao.deletePendingAction(action.id)
                    Log.d(TAG, "performSync: Processed pending action ${action.type}")
                } catch (e: Exception) {
                    Log.e(TAG, "performSync: Failed pending action ${action.type}: ${e.message}")
                }
            }

            // ===== STEP 2: Upload current local state to server =====
            try {
                forceUploadState()
            } catch (e: Exception) {
                Log.e(TAG, "performSync: forceUploadState failed", e)
            }

            // ===== STEP 3: Pull remote state — but DON'T clear if server returns nothing =====
            try {
                // Fetch Favorites
                val favResponse = echoStreamApi.getFavorites()
                if (favResponse.isSuccessful) {
                    val favData = favResponse.body()?.get("favorites") as? List<Map<String, Any>>
                    if (favData != null && favData.isNotEmpty()) {
                        Log.d(TAG, "performSync: Server has ${favData.size} favorites, replacing local")
                        likeDao.clearAll()
                        favData.forEach { fav ->
                            val songId = fav["songId"] as? String
                            if (songId != null) {
                                val songTitle = fav["title"] as? String ?: "Unknown"
                                val songArtist = fav["artist"] as? String ?: "Unknown"
                                val songAlbum = fav["album"] as? String ?: "Unknown"
                                val songThumb = fav["thumbnail"] as? String ?: ""
                                val duration = (fav["duration"] as? Number)?.toInt() ?: 0

                                val songEntity = com.echostream.app.data.local.entity.SongEntity(
                                    id = songId,
                                    title = songTitle,
                                    artist = songArtist,
                                    album = songAlbum,
                                    artworkUrl = songThumb,
                                    duration = duration,
                                    audioUrl = "resolve://$songId",
                                    artistId = "unknown",
                                    albumId = "unknown",
                                    genre = "Pop",
                                    playCount = 0,
                                    likeCount = 0,
                                    isTrending = false
                                )
                                songDao.insertSong(songEntity)
                                likeDao.insertLike(LikeEntity(songId = songId))
                            }
                        }
                    } else {
                        Log.d(TAG, "performSync: Server has NO favorites, keeping local data intact")
                    }
                } else {
                    Log.e(TAG, "performSync: getFavorites failed, HTTP ${favResponse.code()}: ${favResponse.errorBody()?.string()}")
                }

                // Fetch Playlists
                val plResponse = echoStreamApi.getPlaylists()
                if (plResponse.isSuccessful) {
                    val plData = plResponse.body()?.get("playlists") as? List<Map<String, Any>>
                    if (plData != null && plData.isNotEmpty()) {
                        Log.d(TAG, "performSync: Server has ${plData.size} playlists, replacing local")
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
                                    val sTitle = sMap["title"] as? String ?: "Unknown"
                                    val sArtist = sMap["artist"] as? String ?: "Unknown"
                                    val sThumb = sMap["thumbnail"] as? String ?: ""

                                    val songEntity = com.echostream.app.data.local.entity.SongEntity(
                                        id = sId,
                                        title = sTitle,
                                        artist = sArtist,
                                        album = sMap["album"] as? String ?: "Unknown",
                                        artworkUrl = sThumb,
                                        duration = (sMap["duration"] as? Double)?.toInt() ?: 0,
                                        audioUrl = "resolve://$sId",
                                        artistId = "unknown",
                                        albumId = "unknown",
                                        genre = "Pop",
                                        playCount = 0,
                                        likeCount = 0,
                                        isTrending = false
                                    )
                                    songDao.insertSong(songEntity)

                                    playlistDao.addSongToPlaylist(
                                        com.echostream.app.data.local.entity.PlaylistSongEntity(
                                            playlistId = id,
                                            songId = sId
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "performSync: Server has NO playlists, keeping local data intact")
                    }
                } else {
                    Log.e(TAG, "performSync: getPlaylists failed, HTTP ${plResponse.code()}: ${plResponse.errorBody()?.string()}")
                }

                // Fetch History
                val histResponse = echoStreamApi.getHistory()
                if (histResponse.isSuccessful) {
                    val histData = histResponse.body()?.get("history") as? List<Map<String, Any>>
                    if (histData != null && histData.isNotEmpty()) {
                        Log.d(TAG, "performSync: Server has ${histData.size} history entries, replacing local")
                        historyDao.clearHistory()
                        histData.forEach { h ->
                            val sId = h["songId"] as? String
                            val durationPlayed = (h["durationPlayed"] as? Number)?.toInt() ?: 0
                            val playedAt = (h["playedAt"] as? String)?.let {
                                try {
                                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { 
                                        timeZone = java.util.TimeZone.getTimeZone("UTC") 
                                    }.parse(it)?.time
                                } catch (e: Exception) { null }
                            } ?: System.currentTimeMillis()

                            if (sId != null) {
                                val songTitle = h["title"] as? String ?: "Unknown"
                                val songArtist = h["artist"] as? String ?: "Unknown"
                                val songThumb = h["thumbnail"] as? String ?: ""

                                val songEntity = com.echostream.app.data.local.entity.SongEntity(
                                    id = sId,
                                    title = songTitle,
                                    artist = songArtist,
                                    album = h["album"] as? String ?: "Unknown",
                                    artworkUrl = songThumb,
                                    duration = (h["duration"] as? Number)?.toInt() ?: 0,
                                    audioUrl = "resolve://$sId",
                                    artistId = "unknown",
                                    albumId = "unknown",
                                    genre = "Pop",
                                    playCount = 0,
                                    likeCount = 0,
                                    isTrending = false
                                )
                                songDao.insertSong(songEntity)

                                historyDao.insertHistory(
                                    com.echostream.app.data.local.entity.HistoryEntity(
                                        songId = sId,
                                        playedAt = playedAt,
                                        durationPlayed = durationPlayed
                                    )
                                )
                            }
                        }
                    } else {
                        Log.d(TAG, "performSync: Server has NO history, keeping local data intact")
                    }
                } else {
                    Log.e(TAG, "performSync: getHistory failed, HTTP ${histResponse.code()}: ${histResponse.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "performSync: Error during remote fetch: ${e.message}", e)
            }

            Log.d(TAG, "performSync: DONE")
            Result.success("Sync completed successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "performSync: Fatal error", e)
            Result.failure(Exception("Sync failed: ${e.message}"))
        }
    }
}
