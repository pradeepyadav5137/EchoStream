package com.echostream.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.echostream.app.data.model.Album
import com.echostream.app.data.model.Song
import com.echostream.app.ui.components.SongListItem
import com.echostream.app.ui.theme.BackgroundDark
import com.echostream.app.ui.theme.PrimaryViolet
import com.echostream.app.ui.theme.TextMuted
import com.echostream.app.ui.theme.TextPrimary

@Composable
fun AlbumDetailScreen(
    album: Album,
    songs: List<Song>,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onSongSelect: (Song, List<Song>) -> Unit,
    onLikeClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = album.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = album.artworkUrl,
                        contentDescription = album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = album.title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text(text = "${album.artist} • ${album.releaseYear}", style = MaterialTheme.typography.bodyMedium, color = TextMuted)

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        shape = CircleShape
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play All", tint = TextPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play All", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                }
            }

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
