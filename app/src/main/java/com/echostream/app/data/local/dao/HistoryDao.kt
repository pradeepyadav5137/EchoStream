package com.echostream.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.echostream.app.data.local.entity.HistoryEntity
import com.echostream.app.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN history h ON s.id = h.songId
        GROUP BY s.id
        ORDER BY MAX(h.playedAt) DESC
        LIMIT 30
    """)
    fun getRecentlyPlayedSongs(): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Query("SELECT SUM(durationPlayed) FROM history")
    fun getTotalListeningTime(): Flow<Int?>

    @Query("SELECT SUM(durationPlayed) FROM history WHERE playedAt >= :startOfDay")
    fun getTodayListeningTime(startOfDay: Long): Flow<Int?>

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN history h ON s.id = h.songId
        GROUP BY s.id
        ORDER BY COUNT(h.id) DESC
        LIMIT 1
    """)
    fun getMostPlayedSong(): Flow<SongEntity?>
}
