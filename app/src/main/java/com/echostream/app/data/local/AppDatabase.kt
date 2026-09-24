package com.echostream.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.echostream.app.data.local.dao.DownloadDao
import com.echostream.app.data.local.dao.HistoryDao
import com.echostream.app.data.local.dao.LikeDao
import com.echostream.app.data.local.dao.PendingSyncDao
import com.echostream.app.data.local.dao.PlaylistDao
import com.echostream.app.data.local.dao.SongDao
import com.echostream.app.data.local.entity.DownloadEntity
import com.echostream.app.data.local.entity.HistoryEntity
import com.echostream.app.data.local.entity.LikeEntity
import com.echostream.app.data.local.entity.PendingSyncEntity
import com.echostream.app.data.local.entity.PlaylistEntity
import com.echostream.app.data.local.entity.PlaylistSongEntity
import com.echostream.app.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        LikeEntity::class,
        HistoryEntity::class,
        PendingSyncEntity::class,
        DownloadEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun likeDao(): LikeDao
    abstract fun historyDao(): HistoryDao
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "echostream_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
