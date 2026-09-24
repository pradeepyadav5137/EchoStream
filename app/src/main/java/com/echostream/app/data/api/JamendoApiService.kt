package com.echostream.app.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class JamendoMusicInfo(
    val tags: JamendoTags? = null
)

data class JamendoTags(
    val genres: List<String>? = null
)

data class JamendoTrack(
    val id: String,
    val name: String,
    val duration: Int,
    val artist_id: String? = null,
    val artist_name: String,
    val album_id: String? = null,
    val album_name: String? = null,
    val album_image: String? = null,
    val image: String? = null,
    val audio: String? = null,
    val audiodownload: String? = null,
    val musicinfo: JamendoMusicInfo? = null
)

data class JamendoResponse(
    val results: List<JamendoTrack> = emptyList()
)

interface JamendoApiService {
    @GET("tracks/")
    suspend fun getTracks(
        @Query("client_id") clientId: String = "56b49247",
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null,
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("include") include: String = "musicinfo"
    ): Response<JamendoResponse>
}
