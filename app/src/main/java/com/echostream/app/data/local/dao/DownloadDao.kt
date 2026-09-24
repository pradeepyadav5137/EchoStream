package com.echostream.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.echostream.app.data.local.entity.DownloadEntity
import com.echostream.app.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN downloads d ON s.id = d.songId
        ORDER BY d.downloadedAt DESC
    """)
    fun getDownloadedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM downloads WHERE songId = :songId LIMIT 1")
    suspend fun getDownloadBySongId(songId: String): DownloadEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE songId = :songId)")
    fun isSongDownloaded(songId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE songId = :songId")
    suspend fun deleteDownload(songId: String)
}
