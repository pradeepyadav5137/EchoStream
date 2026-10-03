package com.echostream.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.echostream.app.data.model.Song
import com.echostream.app.ui.theme.AccentPink
import com.echostream.app.ui.theme.PrimaryViolet
import com.echostream.app.ui.theme.TextMuted
import com.echostream.app.ui.theme.TextPrimary
import com.echostream.app.ui.theme.TextSecondary

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick?.invoke() }
            )
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.artworkUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Downloaded",
                        tint = PrimaryViolet,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 4.dp)
                    )
                }
                
                val isUnknownArtist = song.artist.equals("Unknown Artist", ignoreCase = true)
                val isUnknownAlbum = song.album.equals("Unknown Album", ignoreCase = true) || song.album.isEmpty()
                
                val artistText = if (isUnknownArtist) "" else "${song.artist} • "
                val albumText = if (isUnknownAlbum) "Single" else song.album
                
                Text(
                    text = "$artistText$albumText",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onLikeClick) {
            Icon(
                imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Like",
                tint = if (song.isLiked) AccentPink else TextMuted
            )
        }

        if (onMoreClick != null) {
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = TextMuted
                )
            }
        }
    }
}
