package com.echostream.app.data.model

data class PendingAction(
    val type: String, // 'LIKE', 'UNLIKE', 'HISTORY', 'CREATE_PLAYLIST'
    val payload: Map<String, Any>
)

data class SyncRequest(
    val pendingActions: List<PendingAction>
)

data class SyncResponse(
    val message: String? = null,
    val serverTime: String? = null,
    val likedSongIds: List<String> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val historySongIds: List<String> = emptyList()
)
