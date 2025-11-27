package com.example.mobile.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mobile.auth.AuthRepository
import com.example.mobile.network.ApiService
import com.example.mobile.network.EventDto
import com.example.mobile.network.PhotoDto
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProfileScreen(
    authRepo: AuthRepository,
    api: ApiService,
    @Suppress("UNUSED_PARAMETER") snackbarHostState: SnackbarHostState,
    onEventClick: (Int) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val userJson by authRepo.userJsonState.collectAsState()
    val displayName = userJson?.let {
        try {
            val obj = JSONObject(it)
            obj.optString("displayName")
        } catch (_: Exception) { null }
    }
    val email = userJson?.let {
        try { JSONObject(it).optString("email") } catch (_: Exception) { null }
    }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val events = remember { mutableStateListOf<EventDto>() }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            val fetched = api.getMyPastEvents()
            val sorted = fetched.sortedByDescending { it.date ?: "" }
            events.clear()
            events.addAll(sorted)
        } catch (e: Exception) {
            error = e.message ?: "Erreur lors du fetch des événements"
        } finally {
            loading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp)
        ) {
            // User Info Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Avatar placeholder
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .padding(bottom = 16.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName?.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (displayName != null) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (email != null) {
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            if (error != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Empty state
            if (!loading && events.isEmpty() && error == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun événement passé",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Events list
            items(events) { ev ->
                EventCard(event = ev, onClick = { onEventClick(ev.id) })
            }
        }

        // Logout button - sticky at bottom
        Button(
            onClick = { scope.launch { authRepo.clear() } },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Se déconnecter", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun EventCard(event: EventDto, onClick: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm").withZone(ZoneId.systemDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            val photoUrl = getFirstPhotoUrl(event.photos)
            if (photoUrl != null) {
                coil.compose.AsyncImage(
                    model = photoUrl,
                    contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
            Column(Modifier.padding(16.dp)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))

                val dt = event.date?.let { runCatching { Instant.parse(it) }.getOrNull() }
                if (dt != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🕒 ", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatter.format(dt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!event.location.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            event.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        event.city,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Display event types
                if (!event.types.isNullOrEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                                        items(event.types) { type ->
                            Card(
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = type.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function to filter supported photo formats and normalize URLs
private fun getSupportedPhotos(photos: List<PhotoDto>?): List<PhotoDto> {
    if (photos.isNullOrEmpty()) return emptyList()

    return photos
        .filter { photo ->
            // Filter out HEIC format as it's not well supported on Android
            val url = photo.url.lowercase()
            !url.endsWith(".heic") && !url.contains(".heic?")
        }
        .map { photo ->
            // Replace localhost with 10.0.2.2 (Android emulator host IP)
            photo.copy(url = photo.url.replace("http://localhost:", "http://10.0.2.2:"))
        }
}

// Get the first supported photo URL for event cards
private fun getFirstPhotoUrl(photos: List<PhotoDto>?): String? {
    return getSupportedPhotos(photos).firstOrNull()?.url
}
