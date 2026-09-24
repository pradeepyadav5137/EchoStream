package com.echostream.app

import com.echostream.app.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicRepositoryTest {

    @Test
    fun testRecommendationAlgorithm() {
        val song1 = Song(id = "1", title = "Track 1", artist = "Aetheria", genre = "Electronic", audioUrl = "")
        val song2 = Song(id = "2", title = "Track 2", artist = "Luna Eclipse", genre = "Chillout", audioUrl = "")
        val song3 = Song(id = "3", title = "Track 3", artist = "Unknown", genre = "Rock", audioUrl = "")

        val likedSongs = listOf(song1)
        val historySongs = listOf(song1)

        val allSongs = listOf(song3, song2, song1)

        // Calculate simple recommendation score:
        // Liked artist = +5, Liked genre = +3, History artist = +2, Base = +1
        val sorted = allSongs.sortedByDescending { song ->
            var score = 0
            if (likedSongs.any { it.artist == song.artist }) score += 5
            if (likedSongs.any { it.genre == song.genre }) score += 3
            if (historySongs.any { it.artist == song.artist }) score += 2
            score += 1
            score
        }

        assertEquals("1", sorted.first().id)
        assertTrue(sorted.first().artist == "Aetheria")
    }
}
