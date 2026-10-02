package com.echostream.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.echostream.app.data.model.User
import com.echostream.app.ui.theme.BackgroundDark
import com.echostream.app.ui.theme.CardBackground
import com.echostream.app.ui.theme.ErrorRed
import com.echostream.app.ui.theme.PrimaryViolet
import com.echostream.app.ui.theme.SurfaceDark
import com.echostream.app.ui.theme.TextMuted
import com.echostream.app.ui.theme.TextPrimary
import com.echostream.app.ui.theme.TextSecondary

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun ProfileScreen(
    currentUser: User?,
    isOnline: Boolean,
    currentBaseUrl: String,
    totalTime: Int,
    todayTime: Int,
    mostPlayedSong: com.echostream.app.data.model.Song?,
    onPerformSync: () -> Unit,
    onForceUpload: () -> Unit,
    onUpdateBaseUrl: (String) -> Unit,
    onLogout: () -> Unit
) {
    var showUrlDialog by remember { mutableStateOf(false) }
    var newUrl by remember { mutableStateOf(currentBaseUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Profile Avatar - Cute Boy with Headphones
        AsyncImage(
            model = com.echostream.app.R.drawable.profile_boy,
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "EchoStream Listener",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = TextPrimary
        )

        Text(
            text = currentUser?.email ?: "guest@echostream.com",
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Stats Row in a Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${(totalTime / 60)}m", style = MaterialTheme.typography.headlineMedium, color = PrimaryViolet, fontWeight = FontWeight.Bold)
                    Text(text = "Total Time", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${(todayTime / 60)}m", style = MaterialTheme.typography.headlineMedium, color = PrimaryViolet, fontWeight = FontWeight.Bold)
                    Text(text = "Today", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = mostPlayedSong?.title?.take(10)?.let { if (it.length == 10) "$it..." else it } ?: "-",
                        style = MaterialTheme.typography.headlineMedium, color = PrimaryViolet, fontWeight = FontWeight.Bold
                    )
                    Text(text = "Most Played", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Sync",
                        tint = PrimaryViolet,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Cloud Synchronization", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(text = "Sync likes, history, and playlists with MongoDB Atlas", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onPerformSync,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sync Now")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onForceUpload,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryViolet),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Force Upload Device State")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connection & Server Config Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Connection",
                        tint = if (isOnline) PrimaryViolet else ErrorRed
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isOnline) "Connected to Internet" else "Offline Mode Active",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "API Base URL", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Text(text = currentBaseUrl, style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp), color = TextSecondary)
                    }

                    TextButton(onClick = { showUrlDialog = true }) {
                        Text("Edit", color = PrimaryViolet)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logout Button
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout", tint = TextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Configure Backend Base URL", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        text = "Use http://10.0.2.2:5000 for emulator, http://YOUR_IP:5000 for physical phone, or https://api.yourdomain.com for production EC2.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        label = { Text("Base URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUrl.isNotBlank()) {
                            onUpdateBaseUrl(newUrl.trim())
                            showUrlDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
