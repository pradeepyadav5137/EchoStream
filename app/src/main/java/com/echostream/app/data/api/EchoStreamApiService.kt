package com.echostream.app.data.api

import com.echostream.app.data.model.AuthResponse
import com.echostream.app.data.model.Playlist
import com.echostream.app.data.model.Song
import com.echostream.app.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface EchoStreamApiService {
    // Auth
    @POST("api/auth/register")
    suspend fun register(@Body body: Map<String, String>): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: Map<String, String>): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<User>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Map<String, String>>

    // Music (Search and Details)
    @GET("api/music/search")
    suspend fun search(@Query("q") query: String, @Query("page") page: Int = 1, @Query("limit") limit: Int = 20): Response<Map<String, Any>>

    @GET("api/music/song/{id}")
    suspend fun getSongById(@Path("id") id: String): Response<Map<String, Any>>

    @GET("api/music/stream/{id}")
    suspend fun getStreamUrl(@Path("id") id: String): Response<Map<String, Any>>

    // Playlists
    @GET("api/playlists")
    suspend fun getPlaylists(): Response<Map<String, Any>>

    @POST("api/playlists")
    suspend fun createPlaylist(@Body body: Map<String, Any>): Response<Map<String, Any>>

    @GET("api/playlists/{id}")
    suspend fun getPlaylistById(@Path("id") id: String): Response<Map<String, Any>>

    @PATCH("api/playlists/{id}")
    suspend fun updatePlaylist(@Path("id") id: String, @Body body: Map<String, Any>): Response<Map<String, Any>>

    @DELETE("api/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: String): Response<Map<String, Any>>

    @POST("api/playlists/{id}/songs")
    suspend fun addSongToPlaylist(@Path("id") id: String, @Body body: Map<String, Any>): Response<Map<String, Any>>

    @DELETE("api/playlists/{id}/songs/{songId}")
    suspend fun removeSongFromPlaylist(@Path("id") id: String, @Path("songId") songId: String): Response<Map<String, Any>>

    // Favorites
    @GET("api/favorites")
    suspend fun getFavorites(): Response<Map<String, Any>>

    @POST("api/favorites/{songId}")
    suspend fun addFavorite(@Path("songId") songId: String, @Body body: Map<String, Any>): Response<Map<String, Any>>

    @DELETE("api/favorites/{songId}")
    suspend fun removeFavorite(@Path("songId") songId: String): Response<Map<String, Any>>

    // History
    @GET("api/history")
    suspend fun getHistory(): Response<Map<String, Any>>

    @POST("api/history")
    suspend fun addHistory(@Body body: Map<String, Any>): Response<Map<String, Any>>

    @DELETE("api/history")
    suspend fun clearHistory(): Response<Map<String, Any>>
}
