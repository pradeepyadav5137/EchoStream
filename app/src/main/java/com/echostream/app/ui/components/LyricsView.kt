package com.echostream.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echostream.app.data.model.Lyrics
import com.echostream.app.ui.theme.PrimaryViolet
import com.echostream.app.ui.theme.TextMuted
import com.echostream.app.ui.theme.TextPrimary

@Composable
fun LyricsView(
    lyrics: Lyrics?,
    currentPositionMs: Long,
    modifier: Modifier = Modifier
) {
    if (lyrics == null || (lyrics.plainLyrics.isEmpty() && lyrics.syncedLyrics.isEmpty())) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No lyrics available for this track.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted
            )
        }
        return
    }

    if (lyrics.syncedLyrics.isNotEmpty()) {
        val listState = rememberLazyListState()

        // Find current highlighted line
        val currentIndex = lyrics.syncedLyrics.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)

        LaunchedEffect(currentIndex) {
            if (currentIndex in lyrics.syncedLyrics.indices) {
                listState.animateScrollToItem(currentIndex)
            }
        }

        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize()
        ) {
            itemsIndexed(lyrics.syncedLyrics) { index, line ->
                val isCurrent = index == currentIndex
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = if (isCurrent) 22.sp else 16.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isCurrent) PrimaryViolet else TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                )
            }
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            item {
                Text(
                    text = lyrics.plainLyrics,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        lineHeight = 28.sp
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}
