package com.echostream.app.data.repository

import android.content.Context
import com.echostream.app.data.local.dao.DownloadDao
import com.echostream.app.data.local.entity.DownloadEntity
import com.echostream.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DownloadRepository(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val client = OkHttpClient()

    val downloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    fun isSongDownloaded(songId: String): Flow<Boolean> = downloadDao.isSongDownloaded(songId)

    suspend fun downloadSong(song: Song, onProgress: (Int) -> Unit): Result<String> = withContext(Dispatchers.IO) {
        try {
            val audioDir = File(context.filesDir, "audio_downloads").apply { if (!exists()) mkdirs() }
            val audioFile = File(audioDir, "${song.id}.mp3")

            if (audioFile.exists() && audioFile.length() > 0) {
                downloadDao.insertDownload(DownloadEntity(songId = song.id, localAudioPath = audioFile.absolutePath))
                return@withContext Result.success(audioFile.absolutePath)
            }

            val request = Request.Builder().url(song.audioUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful || response.body == null) {
                return@withContext Result.failure(Exception("Failed to download audio file"))
            }

            val body = response.body!!
            val contentLength = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(audioFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (contentLength > 0) {
                            val progress = ((downloadedBytes * 100) / contentLength).toInt()
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            downloadDao.insertDownload(
                DownloadEntity(
                    songId = song.id,
                    localAudioPath = audioFile.absolutePath,
                    localArtworkPath = song.artworkUrl
                )
            )

            Result.success(audioFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDownload(songId: String) = withContext(Dispatchers.IO) {
        val download = downloadDao.getDownloadBySongId(songId)
        if (download != null) {
            val file = File(download.localAudioPath)
            if (file.exists()) {
                file.delete()
            }
            downloadDao.deleteDownload(songId)
        }
    }
}
