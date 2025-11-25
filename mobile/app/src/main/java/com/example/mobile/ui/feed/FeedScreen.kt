package com.example.mobile.ui.feed

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
import com.example.mobile.model.EventType
import com.example.mobile.search.rankCities
import coil.compose.AsyncImage
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
import com.example.mobile.cities.CitiesRepository
import com.example.mobile.network.ApiService
import com.example.mobile.network.EventDto
import com.example.mobile.network.SearchEventsRequest
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.ContentScale

@Composable
fun FeedScreen(modifier: Modifier = Modifier, citiesRepo: CitiesRepository, api: ApiService) {
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Track the actually selected city (clean name) vs the input text
    var selectedCityName by remember { mutableStateOf<String?>(null) }

    var selectedEventType by remember { mutableStateOf<EventType?>(null) }

    // Base events (unfiltered) and current events
    var baseEvents by remember { mutableStateOf<List<EventDto>>(emptyList()) }
    var events by remember { mutableStateOf<List<EventDto>>(emptyList()) }
    var eventsLoading by remember { mutableStateOf(false) }

    val cityList by citiesRepo.cities.collectAsState()

    LaunchedEffect(Unit) {
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

    LaunchedEffect(query) {
        if (selectedCityName == null) return@LaunchedEffect
        selectedCityName = null
    }

    LaunchedEffect(selectedCityName, selectedEventType) {
        if (selectedCityName.isNullOrBlank() && selectedEventType == null) {
            events = baseEvents
            return@LaunchedEffect
        }
        eventsLoading = true
        val body = SearchEventsRequest(
            types = selectedEventType?.let { listOf(it.displayName) },
            cityName = selectedCityName
        )
        val list = runCatching { api.searchEvents(body) }.getOrElse { emptyList() }
        events = list.sortedBy { e -> e.date?.let { runCatching { Instant.parse(it) }.getOrNull() } }
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
                query = txt
                selectedCityName = null
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
                                    query = item
                                    selectedCityName = item.substringBefore(" (").trim()
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
            items(EventType.entries.toList()) { type ->
                val selected = selectedEventType == type
                val icon = type.icon()
                FilterChip(
                    selected = selected,
                    onClick = { selectedEventType = if (selected) null else type },
                    label = {
                        Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = type.displayName,
                                modifier = Modifier.height(20.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                type.displayName,
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
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            val photoUrl = ev.photos?.firstOrNull()?.url
                            if (photoUrl != null) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = ev.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                )
                            }
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    ev.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(8.dp))

                                val dt = ev.date?.let { runCatching { Instant.parse(it) }.getOrNull() }
                                if (dt != null) {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Text("🕒 ", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            formatter.format(dt),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (!ev.location.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Text(
                                            ev.location,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Text(
                                        ev.city,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun EventType.icon() = when (this) {
    EventType.SPECTACLE -> Icons.Outlined.Theaters
    EventType.CONCERT_SPECTACLE_MUSICAL -> Icons.Outlined.MusicNote
    EventType.ACTIVITE_DE_LOISIRS -> Icons.Outlined.EmojiEvents
    EventType.EXPOSITION_MUSEE -> Icons.Outlined.Museum
    EventType.VISITE_BALADE -> Icons.AutoMirrored.Outlined.DirectionsWalk
    EventType.CONFERENCE_DEBAT -> Icons.Outlined.Forum
    EventType.SPORT -> Icons.AutoMirrored.Outlined.DirectionsRun
}
