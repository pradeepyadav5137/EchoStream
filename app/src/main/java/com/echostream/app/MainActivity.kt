package com.echostream.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.echostream.app.player.PlaybackService
import com.echostream.app.ui.navigation.EchoStreamNavGraph
import com.echostream.app.ui.theme.EchoStreamTheme
import com.echostream.app.viewmodel.AuthViewModel
import com.echostream.app.viewmodel.HomeViewModel
import com.echostream.app.viewmodel.LibraryViewModel
import com.echostream.app.viewmodel.PlayerViewModel
import com.echostream.app.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as EchoStreamApplication

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Initialize ViewModels
        val authViewModel = AuthViewModel(app.authRepository)
        val homeViewModel = HomeViewModel(app.musicRepository)
        val searchViewModel = SearchViewModel(app.musicRepository)
        val libraryViewModel = LibraryViewModel(app.musicRepository, app.playlistRepository)
        val playerViewModel = PlayerViewModel(app.playerManager, app.musicRepository, app.downloadRepository, app.playlistRepository)

        setContent {
            EchoStreamTheme {
                val navController = rememberNavController()
                val isOnline by app.networkMonitor.isOnline.collectAsState(initial = true)
                var baseUrl by remember { mutableStateOf(app.currentBaseUrl) }
                val scope = rememberCoroutineScope()

                EchoStreamNavGraph(
                    navController = navController,
                    authViewModel = authViewModel,
                    homeViewModel = homeViewModel,
                    searchViewModel = searchViewModel,
                    libraryViewModel = libraryViewModel,
                    playerViewModel = playerViewModel,
                    isOnline = isOnline,
                    currentBaseUrl = baseUrl,
                    onPerformSync = {
                        scope.launch {
                            val result = app.syncRepository.performSync()
                            Toast.makeText(
                                this@MainActivity,
                                result.getOrElse { it.message ?: "Sync completed" },
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onUpdateBaseUrl = { newUrl ->
                        app.updateBaseUrl(newUrl)
                        baseUrl = newUrl
                        Toast.makeText(this@MainActivity, "Updated API Base URL to $newUrl", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
