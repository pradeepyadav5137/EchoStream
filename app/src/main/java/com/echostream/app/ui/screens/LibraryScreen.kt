package com.echostream.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echostream.app.data.model.Playlist
import com.echostream.app.data.model.Song
import com.echostream.app.ui.components.SongListItem
import com.echostream.app.ui.theme.AccentPink
import com.echostream.app.ui.theme.BackgroundDark
import com.echostream.app.ui.theme.CardBackground
import com.echostream.app.ui.theme.PrimaryViolet
import com.echostream.app.ui.theme.SurfaceDark
import com.echostream.app.ui.theme.TextMuted
import com.echostream.app.ui.theme.TextPrimary
import com.echostream.app.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onSongSelect: (Song, List<Song>) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    val likedSongs by viewModel.likedSongs.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    val tabs = listOf("Liked Songs", "Downloads", "Playlists", "History")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )

            IconButton(onClick = { showCreateDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Playlist",
                    tint = PrimaryViolet
                )
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = SurfaceDark,
            contentColor = PrimaryViolet,
            edgePadding = 16.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTabIndex == index) PrimaryViolet else TextMuted,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTabIndex) {
            0 -> SongListTab(songs = likedSongs, emptyMessage = "No liked songs yet.", onSongSelect = onSongSelect, onLikeClick = { viewModel.toggleLike(it) })
            1 -> SongListTab(songs = downloadedSongs, emptyMessage = "No downloaded songs available offline.", onSongSelect = onSongSelect, onLikeClick = { viewModel.toggleLike(it) })
            2 -> PlaylistsTab(
                playlists = playlists,
                onPlaylistClick = onPlaylistClick,
                onCreateClick = { showCreateDialog = true },
                onDeleteClick = { playlistToDelete = it }
            )
            3 -> SongListTab(songs = recentlyPlayed, emptyMessage = "No recently played tracks.", onSongSelect = onSongSelect, onLikeClick = { viewModel.toggleLike(it) })
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Playlist", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName.trim())
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    if (playlistToDelete != null) {
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("Delete Playlist", color = TextPrimary) },
            text = { Text("Are you sure you want to delete '${playlistToDelete?.name}'?", color = TextMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        playlistToDelete?.let {
                            viewModel.deletePlaylist(it.id)
                        }
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun SongListTab(
    songs: List<Song>,
    emptyMessage: String,
    onSongSelect: (Song, List<Song>) -> Unit,
    onLikeClick: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emptyMessage, style = MaterialTheme.typography.bodyLarge, color = TextMuted)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(songs) { song ->
                SongListItem(
                    song = song,
                    onClick = { onSongSelect(song, songs) },
                    onLikeClick = { onLikeClick(song) }
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlaylistsTab(
    playlists: List<Playlist>,
    onPlaylistClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onDeleteClick: (Playlist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(playlists) { pl ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .combinedClickable(
                        onClick = { onPlaylistClick(pl.id) },
                        onLongClick = { onDeleteClick(pl) }
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = PrimaryViolet,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column {
                    Text(text = pl.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(text = "${pl.songIds.size} songs", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        }
    }
}
