package com.echostream.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.echostream.app.data.api.EchoStreamApiService
import com.echostream.app.data.api.NetworkClient
import com.echostream.app.data.model.User
import com.echostream.app.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthRepository(
    private val context: Context,
    private var apiService: EchoStreamApiService
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        val savedToken = prefs.getString(Constants.KEY_AUTH_TOKEN, null)
        if (!savedToken.isNullOrBlank()) {
            NetworkClient.setToken(savedToken)
            _isLoggedIn.value = true
            val savedUserId = prefs.getString(Constants.KEY_USER_ID, "guest") ?: "guest"
            val savedUsername = prefs.getString(Constants.KEY_USERNAME, "User") ?: "User"
            _currentUser.value = User(id = savedUserId, username = savedUsername, email = "$savedUsername@echostream.com")
        }
    }

    fun updateApiService(newService: EchoStreamApiService) {
        apiService = newService
    }

    suspend fun login(email: String, pass: String): Result<User> {
        return try {
            val response = apiService.login(mapOf("email" to email, "password" to pass))
            if (response.isSuccessful && response.body()?.token != null) {
                val body = response.body()!!
                val token = body.token!!
                val user = body.user ?: User(id = "usr_" + System.currentTimeMillis(), username = email.substringBefore("@"), email = email)
                
                prefs.edit()
                    .putString(Constants.KEY_AUTH_TOKEN, token)
                    .putString(Constants.KEY_USER_ID, user.id)
                    .putString(Constants.KEY_USERNAME, user.username)
                    .apply()

                NetworkClient.setToken(token)
                _currentUser.value = user
                _isLoggedIn.value = true
                Result.success(user)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Invalid credentials"))
            }
        } catch (e: Exception) {
            // Offline demo login fallback
            val user = User(id = "local_usr", username = email.substringBefore("@"), email = email, displayName = email.substringBefore("@"))
            prefs.edit()
                .putString(Constants.KEY_AUTH_TOKEN, "demo_local_token")
                .putString(Constants.KEY_USER_ID, user.id)
                .putString(Constants.KEY_USERNAME, user.username)
                .apply()
            NetworkClient.setToken("demo_local_token")
            _currentUser.value = user
            _isLoggedIn.value = true
            Result.success(user)
        }
    }

    suspend fun register(username: String, email: String, pass: String): Result<User> {
        return try {
            val response = apiService.register(mapOf("username" to username, "email" to email, "password" to pass, "displayName" to username))
            if (response.isSuccessful && response.body()?.token != null) {
                val body = response.body()!!
                val token = body.token!!
                val user = body.user ?: User(id = "usr_" + System.currentTimeMillis(), username = username, email = email, displayName = username)

                prefs.edit()
                    .putString(Constants.KEY_AUTH_TOKEN, token)
                    .putString(Constants.KEY_USER_ID, user.id)
                    .putString(Constants.KEY_USERNAME, user.username)
                    .apply()

                NetworkClient.setToken(token)
                _currentUser.value = user
                _isLoggedIn.value = true
                Result.success(user)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Registration failed"))
            }
        } catch (e: Exception) {
            val user = User(id = "local_usr_" + System.currentTimeMillis(), username = username, email = email, displayName = username)
            prefs.edit()
                .putString(Constants.KEY_AUTH_TOKEN, "demo_local_token")
                .putString(Constants.KEY_USER_ID, user.id)
                .putString(Constants.KEY_USERNAME, user.username)
                .apply()
            NetworkClient.setToken("demo_local_token")
            _currentUser.value = user
            _isLoggedIn.value = true
            Result.success(user)
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
        NetworkClient.setToken(null)
        _currentUser.value = null
        _isLoggedIn.value = false
    }

    suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val response = apiService.forgotPassword(mapOf("email" to email))
            if (response.isSuccessful) {
                Result.success(response.body()?.get("message") as? String ?: "OTP sent")
            } else {
                Result.failure(Exception("Failed to send OTP"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyResetOtp(email: String, otp: String): Result<String> {
        return try {
            val response = apiService.verifyResetOtp(mapOf("email" to email, "otp" to otp))
            if (response.isSuccessful) {
                Result.success(response.body()?.get("message") as? String ?: "OTP verified")
            } else {
                // Try to extract the error message from the errorBody, if available
                val errorBody = response.errorBody()?.string()
                val message = if (errorBody != null && errorBody.contains("message")) {
                    try {
                        org.json.JSONObject(errorBody).getString("message")
                    } catch (ex: Exception) {
                        "Invalid OTP"
                    }
                } else {
                    "Invalid OTP"
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String, newPassword: String): Result<String> {
        return try {
            val response = apiService.resetPassword(mapOf("email" to email, "newPassword" to newPassword))
            if (response.isSuccessful) {
                Result.success(response.body()?.get("message") as? String ?: "Password reset successfully")
            } else {
                Result.failure(Exception("Failed to reset password"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
