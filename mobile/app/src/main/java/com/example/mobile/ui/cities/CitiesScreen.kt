package com.example.mobile.ui.cities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.mobile.auth.AuthRepository
import com.example.mobile.cities.CitiesRepository
import com.example.mobile.network.ApiService
import com.example.mobile.network.CityDto
import com.example.mobile.network.EventDto
import com.example.mobile.search.rankCities
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun CitiesScreen(
    api: ApiService,
    authRepo: AuthRepository,
    citiesRepo: CitiesRepository,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val userJson by authRepo.userJsonState.collectAsState()
    val userId = remember(userJson) {
        userJson?.let {
            try { JSONObject(it).optInt("id", -1) } catch (_: Exception) { -1 }
        } ?: -1
    }

    var userCities by remember { mutableStateOf<List<CityDto>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val allCities by citiesRepo.cities.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<CityDto>>(emptyList()) }
    var isQueryLoading by remember { mutableStateOf(false) }

    // Events state for user's cities
    var events by remember { mutableStateOf<List<EventDto>>(emptyList()) }
    var eventsLoading by remember { mutableStateOf(false) }

    // Global mutation loader (add/remove + refresh)
    var isMutating by remember { mutableStateOf(false) }

    suspend fun refreshUserCities() {
        if (userId <= 0) return
        loading = true
        error = null
        try {
            val list = withContext(Dispatchers.IO) { api.getUserCities(userId) }
            userCities = list
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Erreur lors du chargement"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(userId) { if (userId > 0) refreshUserCities() }

    LaunchedEffect(query, allCities) {
        val q = query.trim()
        if (q.length < 2) {
            suggestions = emptyList()
            isQueryLoading = false
        } else {
            isQueryLoading = true
            kotlinx.coroutines.delay(150)
            val ranked = rankCities(q, allCities).take(20)
            suggestions = ranked
            isQueryLoading = false
        }
    }

    LaunchedEffect(showDialog) {
        if (showDialog && userId > 0) {
            refreshUserCities()
        }
    }

    LaunchedEffect(userCities) {
        // When user cities change, refetch events for those cities
        if (userCities.isEmpty()) {
            events = emptyList()
        } else {
            eventsLoading = true
            val fetched = mutableListOf<EventDto>()
            userCities.forEach { c ->
                val list = runCatching { withContext(Dispatchers.IO) { api.getEventsByCity(c.name) } }.getOrElse { emptyList() }
                fetched += list
            }
            // Optional: sort by date ascending
            events = fetched.sortedBy { e -> e.date?.let { runCatching { Instant.parse(it) }.getOrNull() } }
            eventsLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Main page: events list with space for sticky button
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 88.dp)
        ) {
            if (eventsLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }
            if (events.isEmpty() && !eventsLoading) {
                Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        "Aucun évènement trouvé pour vos villes.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm").withZone(ZoneId.systemDefault()) }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(events) { ev ->
                        ElevatedCard(
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
                                            .height(180.dp)
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
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

        // Sticky bottom action
        Surface(
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(Modifier.padding(16.dp)) {
                Button(
                    onClick = { if (!isMutating) showDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isMutating,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Text(
                        "Modifier mes villes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { if (!isMutating && !loading) showDialog = false },
            confirmButton = {},
            title = { Text("Mes villes") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    // Linear loader shown during fetch OR mutation
                    if (loading || isMutating) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                    }

                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (userCities.isNotEmpty()) {
                        Text("Vos villes", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(userCities) { c ->
                                ElevatedCard {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                c.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                c.postalCode.take(2),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                if (userId <= 0 || isMutating || loading) return@TextButton
                                                scope.launch {
                                                    isMutating = true
                                                    val ok = withContext(Dispatchers.IO) { api.removeUserCity(c.id, userId).isSuccessful }
                                                    if (ok) {
                                                        // Show snackbar without blocking the refresh
                                                        scope.launch { snackbarHostState.showSnackbar("Ville supprimée") }
                                                        refreshUserCities()
                                                    } else {
                                                        scope.launch { snackbarHostState.showSnackbar("Suppression impossible") }
                                                    }
                                                    isMutating = false
                                                }
                                            },
                                            enabled = !isMutating && !loading
                                        ) { Text("Supprimer") }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                    }

                    // Add city via search
                    Text("Ajouter une ville", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { if (!isMutating && !loading) query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isMutating && !loading,
                        placeholder = { Text("Tapez une ville (ex: Paris)") },
                        label = { Text("Ville") },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    if (isQueryLoading && query.length >= 2) {
                        Spacer(Modifier.height(8.dp))
                        Text("Recherche en cours…", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                            items(suggestions) { c ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .let {
                                            if (!isMutating && !loading) it.clickable {
                                                if (userId <= 0) return@clickable
                                                scope.launch {
                                                    isMutating = true
                                                    val ok = withContext(Dispatchers.IO) { api.addUserCity(c.id, userId).isSuccessful }
                                                    if (ok) {
                                                        // Show snackbar without blocking the refresh
                                                        scope.launch { snackbarHostState.showSnackbar("Ville ajoutée") }
                                                        refreshUserCities()
                                                        // keep dialog open; allow multiple additions
                                                        query = ""
                                                    } else {
                                                        scope.launch { snackbarHostState.showSnackbar("Ajout impossible") }
                                                    }
                                                    isMutating = false
                                                }
                                            } else it
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(c.name + " (" + c.postalCode.take(2) + ")")
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isMutating && !loading) showDialog = false }, enabled = !isMutating && !loading) { Text("Fermer") }
            }
        )
    }
}
