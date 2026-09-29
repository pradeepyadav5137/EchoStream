package com.echostream.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.echostream.app.ui.components.BottomNavBar
import com.echostream.app.ui.components.CommentSheet
import com.echostream.app.ui.components.FullPlayerModal
import com.echostream.app.ui.components.MiniPlayer
import com.echostream.app.ui.components.OfflineIndicator
import com.echostream.app.ui.components.PlaylistSelectionSheet
import com.echostream.app.ui.screens.AuthScreen
import com.echostream.app.ui.screens.HomeScreen
import com.echostream.app.ui.screens.LibraryScreen
import com.echostream.app.ui.screens.PlaylistDetailScreen
import com.echostream.app.ui.screens.ProfileScreen
import com.echostream.app.ui.screens.SearchScreen
import com.echostream.app.ui.screens.SplashScreen
import com.echostream.app.viewmodel.AuthViewModel
import com.echostream.app.viewmodel.HomeViewModel
import com.echostream.app.viewmodel.LibraryViewModel
import com.echostream.app.viewmodel.PlayerViewModel
import com.echostream.app.viewmodel.SearchViewModel

@Composable
fun EchoStreamNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    searchViewModel: SearchViewModel,
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    isOnline: Boolean,
    currentBaseUrl: String,
    onPerformSync: () -> Unit,
    onUpdateBaseUrl: (String) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val allSongs by homeViewModel.allSongs.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    val isShuffle by playerViewModel.isShuffle.collectAsState()
    val repeatMode by playerViewModel.repeatMode.collectAsState()
    val showFullPlayer by playerViewModel.showFullPlayer.collectAsState()
    val lyrics by playerViewModel.lyrics.collectAsState()
    val comments by playerViewModel.comments.collectAsState()
    val showCommentsSheet by playerViewModel.showCommentsSheet.collectAsState()
    val showPlaylistSheet by playerViewModel.showPlaylistSheet.collectAsState()
    val playlists by playerViewModel.playlists.collectAsState()

    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.Library.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column {
                    if (currentSong != null && !showFullPlayer) {
                        MiniPlayer(
                            song = currentSong!!,
                            isPlaying = isPlaying,
                            progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                            onPlayPauseClick = { playerViewModel.togglePlayPause() },
                            onNextClick = { playerViewModel.playNext() },
                            onLikeClick = { playerViewModel.toggleLike() },
                            onPlayerClick = { playerViewModel.setFullPlayerVisible(true) }
                        )
                    }
                    BottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isOnline) {
                    OfflineIndicator()
                }

                NavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Screen.Splash.route) {
                        SplashScreen(
                            isLoggedIn = isLoggedIn,
                            onNavigateNext = { loggedIn ->
                                if (loggedIn) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Screen.Auth.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(Screen.Auth.route) {
                        AuthScreen(
                            viewModel = authViewModel,
                            onAuthSuccess = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Auth.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onSongSelect = { song, queue ->
                                playerViewModel.playSong(song, queue)
                            },
                            onSearchClick = {
                                navController.navigate(Screen.Search.route)
                            }
                        )
                    }

                    composable(Screen.Search.route) {
                        SearchScreen(
                            viewModel = searchViewModel,
                            onSongSelect = { song, queue ->
                                playerViewModel.playSong(song, queue)
                            }
                        )
                    }

                    composable(Screen.Library.route) {
                        LibraryScreen(
                            viewModel = libraryViewModel,
                            onSongSelect = { song, queue ->
                                playerViewModel.playSong(song, queue)
                            },
                            onPlaylistClick = { playlistId ->
                                navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                            }
                        )
                    }

                    composable(Screen.Profile.route) {
                        val totalTime by homeViewModel.totalListeningTime.collectAsState()
                        val todayTime by homeViewModel.todayListeningTime.collectAsState()
                        val mostPlayed by homeViewModel.mostPlayedSong.collectAsState()

                        ProfileScreen(
                            currentUser = currentUser,
                            isOnline = isOnline,
                            currentBaseUrl = currentBaseUrl,
                            totalTime = totalTime,
                            todayTime = todayTime,
                            mostPlayedSong = mostPlayed,
                            onPerformSync = onPerformSync,
                            onUpdateBaseUrl = onUpdateBaseUrl,
                            onLogout = {
                                authViewModel.logout()
                                navController.navigate(Screen.Auth.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(
                        route = Screen.PlaylistDetail.route,
                        arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                        val playlistSongs by libraryViewModel.getPlaylistSongs(playlistId).collectAsState(initial = emptyList())
                        val playlists by libraryViewModel.playlists.collectAsState()
                        val playlistName = playlists.find { it.id == playlistId }?.name ?: "Playlist"
                        
                        PlaylistDetailScreen(
                            playlistName = playlistName,
                            songs = playlistSongs,
                            onBack = { navController.popBackStack() },
                            onPlayAll = {
                                if (playlistSongs.isNotEmpty()) {
                                    playerViewModel.playSong(playlistSongs.first(), playlistSongs)
                                }
                            },
                            onSongSelect = { song, queue ->
                                playerViewModel.playSong(song, queue)
                            },
                            onLikeClick = { song ->
                                homeViewModel.toggleLike(song)
                            }
                        )
                    }

                    composable(
                        route = Screen.ArtistDetail.route,
                        arguments = listOf(navArgument("artistId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
                        
                        val artistName = java.net.URLDecoder.decode(artistId, "UTF-8")
                        
                        val artistSongs by androidx.compose.runtime.produceState<List<com.echostream.app.data.model.Song>>(initialValue = emptyList(), key1 = artistName) {
                            value = homeViewModel.allSongs.value.filter { it.artist.contains(artistName, ignoreCase = true) }
                            if (value.isEmpty()) {
                                value = homeViewModel.trendingSongs.value.take(5)
                            }
                        }
                        
                        com.echostream.app.ui.screens.ArtistDetailScreen(
                            artist = com.echostream.app.data.model.Artist(
                                id = artistId,
                                name = artistName,
                                imageUrl = artistSongs.firstOrNull()?.artworkUrl ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500",
                                followersCount = 125000,
                                isFollowed = false
                            ),
                            songs = artistSongs,
                            onBack = { navController.popBackStack() },
                            onSongSelect = { song, queue ->
                                playerViewModel.playSong(song, queue)
                            },
                            onLikeClick = { song ->
                                homeViewModel.toggleLike(song)
                            }
                        )
                    }
                }
            }

            // Full Player Modal Overlay
            if (showFullPlayer && currentSong != null) {
                FullPlayerModal(
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPosition,
                    durationMs = duration,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    lyrics = lyrics,
                    onClose = { playerViewModel.setFullPlayerVisible(false) },
                    onPlayPause = { playerViewModel.togglePlayPause() },
                    onNext = { playerViewModel.playNext() },
                    onPrevious = { playerViewModel.playPrevious() },
                    onSeek = { position -> playerViewModel.seekTo(position) },
                    onLike = { playerViewModel.toggleLike() },
                    onDownload = { playerViewModel.downloadSong() },
                    onToggleShuffle = { playerViewModel.toggleShuffle() },
                    onToggleRepeat = { playerViewModel.toggleRepeat() },
                    onOpenComments = { playerViewModel.setShowCommentsSheet(true) },
                    onAddToPlaylist = { playerViewModel.setShowPlaylistSheet(true) }
                )
            }

            // Comments Bottom Sheet Overlay
            if (showCommentsSheet) {
                CommentSheet(
                    comments = comments,
                    onAddComment = { text -> playerViewModel.addComment(text) },
                    onDismiss = { playerViewModel.setShowCommentsSheet(false) }
                )
            }

            // Playlist Selection Sheet Overlay
            if (showPlaylistSheet) {
                PlaylistSelectionSheet(
                    playlists = playlists,
                    onPlaylistSelect = { playlistId ->
                        playerViewModel.addSongToPlaylist(playlistId)
                    },
                    onCreatePlaylist = { name ->
                        libraryViewModel.createPlaylist(name)
                    },
                    onDismiss = { playerViewModel.setShowPlaylistSheet(false) }
                )
            }
        }
    }
}
