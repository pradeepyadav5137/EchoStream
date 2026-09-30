package com.echostream.app.data.repository

import com.echostream.app.data.api.EchoStreamApiService
import com.echostream.app.data.api.JamendoApiService
import com.echostream.app.data.local.dao.DownloadDao
import com.echostream.app.data.local.dao.HistoryDao
import com.echostream.app.data.local.dao.LikeDao
import com.echostream.app.data.local.dao.PendingSyncDao
import com.echostream.app.data.local.dao.SongDao
import com.echostream.app.data.local.entity.HistoryEntity
import com.echostream.app.data.local.entity.LikeEntity
import com.echostream.app.data.local.entity.PendingSyncEntity
import com.echostream.app.data.local.entity.SongEntity
import com.echostream.app.data.model.Lyrics
import com.echostream.app.data.model.LyricsLine
import com.echostream.app.data.model.Song
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

class MusicRepository(
    private var echoStreamApi: EchoStreamApiService,
    private val jamendoApi: JamendoApiService,
    private val songDao: SongDao,
    private val likeDao: LikeDao,
    private val historyDao: HistoryDao,
    private val downloadDao: DownloadDao,
    private val pendingSyncDao: PendingSyncDao
) {
    private val gson = Gson()

    // Catalog removed

    fun updateApiService(newService: EchoStreamApiService) {
        echoStreamApi = newService
    }

    // Get all songs combined with likes and download states
    fun getSongs(): Flow<List<Song>> {
        return combine(
            songDao.getAllSongs(),
            likeDao.getAllLikes(),
            downloadDao.getAllDownloads()
        ) { localEntities, likes, downloads ->
            val likedIds = likes.map { it.songId }.toSet()
            val downloadMap = downloads.associateBy { it.songId }

            if (localEntities.isNotEmpty()) {
                localEntities.map { entity ->
                    val isLiked = likedIds.contains(entity.id)
                    val download = downloadMap[entity.id]
                    entity.toSong(
                        isLiked = isLiked,
                        isDownloaded = download != null,
                        localFilePath = download?.localAudioPath
                    )
                }
            } else {
                emptyList()
            }
        }
    }

    // Refresh songs from online backend or Jamendo
    suspend fun refreshSongs(): Result<Unit> {
        return try {
            val response = echoStreamApi.search("top punjabi hits 2024", 1, 20)
            if (response.isSuccessful && response.body() != null) {
                val results = response.body()?.get("results") as? List<Map<String, Any>> ?: emptyList()
                val remoteSongs = results.map { r ->
                    Song(
                        id = r["id"] as? String ?: "",
                        title = r["title"] as? String ?: "Unknown Title",
                        artist = r["artist"] as? String ?: "Unknown Artist",
                        artistId = "unknown",
                        album = r["album"] as? String ?: "Unknown Album",
                        albumId = "unknown",
                        genre = "Punjabi",
                        duration = (r["duration"] as? Double)?.toInt() ?: 0,
                        audioUrl = "resolve://${r["id"]}",
                        artworkUrl = r["thumbnail"] as? String ?: "",
                        playCount = 0,
                        likeCount = 0,
                        isTrending = true
                    )
                }
                songDao.clearAll()
                songDao.insertSongs(remoteSongs.map { SongEntity.fromSong(it) })
                Result.success(Unit)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    // Liked songs
    fun getLikedSongs(): Flow<List<Song>> {
        return combine(likeDao.getLikedSongs(), downloadDao.getAllDownloads()) { songEntities, downloads ->
            val downloadMap = downloads.associateBy { it.songId }
            songEntities.map { entity ->
                val download = downloadMap[entity.id]
                entity.toSong(
                    isLiked = true,
                    isDownloaded = download != null,
                    localFilePath = download?.localAudioPath
                )
            }
        }
    }

    // Downloaded songs
    fun getDownloadedSongs(): Flow<List<Song>> {
        return combine(downloadDao.getDownloadedSongs(), likeDao.getAllLikes()) { songEntities, likes ->
            val likedIds = likes.map { it.songId }.toSet()
            songEntities.map { entity ->
                val download = downloadDao.getDownloadBySongId(entity.id)
                entity.toSong(
                    isLiked = likedIds.contains(entity.id),
                    isDownloaded = true,
                    localFilePath = download?.localAudioPath
                )
            }
        }
    }

    // Recently played history
    fun getRecentlyPlayed(): Flow<List<Song>> {
        return combine(historyDao.getRecentlyPlayedSongs(), likeDao.getAllLikes(), downloadDao.getAllDownloads()) { songEntities, likes, downloads ->
            val likedIds = likes.map { it.songId }.toSet()
            val downloadMap = downloads.associateBy { it.songId }
            songEntities.map { entity ->
                val download = downloadMap[entity.id]
                entity.toSong(
                    isLiked = likedIds.contains(entity.id),
                    isDownloaded = download != null,
                    localFilePath = download?.localAudioPath
                )
            }
        }
    }

    // Stats
    fun getTotalListeningTime(): Flow<Int?> = historyDao.getTotalListeningTime()
    
    fun getTodayListeningTime(startOfDay: Long): Flow<Int?> = historyDao.getTodayListeningTime(startOfDay)
    
    fun getMostPlayedSong(): Flow<SongEntity?> = historyDao.getMostPlayedSong()

    // Ensure song is inserted into the local database (used for playlists/history/likes)
    suspend fun insertSongLocally(song: Song) {
        songDao.insertSong(SongEntity.fromSong(song))
    }

    // Like / Unlike song
    suspend fun toggleLike(song: Song): Boolean {
        songDao.insertSong(SongEntity.fromSong(song))
        val newLikedState = !song.isLiked
        if (newLikedState) {
            likeDao.insertLike(LikeEntity(songId = song.id))
            try {
                echoStreamApi.addFavorite(song.id, mapOf(
                    "title" to song.title,
                    "artist" to song.artist,
                    "album" to song.album,
                    "thumbnail" to song.artworkUrl,
                    "duration" to song.duration,
                    "source" to "youtube"
                ))
            } catch (e: Exception) {
                pendingSyncDao.insertPendingAction(
                    PendingSyncEntity(type = "LIKE", payloadJson = gson.toJson(mapOf("songId" to song.id)))
                )
            }
        } else {
            likeDao.deleteLike(song.id)
            try {
                echoStreamApi.removeFavorite(song.id)
            } catch (e: Exception) {
                pendingSyncDao.insertPendingAction(
                    PendingSyncEntity(type = "UNLIKE", payloadJson = gson.toJson(mapOf("songId" to song.id)))
                )
            }
        }
        return newLikedState
    }

    // Record listening history
    suspend fun recordHistory(song: Song, durationPlayed: Int = 0) {
        songDao.insertSong(SongEntity.fromSong(song))
        historyDao.insertHistory(HistoryEntity(songId = song.id, durationPlayed = durationPlayed))
        
        val payload = mapOf(
            "songId" to song.id,
            "title" to song.title,
            "artist" to song.artist,
            "album" to song.album,
            "thumbnail" to song.artworkUrl,
            "duration" to song.duration, // Backend expects full duration as 'duration'
            "durationPlayed" to durationPlayed
        )
        
        try {
            echoStreamApi.addHistory(payload)
        } catch (e: Exception) {
            pendingSyncDao.insertPendingAction(
                PendingSyncEntity(type = "HISTORY", payloadJson = gson.toJson(payload))
            )
        }
    }

    // Get lyrics
    suspend fun getLyrics(songId: String): Lyrics {
        return getDefaultLyrics(songId)
    }

    private fun getDefaultLyrics(songId: String): Lyrics {
        return Lyrics(
            songId = songId,
            plainLyrics = "Lyrics are not available for this track yet.",
            syncedLyrics = emptyList()
        )
    }

    suspend fun search(query: String, limit: Int = 30): Flow<Map<String, List<Any>>> = flow {
        try {
            val response = echoStreamApi.searchAll(query, limit)
            if (response.isSuccessful) {
                val body = response.body()
                
                // Parse songs
                val songsRaw = body?.get("songs") as? List<Map<String, Any>> ?: emptyList()
                val songs = songsRaw.map { mapToSong(it) }
                
                // Parse artists and albums (simplified parsing as Map)
                val artists = body?.get("artists") as? List<Map<String, Any>> ?: emptyList()
                val albums = body?.get("albums") as? List<Map<String, Any>> ?: emptyList()
                
                emit(mapOf(
                    "songs" to songs,
                    "artists" to artists,
                    "albums" to albums
                ))
            } else {
                emit(emptyMap())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyMap())
        }
    }

    private fun mapToSong(r: Map<String, Any>): Song {
        return Song(
            id = r["id"] as? String ?: "",
            title = r["title"] as? String ?: r["name"] as? String ?: "Unknown Title",
            artist = r["artist"] as? String ?: "Unknown Artist",
            artistId = r["artistId"] as? String ?: "unknown",
            album = r["album"] as? String ?: "Unknown Album",
            albumId = r["albumId"] as? String ?: "unknown",
            genre = r["genre"] as? String ?: "Unknown",
            duration = (r["duration"] as? Double)?.toInt() ?: 0,
            audioUrl = "resolve://${r["id"]}",
            artworkUrl = r["thumbnail"] as? String ?: r["imageUrl"] as? String ?: "",
            playCount = (r["playCount"] as? Double)?.toInt() ?: 0,
            likeCount = 0,
            isTrending = false
        )
    }

    // Simple rule-based recommendations fallback
    fun getRecommendations(allSongs: List<Song>, likedSongs: List<Song>, historySongs: List<Song>): List<Song> {
        val likedArtists = likedSongs.map { it.artist }.toSet()
        val likedGenres = likedSongs.map { it.genre }.toSet()
        val historyArtists = historySongs.map { it.artist }.toSet()

        return allSongs.sortedByDescending { song ->
            var score = 0
            if (likedArtists.contains(song.artist)) score += 5
            if (likedGenres.contains(song.genre)) score += 3
            if (historyArtists.contains(song.artist)) score += 2
            score += 1
            score
        }
    }

    suspend fun getArtistSongs(artistName: String): Flow<List<Song>> = flow {
        try {
            val response = echoStreamApi.getArtistSongs(artistName)
            if (response.isSuccessful) {
                val body = response.body()
                val songsRaw = body?.get("songs") as? List<Map<String, Any>> ?: emptyList()
                emit(songsRaw.map { mapToSong(it) })
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun getRecommendedSections(): Flow<List<Map<String, Any>>> = flow {
        try {
            val response = echoStreamApi.getRecommended()
            if (response.isSuccessful) {
                val body = response.body()
                val sections = body?.get("sections") as? List<Map<String, Any>> ?: emptyList()
                
                // Map the inner songs array to Song objects
                val mappedSections = sections.map { section ->
                    val title = section["title"] as? String ?: "Recommended"
                    val songsRaw = section["songs"] as? List<Map<String, Any>> ?: emptyList()
                    mapOf(
                        "title" to title,
                        "songs" to songsRaw.map { mapToSong(it) }
                    )
                }
                emit(mappedSections)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}
