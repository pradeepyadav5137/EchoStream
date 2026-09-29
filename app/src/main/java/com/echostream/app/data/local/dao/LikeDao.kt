package com.echostream.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.echostream.app.data.local.entity.LikeEntity
import com.echostream.app.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LikeDao {
    @Query("SELECT * FROM likes ORDER BY likedAt DESC")
    fun getAllLikes(): Flow<List<LikeEntity>>

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN likes l ON s.id = l.songId
        ORDER BY l.likedAt DESC
    """)
    fun getLikedSongs(): Flow<List<SongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM likes WHERE songId = :songId)")
    fun isSongLiked(songId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM likes WHERE songId = :songId)")
    suspend fun isSongLikedDirect(songId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity)

    @Query("DELETE FROM likes WHERE songId = :songId")
    suspend fun deleteLike(songId: String)

    @Query("DELETE FROM likes")
    suspend fun clearAll()
}
