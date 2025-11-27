package com.example.mobile.views.feed

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mobile.search.rankCities
import coil.compose.AsyncImage
import com.example.mobile.network.EventTypeDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Forum
import com.example.mobile.services.cities.CitiesRepository
import com.example.mobile.network.ApiService
import com.example.mobile.network.EventDto
import com.example.mobile.network.PhotoDto
import com.example.mobile.network.SearchEventsRequest
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.ContentScale
import com.example.mobile.utils.getFirstPhotoUrl
import com.example.mobile.components.EventCard

@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    citiesRepo: CitiesRepository,
    api: ApiService,
    onEventClick: (Int) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Track the actually selected city (clean name) vs the input text
    var selectedCityName by remember { mutableStateOf<String?>(null) }

    var selectedEventType by remember { mutableStateOf<String?>(null) }
    var availableTypes by remember { mutableStateOf<List<EventTypeDto>>(emptyList()) }

    // Base events (unfiltered) and current events
    var baseEvents by remember { mutableStateOf<List<EventDto>>(emptyList()) }
    var events by remember { mutableStateOf<List<EventDto>>(emptyList()) }
    var eventsLoading by remember { mutableStateOf(false) }

    val cityList by citiesRepo.cities.collectAsState()

    LaunchedEffect(Unit) {
        // Load event types
        try {
            availableTypes = api.getEventTypes()
        } catch (e: Exception) {
            Log.e("FeedScreen", "Failed to load event types", e)
        }

        // Load initial events
        eventsLoading = true
        val initial = runCatching { api.searchEvents(SearchEventsRequest()) }.getOrElse { emptyList() }
        val sorted = initial.sortedBy { e -> e.date?.let { runCatching { Instant.parse(it) }.getOrNull() } }
        baseEvents = sorted
        events = sorted
        eventsLoading = false
    }

    LaunchedEffect(query, cityList) {
        errorText = null
        val q = query.trim()
        if (q.length < 2) {
            suggestions = emptyList()
            showSuggestions = false
            return@LaunchedEffect
        }
        isLoading = true
        delay(150)
        val ranked = rankCities(q, cityList).take(20)
        suggestions = ranked.map { it.name + " (" + it.postalCode.take(2) + ")" }
        showSuggestions = ranked.isNotEmpty()
        isLoading = false
        if (ranked.isNotEmpty()) {
            val sample = ranked.take(5).joinToString { it.name + " (" + it.postalCode.take(2) + ")" }
            Log.d("FeedScreen", "Ranked city suggestions for query='" + q + "': " + sample)
        }
    }

    // This effect should NOT reset selectedCityName when query changes
    // because when user selects a city, we update both query AND selectedCityName
    // We only want to reset selectedCityName when user manually types (not when selecting from suggestions)

    LaunchedEffect(selectedCityName, selectedEventType) {
        val cityName = selectedCityName
        val eventType = selectedEventType

        Log.d("FeedScreen", "LaunchedEffect triggered - cityName: $cityName, eventType: $eventType")

        if (cityName.isNullOrBlank() && eventType == null) {
            Log.d("FeedScreen", "No filters, showing baseEvents")
            events = baseEvents
            return@LaunchedEffect
        }
        eventsLoading = true

        val list = if (!cityName.isNullOrBlank()) {
            // Use events/cities/{cityName} when a city is selected
            Log.d("FeedScreen", "Fetching events for city: $cityName")
            val cityEvents = runCatching {
                api.getEventsByCity(cityName)
            }.getOrElse { e ->
                Log.e("FeedScreen", "Failed to fetch events for city: $cityName", e)
                emptyList()
            }
            Log.d("FeedScreen", "Fetched ${cityEvents.size} events for city: $cityName")
            // Apply type filter locally if a type is also selected
            if (eventType != null) {
                cityEvents.filter { ev -> ev.types?.any { it.name == eventType } == true }
            } else {
                cityEvents
            }
        } else {
            // Only type filter, use search API
            Log.d("FeedScreen", "Fetching events by type: $eventType")
            val body = SearchEventsRequest(types = eventType?.let { listOf(it) })
            runCatching { api.searchEvents(body) }.getOrElse { emptyList() }
        }

        events = list.sortedBy { e -> e.date?.let { runCatching { Instant.parse(it) }.getOrNull() } }
        Log.d("FeedScreen", "Final events count: ${events.size}")
        eventsLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { txt ->
                // If the user is typing (text is different), reset selectedCityName
                // This allows filtering suggestions while typing
                if (txt != query) {
                    query = txt
                    // Only reset if the new text doesn't match the format "CityName (XX)"
                    // which would indicate a selection was just made
                    if (!txt.contains(" (") || txt.length < query.length) {
                        Log.d("FeedScreen", "User typing, resetting selectedCityName")
                        selectedCityName = null
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Tapez une ville (ex: Paris)") },
            label = { Text("Ville") },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(Modifier.height(4.dp))

        if (isLoading && query.length >= 2) {
            Spacer(Modifier.height(8.dp))
            Text(text = "Recherche en cours…", style = MaterialTheme.typography.bodySmall)
        }

        if (errorText != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorText!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (showSuggestions) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(suggestions) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val cityName = item.substringBefore(" (").trim()
                                    Log.d("FeedScreen", "City selected: $cityName (from: $item)")
                                    query = item
                                    selectedCityName = cityName
                                    showSuggestions = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            items(availableTypes) { type ->
                val selected = selectedEventType == type.name
                val icon = getIconForEventType(type.name)
                FilterChip(
                    selected = selected,
                    onClick = { selectedEventType = if (selected) null else type.name },
                    label = {
                        Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = type.name,
                                modifier = Modifier.height(20.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                type.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        }
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        iconColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = if (!selected) androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ) else null
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        if (eventsLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
        if (events.isEmpty() && !eventsLoading) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    "Aucun évènement trouvé.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm").withZone(ZoneId.systemDefault()) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(events) { ev ->
                    EventCard(event = ev, modifier = Modifier, onClick = { onEventClick(ev.id) })
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

private fun getIconForEventType(typeName: String) = when (typeName.lowercase()) {
    "spectacle" -> Icons.Outlined.Theaters
    "concert, spectacle musical", "concert" -> Icons.Outlined.MusicNote
    "activité de loisirs", "loisirs" -> Icons.Outlined.EmojiEvents
    "exposition, musée", "musée", "exposition" -> Icons.Outlined.Museum
    "visite, balade", "visite", "balade" -> Icons.AutoMirrored.Outlined.DirectionsWalk
    "conférence, débat", "conférence", "débat" -> Icons.Outlined.Forum
    "sport" -> Icons.AutoMirrored.Outlined.DirectionsRun
    else -> Icons.Outlined.EmojiEvents // Default icon
}
