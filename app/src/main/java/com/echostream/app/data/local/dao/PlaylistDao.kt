package com.echostream.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.echostream.app.data.local.entity.PlaylistEntity
import com.echostream.app.data.local.entity.PlaylistSongEntity
import com.echostream.app.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    suspend fun getAllPlaylistsSync(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_songs")
    fun getAllPlaylistSongs(): Flow<List<PlaylistSongEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<PlaylistEntity>)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(playlistSong: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.addedAt ASC
    """)
    fun getSongsForPlaylist(playlistId: String): Flow<List<SongEntity>>

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.addedAt ASC
    """)
    suspend fun getSongsForPlaylistSync(playlistId: String): List<SongEntity>

    @Query("DELETE FROM playlists")
    suspend fun clearAllPlaylists()

    @Query("DELETE FROM playlist_songs")
    suspend fun clearAllPlaylistSongs()
}
