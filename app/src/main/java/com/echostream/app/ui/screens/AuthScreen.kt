package com.echostream.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echostream.app.ui.theme.BackgroundDark
import com.echostream.app.ui.theme.CardBackground
import com.echostream.app.ui.theme.ErrorRed
import com.echostream.app.ui.theme.PrimaryViolet
import com.echostream.app.ui.theme.TextMuted
import com.echostream.app.ui.theme.TextPrimary
import com.echostream.app.viewmodel.AuthViewModel

enum class AuthState {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD,
    VERIFY_OTP,
    RESET_PASSWORD
}

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    var authState by remember { mutableStateOf(AuthState.LOGIN) }
    
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Back Button for OTP flows
        if (authState != AuthState.LOGIN && authState != AuthState.REGISTER) {
            IconButton(
                onClick = { 
                    viewModel.clearError()
                    authState = AuthState.LOGIN 
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Logo",
                tint = PrimaryViolet,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val title = when (authState) {
                AuthState.LOGIN -> "Welcome to EchoStream"
                AuthState.REGISTER -> "Create Account"
                AuthState.FORGOT_PASSWORD -> "Reset Password"
                AuthState.VERIFY_OTP -> "Enter OTP"
                AuthState.RESET_PASSWORD -> "New Password"
            }
            
            val subtitle = when (authState) {
                AuthState.LOGIN -> "Log in to access your music & playlists"
                AuthState.REGISTER -> "Sign up to start streaming"
                AuthState.FORGOT_PASSWORD -> "Enter your email to receive a reset code"
                AuthState.VERIFY_OTP -> "Enter the 6-digit code sent to your email"
                AuthState.RESET_PASSWORD -> "Create a new strong password"
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Common TextField Colors
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = CardBackground,
                focusedLabelColor = PrimaryViolet,
                unfocusedLabelColor = TextMuted,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )

            if (authState == AuthState.REGISTER) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (authState == AuthState.LOGIN || authState == AuthState.REGISTER || authState == AuthState.FORGOT_PASSWORD) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (authState == AuthState.VERIFY_OTP) {
                OutlinedTextField(
                    value = otp,
                    onValueChange = { otp = it.take(6) },
                    label = { Text("6-Digit OTP") },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (authState == AuthState.LOGIN || authState == AuthState.REGISTER) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (authState == AuthState.RESET_PASSWORD) {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!errorMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorRed
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val buttonText = when (authState) {
                AuthState.LOGIN -> "Log In"
                AuthState.REGISTER -> "Sign Up"
                AuthState.FORGOT_PASSWORD -> "Send OTP"
                AuthState.VERIFY_OTP -> "Verify OTP"
                AuthState.RESET_PASSWORD -> "Reset Password"
            }

            Button(
                onClick = {
                    when (authState) {
                        AuthState.REGISTER -> {
                            viewModel.register(username, email, password, onAuthSuccess)
                        }
                        AuthState.LOGIN -> {
                            viewModel.login(email, password, onAuthSuccess)
                        }
                        AuthState.FORGOT_PASSWORD -> {
                            viewModel.forgotPassword(email) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                authState = AuthState.VERIFY_OTP
                            }
                        }
                        AuthState.VERIFY_OTP -> {
                            viewModel.verifyResetOtp(email, otp) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                authState = AuthState.RESET_PASSWORD
                            }
                        }
                        AuthState.RESET_PASSWORD -> {
                            viewModel.resetPassword(email, newPassword) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                authState = AuthState.LOGIN
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                shape = RoundedCornerShape(24.dp),
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (authState == AuthState.LOGIN) {
                Text(
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.clickable {
                        viewModel.clearError()
                        authState = AuthState.FORGOT_PASSWORD
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (authState == AuthState.LOGIN || authState == AuthState.REGISTER) {
                Text(
                    text = if (authState == AuthState.REGISTER) "Already have an account? Log In" else "Don't have an account? Sign Up",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryViolet,
                    modifier = Modifier.clickable {
                        viewModel.clearError()
                        authState = if (authState == AuthState.REGISTER) AuthState.LOGIN else AuthState.REGISTER
                    }
                )
            }
        }
    }
}
