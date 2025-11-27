package com.example.mobile.views.event

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobile.network.ApiService
import com.example.mobile.network.EventDto
import com.example.mobile.network.PhotoDto
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Int,
    api: ApiService,
    userJsonState: String?,
    onBack: () -> Unit,
    onEdit: (EventDto) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var event by remember { mutableStateOf<EventDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Get current user ID to check if they can edit/delete
    val currentUserId = userJsonState?.let {
        try {
            JSONObject(it).optInt("id", -1)
        } catch (_: Exception) {
            -1
        }
    } ?: -1

    LaunchedEffect(eventId) {
        loading = true
        error = null
        try {
            event = api.getEventById(eventId)
        } catch (e: Exception) {
            error = e.message ?: "Erreur lors du chargement"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails de l'événement") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    // Show edit/delete only if user is creator
                    // Note: API should return creator info, for now we'll show it anyway
                    IconButton(onClick = { event?.let { onEdit(it) } }) {
                        Icon(Icons.Default.Edit, "Modifier")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
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
                event != null -> {
                    EventDetailContent(event = event!!)
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer l'événement") },
            text = { Text("Êtes-vous sûr de vouloir supprimer cet événement ? Cette action est irréversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isDeleting = true
                            try {
                                val response = api.deleteEvent(eventId)
                                if (response.isSuccessful) {
                                    snackbarHostState.showSnackbar("Événement supprimé")
                                    onBack()
                                } else {
                                    snackbarHostState.showSnackbar("Erreur: ${response.code()}")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Erreur: ${e.message}")
                            } finally {
                                isDeleting = false
                                showDeleteDialog = false
                            }
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Supprimer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun EventDetailContent(event: EventDto) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy 'à' HH:mm")
            .withZone(ZoneId.systemDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Photos carousel - show all supported photos
        val supportedPhotos = getSupportedPhotos(event.photos)
        if (supportedPhotos.isNotEmpty()) {
            supportedPhotos.forEach { photo ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(bottom = 12.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    AsyncImage(
                        model = photo.url,
                        contentDescription = "Photo de l'événement",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Title
        Text(
            text = event.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Date & Time
        event.date?.let { dateStr ->
            val instant = remember(dateStr) {
                try {
                    Instant.parse(dateStr)
                } catch (_: Exception) {
                    null
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = instant?.let { formatter.format(it) } ?: dateStr,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Location
        event.location?.let { loc ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = loc,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // City
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            Text(
                text = event.city,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Event Types
        if (!event.types.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Types d'événement",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                event.types.forEach { type ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = type.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Description
        if (!event.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
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

