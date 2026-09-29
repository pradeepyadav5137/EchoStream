package com.echostream.app

import android.app.Application
import com.echostream.app.data.api.NetworkClient
import com.echostream.app.data.local.AppDatabase
import com.echostream.app.data.repository.AuthRepository
import com.echostream.app.data.repository.DownloadRepository
import com.echostream.app.data.repository.MusicRepository
import com.echostream.app.data.repository.PlaylistRepository
import com.echostream.app.data.repository.SyncRepository
import com.echostream.app.player.PlayerManager
import com.echostream.app.utils.Constants
import com.echostream.app.utils.NetworkMonitor

class EchoStreamApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var playerManager: PlayerManager
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var musicRepository: MusicRepository
        private set

    lateinit var playlistRepository: PlaylistRepository
        private set

    lateinit var syncRepository: SyncRepository
        private set

    lateinit var downloadRepository: DownloadRepository
        private set

    var currentBaseUrl: String = Constants.DEFAULT_BASE_URL
        private set

    override fun onCreate() {
        super.onCreate()

        val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        currentBaseUrl = prefs.getString(Constants.KEY_BASE_URL, Constants.DEFAULT_BASE_URL) ?: Constants.DEFAULT_BASE_URL

        database = AppDatabase.getDatabase(this)
        playerManager = PlayerManager.getInstance(this).apply {
            updateBaseUrl(currentBaseUrl)
        }
        networkMonitor = NetworkMonitor(this)

        val echoStreamApi = NetworkClient.getEchoStreamService(this, currentBaseUrl)
        val jamendoApi = NetworkClient.getJamendoService()

        authRepository = AuthRepository(this, echoStreamApi)

        musicRepository = MusicRepository(
            echoStreamApi = echoStreamApi,
            jamendoApi = jamendoApi,
            songDao = database.songDao(),
            likeDao = database.likeDao(),
            historyDao = database.historyDao(),
            downloadDao = database.downloadDao(),
            pendingSyncDao = database.pendingSyncDao()
        )

        playlistRepository = PlaylistRepository(
            echoStreamApi = echoStreamApi,
            playlistDao = database.playlistDao(),
            songDao = database.songDao(),
            pendingSyncDao = database.pendingSyncDao()
        )

        syncRepository = SyncRepository(
            echoStreamApi = echoStreamApi,
            pendingSyncDao = database.pendingSyncDao(),
            likeDao = database.likeDao(),
            playlistDao = database.playlistDao(),
            historyDao = database.historyDao()
        )

        downloadRepository = DownloadRepository(
            context = this,
            downloadDao = database.downloadDao(),
            echoStreamApi = echoStreamApi
        )
    }

    fun updateBaseUrl(newUrl: String) {
        currentBaseUrl = newUrl
        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(Constants.KEY_BASE_URL, newUrl)
            .apply()

        playerManager.updateBaseUrl(newUrl)
        val newApi = NetworkClient.getEchoStreamService(this, newUrl)
        authRepository.updateApiService(newApi)
        musicRepository.updateApiService(newApi)
        playlistRepository.updateApiService(newApi)
        syncRepository.updateApiService(newApi)
        downloadRepository.updateApiService(newApi)
    }
}
