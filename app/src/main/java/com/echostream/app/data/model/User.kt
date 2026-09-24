package com.echostream.app.data.model

data class User(
    val id: String,
    val username: String,
    val email: String,
    val displayName: String = "",
    val avatarUrl: String = ""
)

data class AuthResponse(
    val message: String? = null,
    val token: String? = null,
    val user: User? = null,
    val error: String? = null
)
