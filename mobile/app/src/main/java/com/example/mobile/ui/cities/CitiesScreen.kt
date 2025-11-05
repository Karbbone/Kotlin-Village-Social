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
import com.example.mobile.auth.AuthRepository
import com.example.mobile.cities.CitiesRepository
import com.example.mobile.network.ApiService
import com.example.mobile.network.CityDto
import com.example.mobile.search.rankCities
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

    Box(modifier = modifier.fillMaxSize()) {
        // Main page: events placeholder; leave bottom space for sticky button
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 88.dp)
        ) {
            Text("Évènements liés à vos villes", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Affichage des évènements filtrés par vos villes à venir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isMutating
                ) { Text("Modifier mes villes") }
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
                        label = { Text("Ville") }
                    )
                    if (isQueryLoading && query.length >= 2) {
                        Spacer(Modifier.height(8.dp))
                        Text("Recherche en cours…", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Card(Modifier.fillMaxWidth()) {
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
