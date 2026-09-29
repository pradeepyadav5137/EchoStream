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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.echostream.app.data.model.Artist
import com.echostream.app.data.model.Song
import com.echostream.app.ui.components.SongListItem
import com.echostream.app.ui.theme.BackgroundDark
import com.echostream.app.ui.theme.CardBackground
import com.echostream.app.ui.theme.PrimaryViolet
import com.echostream.app.ui.theme.TextMuted
import com.echostream.app.ui.theme.TextPrimary

@Composable
fun ArtistDetailScreen(
    artist: Artist,
    songs: List<Song>,
    onBack: () -> Unit,
    onSongSelect: (Song, List<Song>) -> Unit,
    onLikeClick: (Song) -> Unit
) {
    var isFollowing by remember { mutableStateOf(artist.isFollowed) }

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
            Text(text = artist.name, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
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
                        model = artist.imageUrl,
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = artist.name, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text(text = "${artist.followersCount} Followers", style = MaterialTheme.typography.bodyMedium, color = TextMuted)

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { isFollowing = !isFollowing },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isFollowing) CardBackground else PrimaryViolet),
                        shape = CircleShape
                    ) {
                        Text(if (isFollowing) "Following" else "Follow", color = TextPrimary)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Popular Tracks",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )

                    Button(
                        onClick = { 
                            if (songs.isNotEmpty()) {
                                onSongSelect(songs.first(), songs)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Text("Play All (Radio)", color = TextPrimary)
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
