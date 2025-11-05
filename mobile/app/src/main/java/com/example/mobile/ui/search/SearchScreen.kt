package com.example.mobile.ui.search

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Single-select event type from shared enum
    var selectedEventType by remember { mutableStateOf<EventType?>(null) }

    // Debounced search for cities
    LaunchedEffect(query) {
        errorText = null
        if (query.trim().length < 2) {
            suggestions = emptyList()
            showSuggestions = false
            return@LaunchedEffect
        }
        isLoading = true
        delay(300)
        try {
            val cities = fetchFrenchCities(query.trim())
            suggestions = cities
            showSuggestions = cities.isNotEmpty()
        } catch (e: Exception) {
            errorText = "Erreur de recherche: ${e.message ?: "inconnue"}"
            suggestions = emptyList()
            showSuggestions = false
        } finally {
            isLoading = false
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
            onValueChange = { new -> query = new },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Tapez une ville (ex: Paris)") },
            label = { Text("Ville") }
        )

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
                FilterChip(
                    selected = selected,
                    onClick = { selectedEventType = if (selected) null else type },
                    label = { Text(type.displayName) },
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
                    if (query.isNotBlank()) append("ville=\"$query\"")
                    if (selectedEventType != null) {
                        if (query.isNotBlank()) append(" · ")
                        append("type=\"${selectedEventType!!.displayName}\"")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private suspend fun fetchFrenchCities(query: String): List<String> = withContext(Dispatchers.IO) {
    if (query.length < 2) return@withContext emptyList()
    val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
    val url = URL("https://geo.api.gouv.fr/communes?nom=$encoded&fields=nom&boost=population&limit=8")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 6000
        readTimeout = 6000
    }
    try {
        connection.inputStream.bufferedReader().use { reader ->
            val body = reader.readText()
            val arr = JSONArray(body)
            val result = ArrayList<String>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val name = o.optString("nom")
                if (name.isNotBlank()) result.add(name)
            }
            result
        }
    } finally {
        connection.disconnect()
    }
}
