package com.echostream.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echostream.app.data.model.Song
import com.echostream.app.ui.components.LoadingSkeleton
import com.echostream.app.ui.components.SongCard
import com.echostream.app.ui.components.SongListItem
import com.echostream.app.ui.theme.BackgroundDark
import com.echostream.app.ui.theme.TextMuted
import com.echostream.app.ui.theme.TextPrimary
import com.echostream.app.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSongSelect: (Song, List<Song>) -> Unit,
    onSearchClick: () -> Unit
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val recommendedSongs by viewModel.recommendedSongs.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Good Evening",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "EchoStream",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
            }

            Row {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextPrimary
                    )
                }

                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextPrimary
                    )
                }
            }
        }

        if (isLoading && allSongs.isEmpty()) {
            LoadingSkeleton()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Recently Played Section
                if (recentlyPlayed.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Recently Played")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentlyPlayed) { song ->
                                SongCard(
                                    song = song,
                                    onClick = { onSongSelect(song, recentlyPlayed) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Recommended / Made For You Section
                if (recommendedSongs.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Made For You")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recommendedSongs) { song ->
                                SongCard(
                                    song = song,
                                    onClick = { onSongSelect(song, recommendedSongs) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Trending Section
                if (trendingSongs.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Trending Now 🔥")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(trendingSongs) { song ->
                                SongCard(
                                    song = song,
                                    onClick = { onSongSelect(song, trendingSongs) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // All Tracks Header
                item {
                    SectionHeader(title = "Top Charts & Discover")
                }

                items(allSongs) { song ->
                    SongListItem(
                        song = song,
                        onClick = { onSongSelect(song, allSongs) },
                        onLikeClick = { viewModel.toggleLike(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
        color = TextPrimary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
