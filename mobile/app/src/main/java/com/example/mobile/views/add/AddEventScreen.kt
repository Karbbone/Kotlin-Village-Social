package com.example.mobile.views.add

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import com.example.mobile.services.cities.CitiesRepository
import androidx.compose.runtime.collectAsState
import com.example.mobile.network.ApiService
import com.example.mobile.network.CreateEventRequest
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import com.example.mobile.network.CityDto
import com.example.mobile.network.EventTypeDto
import com.example.mobile.search.rankCities
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import com.example.mobile.utils.urisToMultipartParts
import com.example.mobile.utils.toRequestBody
import com.example.mobile.utils.stringsToMultipartParts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    modifier: Modifier = Modifier,
    citiesRepo: CitiesRepository,
    api: ApiService,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    // Text fields
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    // DateTime Picker additions
    var showDatePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var showTimePicker by remember { mutableStateOf(false) }
    val timeState = rememberTimePickerState()
    var pickedDateDisplay by remember { mutableStateOf("") }
    var pickedDateIso by remember { mutableStateOf("") }
    var selectedLocalDate by remember { mutableStateOf<LocalDate?>(null) }

    // Multi-select event types via inline checkboxes
    val selectedTypes = remember { mutableStateListOf<String>() }
    var availableTypes by remember { mutableStateOf<List<EventTypeDto>>(emptyList()) }
    var typesLoading by remember { mutableStateOf(true) }

    // City selector state using cached repo
    var cityQuery by remember { mutableStateOf("") }
    var citySuggestions by remember { mutableStateOf<List<CityDto>>(emptyList()) }
    var cityShowSuggestions by remember { mutableStateOf(false) }
    var cityIsLoading by remember { mutableStateOf(false) }
    var cityErrorText by remember { mutableStateOf<String?>(null) }
    var selectedCity by remember { mutableStateOf<CityDto?>(null) }
    var createLoading by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    // Photo picker state
    val selectedPhotos = remember { mutableStateListOf<Uri>() }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
        onResult = { uris ->
            selectedPhotos.clear()
            selectedPhotos.addAll(uris)
        }
    )

    val focusManager = LocalFocusManager.current

    fun openPickersIfNeeded() {
        if (showDatePicker || showTimePicker) return
        showDatePicker = true
    }

    val cities by citiesRepo.cities.collectAsState()

    // Load event types from API
    LaunchedEffect(Unit) {
        typesLoading = true
        try {
            availableTypes = api.getEventTypes()
        } catch (e: Exception) {
            Log.e("AddEventScreen", "Failed to load event types", e)
        } finally {
            typesLoading = false
        }
    }

    // Debounced local filtering of cached cities
    LaunchedEffect(cityQuery, cities) {
        cityErrorText = null
        val norm = cityQuery.trim()
        if (norm.length < 2) {
            citySuggestions = emptyList()
            cityShowSuggestions = false
            return@LaunchedEffect
        }
        cityIsLoading = true
        // small debounce
        kotlinx.coroutines.delay(150)
        val ranked = rankCities(norm, cities).take(20)
        citySuggestions = ranked
        cityShowSuggestions = ranked.isNotEmpty()
        cityIsLoading = false
        // debug: show first few suggestions
        if (ranked.isNotEmpty()) {
            val sample = ranked.take(5).joinToString { it.name + " (" + it.postalCode + ")" }
            Log.d("AddEventScreen", "Ranked city suggestions for query='${cityQuery}': ${sample}")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("*", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Text("Champs obligatoires", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { RequiredLabel("Titre") },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { RequiredLabel("Description") },
            minLines = 3,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            modifier = Modifier.fillMaxWidth(),
            label = { RequiredLabel("Lieu") },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )

        // Inserted city selector between location and datetime
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = selectedCity?.let { it.name + " (" + it.postalCode.take(2) + ")" } ?: cityQuery,
            onValueChange = {
                selectedCity = null
                cityQuery = it
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Tapez une ville (ex: Paris)") },
            label = { RequiredLabel("Ville") },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(Modifier.height(4.dp))

        if (cityIsLoading && cityQuery.length >= 2) {
            Spacer(Modifier.height(8.dp))
            Text(text = "Recherche en cours…", style = MaterialTheme.typography.bodySmall)
        }
        if (cityErrorText != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = cityErrorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (cityShowSuggestions) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .background(MaterialTheme.colorScheme.surface),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
                ) {
                    items(citySuggestions) { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCity = c
                                    cityQuery = c.name + " (" + c.postalCode.take(2) + ")"
                                    cityShowSuggestions = false
                                    focusManager.clearFocus(force = true)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = c.name + " (" + c.postalCode.take(2) + ")",
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = pickedDateDisplay,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        openPickersIfNeeded()
                        // Empêcher le clavier et retirer le focus dès l'ouverture
                        focusManager.clearFocus(force = true)
                    }
                }
                .pointerInput(Unit) { detectTapGestures(onTap = { openPickersIfNeeded() }) }
                .semantics { onClick(label = "Ouvrir le sélecteur date/heure") { openPickersIfNeeded(); true } },
            readOnly = true,
            placeholder = { Text("Choisir la date et l'heure") },
            label = { RequiredLabel("Date et heure") },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Event,
                    contentDescription = "Choisir la date",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(onClick = {
                        val millis = dateState.selectedDateMillis
                        if (millis != null) {
                            selectedLocalDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            showTimePicker = true
                        }
                        showDatePicker = false
                    }) { Text("Suivant") }
                },
                dismissButton = { Button(onClick = { showDatePicker = false }) { Text("Annuler") } }
            ) { DatePicker(state = dateState) }
        }

        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    Button(onClick = {
                        val baseDate = selectedLocalDate
                            ?: dateState.selectedDateMillis?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            }
                        if (baseDate != null) {
                            val hour = timeState.hour
                            val minute = timeState.minute
                            val localDateTime = baseDate.atTime(hour, minute)
                            pickedDateDisplay = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm")
                                .withZone(ZoneId.systemDefault())
                                .format(localDateTime.atZone(ZoneId.systemDefault()))
                            pickedDateIso = localDateTime
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toString()
                        }
                        showTimePicker = false
                    }) { Text("OK") }
                },
                dismissButton = { Button(onClick = { showTimePicker = false }) { Text("Annuler") } },
                text = { TimePicker(state = timeState) }
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Types d'évènement", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(4.dp))
            Text("*", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(6.dp))
        // Inline list of checkboxes for multi-select
        if (typesLoading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Column {
                availableTypes.forEach { type ->
                    val checked = selectedTypes.contains(type.name)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (checked) selectedTypes.remove(type.name) else selectedTypes.add(type.name)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                if (checked) selectedTypes.remove(type.name) else selectedTypes.add(type.name)
                            }
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(type.name)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text("📷 Ajouter des photos (${selectedPhotos.size}/5)", style = MaterialTheme.typography.bodyLarge)
        }

        // Display selected photos
        if (selectedPhotos.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                selectedPhotos.forEach { uri ->
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Photo sélectionnée",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        val scope = rememberCoroutineScope()

        Button(
            onClick = {
                // Build payload and call API
                scope.launch {
                    createError = null
                    createLoading = true
                    try {
                        val cityName = selectedCity?.name ?: run {
                            createError = "Aucune ville sélectionnée"
                            null
                        }
                        if (cityName != null) {
                            // Use multipart API if photos are selected
                            val created = if (selectedPhotos.isNotEmpty() || selectedTypes.isNotEmpty()) {
                                val photoParts = urisToMultipartParts(context, selectedPhotos)
                                val typesParts = stringsToMultipartParts("types", selectedTypes.toList())

                                api.createEventWithPhotos(
                                    cityName = cityName,
                                    title = title.toRequestBody(),
                                    description = description.takeIf { it.isNotBlank() }?.toRequestBody(),
                                    location = location.takeIf { it.isNotBlank() }?.toRequestBody(),
                                    date = pickedDateIso.takeIf { it.isNotBlank() }?.toRequestBody(),
                                    types = typesParts,
                                    photos = photoParts
                                )
                            } else {
                                // Use JSON API if no photos
                                val body = CreateEventRequest(
                                    title = title,
                                    description = description.takeIf { it.isNotBlank() },
                                    location = location.takeIf { it.isNotBlank() },
                                    date = if (pickedDateIso.isNotBlank()) pickedDateIso else null,
                                    types = if (selectedTypes.isNotEmpty()) selectedTypes.toList() else null,
                                    photoUrls = null
                                )
                                api.createEventForCity(cityName, body)
                            }

                            snackbarHostState.showSnackbar("Évènement créé : ${created.title}", duration = SnackbarDuration.Short)
                            // Reset form
                            title = ""
                            description = ""
                            location = ""
                            pickedDateDisplay = ""
                            pickedDateIso = ""
                            selectedTypes.clear()
                            selectedCity = null
                            cityQuery = ""
                            selectedPhotos.clear()
                        }
                    } catch (e: Exception) {
                        Log.w("AddEventScreen", "Failed to create event", e)
                        createError = e.message ?: "Erreur lors de la création"
                        snackbarHostState.showSnackbar("Erreur: ${createError}", duration = SnackbarDuration.Short)
                    } finally {
                        createLoading = false
                    }
                }
            },
            enabled = title.isNotBlank() && description.isNotBlank() && location.isNotBlank() &&
                    selectedCity != null && pickedDateIso.isNotBlank() && selectedTypes.isNotEmpty() && !createLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
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
            if (createLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 3.dp
                )
            } else {
                Text(
                    "Créer l'évènement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        if (createError != null) {
            Text(text = createError!!, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun RequiredLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text)
        Spacer(Modifier.width(2.dp))
        Text("*", color = MaterialTheme.colorScheme.error)
    }
}
