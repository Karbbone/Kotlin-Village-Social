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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mobile.model.EventType
import com.example.mobile.search.rankCities
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Forum
import com.example.mobile.cities.CitiesRepository
import androidx.compose.runtime.collectAsState

@Composable
fun FeedScreen(modifier: Modifier = Modifier, citiesRepo: CitiesRepository) {
    val context = LocalContext.current

    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var selectedEventType by remember { mutableStateOf<EventType?>(null) }

    // Load mocks
    var typeCounts by remember { mutableStateOf<Map<EventType, Int>>(emptyMap()) }
    val allEvents = remember { mutableStateListOf<MockEvent>() }
    LaunchedEffect(Unit) {
        typeCounts = loadEventTypeCountsFromAssets(context)
        allEvents.clear()
        allEvents.addAll(loadMockEventsFromAssets(context))
    }

    val cityList by citiesRepo.cities.collectAsState()

    // Debounced search: filter locally from cached cities
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
            Log.d("FeedScreen", "Ranked city suggestions for query='${q}': ${ranked.take(5).joinToString { it.name + " (" + it.postalCode.take(2) + ")" }}")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Recherche par ville",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Tapez une ville (ex: Paris)") },
            label = { Text("Ville") }
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
                modifier = Modifier.fillMaxWidth()
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

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Type d'évènement",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(EventType.entries.toList()) { type ->
                val selected = selectedEventType == type
                val count = typeCounts[type] ?: 0
                val icon = type.icon()
                FilterChip(
                    selected = selected,
                    onClick = { selectedEventType = if (selected) null else type },
                    label = {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Icon(imageVector = icon, contentDescription = type.displayName)
                            Spacer(Modifier.height(4.dp))
                            Text(type.displayName, style = MaterialTheme.typography.labelMedium)
                            if (count > 0) {
                                Spacer(Modifier.height(2.dp))
                                Text("${count} év.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        if (selectedEventType != null || query.isNotBlank()) {
            Text(
                text = buildString {
                    append("Filtre: ")
                    if (query.isNotBlank()) append("ville=\"" + query + "\"")
                    if (selectedEventType != null) {
                        if (query.isNotBlank()) append(" · ")
                        append("type=\"" + selectedEventType!!.displayName + "\"")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Show mock events filtered by city and type (hide suggestions overlay)
        if (!showSuggestions) {
            Spacer(Modifier.height(16.dp))
            Text("Évènements", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val now = remember { Instant.now() }
            val formatter = remember {
                DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm").withZone(ZoneId.systemDefault())
            }
            val filtered = remember(query, selectedEventType, allEvents) {
                allEvents
                    .filter { it.dateIso?.let { d -> runCatching { Instant.parse(d) }.getOrNull() }?.isAfter(now) == true }
                    .filter { ev -> query.isBlank() || ev.city.equals(query.trim(), ignoreCase = true) }
                    .filter { ev -> selectedEventType == null || ev.types.contains(selectedEventType!!.name) }
                    .sortedBy { Instant.parse(it.dateIso) }
            }
            if (filtered.isEmpty()) {
                Text("Aucun évènement à afficher", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { ev ->
                        Card(colors = CardDefaults.cardColors()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(ev.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(formatter.format(Instant.parse(ev.dateIso)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(2.dp))
                                Text("${'$'}{ev.city} · ${'$'}{ev.location}", style = MaterialTheme.typography.bodySmall)
                                if (ev.types.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(ev.types) { t ->
                                            Text(t.replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class MockEvent(
    val title: String,
    val description: String?,
    val location: String,
    val dateIso: String?,
    val city: String,
    val types: List<String>
)

private fun EventType.icon() = when (this) {
    EventType.SPECTACLE -> Icons.Outlined.Theaters
    EventType.CONCERT_SPECTACLE_MUSICAL -> Icons.Outlined.MusicNote
    EventType.ACTIVITE_DE_LOISIRS -> Icons.Outlined.EmojiEvents
    EventType.EXPOSITION_MUSEE -> Icons.Outlined.Museum
    EventType.VISITE_BALADE -> Icons.AutoMirrored.Outlined.DirectionsWalk
    EventType.CONFERENCE_DEBAT -> Icons.Outlined.Forum
    EventType.SPORT -> Icons.AutoMirrored.Outlined.DirectionsRun
}

private fun loadEventTypeCountsFromAssets(context: android.content.Context): Map<EventType, Int> {
    return try {
        val json = context.assets.open("mock/event_types_counts.json").bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        EventType.entries.associateWith { et -> obj.optInt(et.name, 0) }
    } catch (_: Exception) {
        emptyMap()
    }
}

private fun loadMockEventsFromAssets(context: android.content.Context): List<MockEvent> {
    return try {
        val json = context.assets.open("mock/events.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    MockEvent(
                        title = o.optString("title"),
                        description = o.optString("description"),
                        location = o.optString("location"),
                        dateIso = o.optString("dateIso"),
                        city = o.optString("city"),
                        types = o.optJSONArray("types")?.let { ta -> List(ta.length()) { idx -> ta.getString(idx) } } ?: emptyList()
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
