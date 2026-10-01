package com.echostream.app.data.repository

import com.echostream.app.data.api.EchoStreamApiService
import com.echostream.app.data.local.dao.PendingSyncDao
import com.echostream.app.data.local.dao.PlaylistDao
import com.echostream.app.data.local.dao.SongDao
import com.echostream.app.data.local.entity.PendingSyncEntity
import com.echostream.app.data.local.entity.PlaylistEntity
import com.echostream.app.data.local.entity.PlaylistSongEntity
import com.echostream.app.data.model.Playlist
import com.echostream.app.data.model.Song
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PlaylistRepository(
    private var echoStreamApi: EchoStreamApiService,
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
    private val pendingSyncDao: PendingSyncDao
) {
    private val gson = Gson()

    fun updateApiService(newService: EchoStreamApiService) {
        echoStreamApi = newService
    }

    fun getPlaylists(): Flow<List<Playlist>> {
        return combine(
            playlistDao.getAllPlaylists(),
            playlistDao.getAllPlaylistSongs()
        ) { entities, playlistSongs ->
            val songCountMap = playlistSongs.groupBy { it.playlistId }.mapValues { it.value.map { ps -> ps.songId } }
            entities.map { entity ->
                Playlist(
                    id = entity.id,
                    userId = entity.userId,
                    name = entity.name,
                    description = entity.description,
                    artworkUrl = entity.artworkUrl,
                    songIds = songCountMap[entity.id] ?: emptyList(),
                    isPublic = entity.isPublic,
                    updatedAt = entity.updatedAt
                )
            }
        }
    }

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylist(playlistId).map { entities ->
            entities.map { it.toSong() }
        }
    }

    suspend fun createPlaylist(name: String, description: String = ""): Playlist {
        val playlistId = "pl_" + System.currentTimeMillis()
        val entity = PlaylistEntity(
            id = playlistId,
            userId = "local_usr",
            name = name,
            description = description,
            artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500",
            isPublic = true,
            updatedAt = System.currentTimeMillis()
        )
        playlistDao.insertPlaylist(entity)

        val payload = mapOf("id" to playlistId, "name" to name, "description" to description)
        try {
            val response = echoStreamApi.createPlaylist(payload)
            if (!response.isSuccessful) throw Exception("API error")
        } catch (e: Exception) {
            pendingSyncDao.insertPendingAction(
                PendingSyncEntity(type = "CREATE_PLAYLIST", payloadJson = gson.toJson(payload))
            )
        }

        return Playlist(id = playlistId, name = name, description = description, artworkUrl = entity.artworkUrl)
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        playlistDao.addSongToPlaylist(PlaylistSongEntity(playlistId = playlistId, songId = songId))
        try {
            val response = echoStreamApi.addSongToPlaylist(playlistId, mapOf("songId" to songId))
            if (!response.isSuccessful) throw Exception("API error")
        } catch (e: Exception) {
            pendingSyncDao.insertPendingAction(
                PendingSyncEntity(type = "ADD_PLAYLIST_SONG", payloadJson = gson.toJson(mapOf("playlistId" to playlistId, "songId" to songId)))
            )
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
        try {
            val response = echoStreamApi.removeSongFromPlaylist(playlistId, songId)
            if (!response.isSuccessful) throw Exception("API error")
        } catch (e: Exception) {
            pendingSyncDao.insertPendingAction(
                PendingSyncEntity(type = "REMOVE_PLAYLIST_SONG", payloadJson = gson.toJson(mapOf("playlistId" to playlistId, "songId" to songId)))
            )
        }
    }

    suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylist(playlistId)
        try {
            echoStreamApi.deletePlaylist(playlistId)
        } catch (e: Exception) {
            // Deleted locally
        }
    }
}
