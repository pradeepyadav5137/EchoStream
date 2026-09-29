package com.echostream.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echostream.app.data.model.User
import com.echostream.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authRepository.login(email, pass)
            _isLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Login failed"
            }
        }
    }

    fun register(username: String, email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authRepository.register(username, email, pass)
            _isLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Registration failed"
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun forgotPassword(email: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authRepository.forgotPassword(email)
            _isLoading.value = false
            if (result.isSuccess) {
                onSuccess(result.getOrNull() ?: "OTP sent")
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to send OTP"
            }
        }
    }

    fun verifyResetOtp(email: String, otp: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authRepository.verifyResetOtp(email, otp)
            _isLoading.value = false
            if (result.isSuccess) {
                onSuccess(result.getOrNull() ?: "OTP verified")
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Invalid OTP"
            }
        }
    }

    fun resetPassword(email: String, newPassword: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authRepository.resetPassword(email, newPassword)
            _isLoading.value = false
            if (result.isSuccess) {
                onSuccess(result.getOrNull() ?: "Password reset successful")
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to reset password"
            }
        }
    }
}
