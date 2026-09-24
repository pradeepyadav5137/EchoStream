package com.echostream.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.echostream.app.data.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val album: String,
    val albumId: String,
    val genre: String,
    val duration: Int,
    val audioUrl: String,
    val artworkUrl: String,
    val playCount: Int,
    val likeCount: Int,
    val isTrending: Boolean,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toSong(isLiked: Boolean = false, isDownloaded: Boolean = false, localFilePath: String? = null): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            artistId = artistId,
            album = album,
            albumId = albumId,
            genre = genre,
            duration = duration,
            audioUrl = audioUrl,
            artworkUrl = artworkUrl,
            playCount = playCount,
            likeCount = likeCount,
            isTrending = isTrending,
            isLiked = isLiked,
            isDownloaded = isDownloaded,
            localFilePath = localFilePath
        )
    }

    companion object {
        fun fromSong(song: Song): SongEntity {
            return SongEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                artistId = song.artistId,
                album = song.album,
                albumId = song.albumId,
                genre = song.genre,
                duration = song.duration,
                audioUrl = song.audioUrl,
                artworkUrl = song.artworkUrl,
                playCount = song.playCount,
                likeCount = song.likeCount,
                isTrending = song.isTrending
            )
        }
    }
}
