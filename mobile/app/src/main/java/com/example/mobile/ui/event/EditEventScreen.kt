package com.example.mobile.ui.event

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mobile.cities.CitiesRepository
import com.example.mobile.network.ApiService
import com.example.mobile.network.EventDto
import com.example.mobile.network.EventTypeDto
import com.example.mobile.network.UpdateEventRequest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    event: EventDto,
    api: ApiService,
    @Suppress("UNUSED_PARAMETER") citiesRepo: CitiesRepository,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Form state
    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description ?: "") }
    var location by remember { mutableStateOf(event.location ?: "") }

    // DateTime state
    var showDatePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = event.date?.let {
            try {
                Instant.parse(it).toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        } ?: System.currentTimeMillis()
    )
    var showTimePicker by remember { mutableStateOf(false) }
    val timeState = rememberTimePickerState(
        initialHour = event.date?.let {
            try {
                Instant.parse(it).atZone(ZoneId.systemDefault()).hour
            } catch (_: Exception) {
                12
            }
        } ?: 12,
        initialMinute = event.date?.let {
            try {
                Instant.parse(it).atZone(ZoneId.systemDefault()).minute
            } catch (_: Exception) {
                0
            }
        } ?: 0
    )

    var pickedDateDisplay by remember {
        mutableStateOf(
            event.date?.let {
                try {
                    val instant = Instant.parse(it)
                    DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm")
                        .withZone(ZoneId.systemDefault())
                        .format(instant)
                } catch (_: Exception) {
                    ""
                }
            } ?: ""
        )
    }
    var pickedDateIso by remember { mutableStateOf(event.date ?: "") }
    var selectedLocalDate by remember { mutableStateOf<LocalDate?>(null) }

    // Event types
    val selectedTypes = remember { mutableStateListOf<String>().apply { addAll(event.types?.map { it.name } ?: emptyList()) } }
    var availableTypes by remember { mutableStateOf<List<EventTypeDto>>(emptyList()) }
    var typesLoading by remember { mutableStateOf(true) }

    // Photo state - show existing photos
    val selectedPhotos = remember { mutableStateListOf<Uri>() }
    val photosToDelete = remember { mutableStateListOf<Int>() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
        onResult = { uris ->
            selectedPhotos.addAll(uris)
        }
    )

    var updateLoading by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }

    fun openPickersIfNeeded() {
        if (showDatePicker || showTimePicker) return
        showDatePicker = true
    }

    // Load event types
    LaunchedEffect(Unit) {
        typesLoading = true
        try {
            availableTypes = api.getEventTypes()
        } catch (e: Exception) {
            Log.e("EditEventScreen", "Failed to load event types", e)
        } finally {
            typesLoading = false
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifier l'événement") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Required fields legend
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("*", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(6.dp))
                    Text("Champs obligatoires", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(20.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { RequiredLabel("Titre") },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            Spacer(Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { RequiredLabel("Description") },
                minLines = 3,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            Spacer(Modifier.height(12.dp))

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                modifier = Modifier.fillMaxWidth(),
                label = { RequiredLabel("Lieu") },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            Spacer(Modifier.height(12.dp))

            // City (read-only display)
            OutlinedTextField(
                value = event.city,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ville") },
                enabled = false,
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
            Spacer(Modifier.height(8.dp))

            // Date time picker
            OutlinedTextField(
                value = pickedDateDisplay,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            openPickersIfNeeded()
                            focusManager.clearFocus(force = true)
                        }
                    }
                    .pointerInput(Unit) { detectTapGestures(onTap = { openPickersIfNeeded() }) }
                    .semantics { onClick(label = "Ouvrir le sélecteur date/heure") { openPickersIfNeeded(); true } },
                readOnly = true,
                placeholder = { Text("Choisir la date et l'heure") },
                label = { RequiredLabel("Date et heure") },
                leadingIcon = {
                    Icon(Icons.Outlined.Event, contentDescription = "Choisir la date", tint = MaterialTheme.colorScheme.primary)
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
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

            // Event types
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Types d'évènement", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(4.dp))
                Text("*", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))

            if (typesLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
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

            // Existing photos
            if (event.photos?.isNotEmpty() == true) {
                Text("Photos existantes", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    event.photos.forEach { photo ->
                        if (!photosToDelete.contains(photo.id)) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = photo.url,
                                    contentDescription = "Photo existante",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { photosToDelete.add(photo.id) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.error, androidx.compose.foundation.shape.CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Add new photos button
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("📷 Ajouter des photos (${selectedPhotos.size})", style = MaterialTheme.typography.bodyLarge)
            }

            // Display new photos
            if (selectedPhotos.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedPhotos.forEachIndexed { index, uri ->
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Nouvelle photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedPhotos.removeAt(index) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.error, androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Supprimer",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        updateError = null
                        updateLoading = true
                        try {
                            // Update event via PATCH
                            val updateBody = UpdateEventRequest(
                                title = title.takeIf { it != event.title },
                                description = description.takeIf { it != event.description },
                                location = location.takeIf { it != event.location },
                                date = pickedDateIso.takeIf { it != event.date },
                                types = if (selectedTypes.toList() != event.types?.map { it.name }) selectedTypes.toList() else null
                            )
                            api.updateEvent(event.id, updateBody)

                            // Delete photos marked for deletion
                            photosToDelete.forEach { photoId ->
                                try {
                                    api.deleteEventPhoto(photoId)
                                } catch (e: Exception) {
                                    Log.w("EditEventScreen", "Failed to delete photo $photoId", e)
                                }
                            }

                            // TODO: Upload new photos (would need multipart upload endpoint)
                            // For now, new photos are not uploaded

                            snackbarHostState.showSnackbar("Événement mis à jour", duration = SnackbarDuration.Short)
                            onBack()
                        } catch (e: Exception) {
                            Log.w("EditEventScreen", "Failed to update event", e)
                            updateError = e.message ?: "Erreur lors de la mise à jour"
                            snackbarHostState.showSnackbar("Erreur: ${updateError}", duration = SnackbarDuration.Short)
                        } finally {
                            updateLoading = false
                        }
                    }
                },
                enabled = title.isNotBlank() && description.isNotBlank() && location.isNotBlank() &&
                        pickedDateIso.isNotBlank() && selectedTypes.isNotEmpty() && !updateLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                if (updateLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text(
                        "Enregistrer les modifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            if (updateError != null) {
                Text(text = updateError!!, color = MaterialTheme.colorScheme.error)
            }
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

