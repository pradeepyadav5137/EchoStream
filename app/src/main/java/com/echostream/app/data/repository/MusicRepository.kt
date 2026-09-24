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
            val response = echoStreamApi.search("top global hits", 1, 20)
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
                        genre = "Global",
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
        try {
            echoStreamApi.addHistory(mapOf("songId" to song.id, "durationPlayed" to durationPlayed))
        } catch (e: Exception) {
            pendingSyncDao.insertPendingAction(
                PendingSyncEntity(type = "HISTORY", payloadJson = gson.toJson(mapOf("songId" to song.id, "durationPlayed" to durationPlayed)))
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
            plainLyrics = "Mujhko jitna bataye koi\nMai utna hi bhoolu\nBaton me teri aane laga hu\nKuch toh hua hai mujhko\nKesariya tera ishq hai piya\nRang jaau jo mai hath lagau",
            syncedLyrics = listOf(
                LyricsLine(0, "Mujhko jitna bataye koi"),
                LyricsLine(4000, "Mai utna hi bhoolu"),
                LyricsLine(8000, "Baton me teri aane laga hu"),
                LyricsLine(12000, "Kuch toh hua hai mujhko"),
                LyricsLine(16000, "Kesariya tera ishq hai piya"),
                LyricsLine(20000, "Rang jaau jo mai hath lagau")
            )
        )
    }

    suspend fun search(query: String, page: Int = 1): Flow<List<Song>> = flow {
        try {
            val response = echoStreamApi.search(query, page, 20)
            if (response.isSuccessful) {
                val body = response.body()
                val results = body?.get("results") as? List<Map<String, Any>> ?: emptyList()
                val mappedSongs = results.map { r ->
                    Song(
                        id = r["id"] as? String ?: "",
                        title = r["title"] as? String ?: "Unknown Title",
                        artist = r["artist"] as? String ?: "Unknown Artist",
                        artistId = "unknown",
                        album = r["album"] as? String ?: "Unknown Album",
                        albumId = "unknown",
                        genre = "Unknown",
                        duration = (r["duration"] as? Double)?.toInt() ?: 0,
                        audioUrl = "resolve://${r["id"]}",
                        artworkUrl = r["thumbnail"] as? String ?: "",
                        playCount = 0,
                        likeCount = 0,
                        isTrending = false
                    )
                }
                emit(mappedSongs)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    // Simple rule-based recommendations
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
}
