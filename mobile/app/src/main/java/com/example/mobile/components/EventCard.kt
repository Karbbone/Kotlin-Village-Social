package com.example.mobile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mobile.network.EventDto
import com.example.mobile.network.PhotoDto

@Composable
fun EventCard(
    event: EventDto,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val formatter = remember { java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm").withZone(java.time.ZoneId.systemDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            val photoUrl = getFirstPhotoUrl(event.photos)
            if (photoUrl != null) {
                AsyncImage(
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

                val dt = event.date?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
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

                if (!event.types.isNullOrEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(event.types) { type ->
                            Card(
                                colors = CardDefaults.cardColors(
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

private fun getSupportedPhotos(photos: List<PhotoDto>?): List<PhotoDto> {
    if (photos.isNullOrEmpty()) return emptyList()
    return photos
        .filter { photo ->
            val url = photo.url.lowercase()
            !url.endsWith(".heic") && !url.contains(".heic?")
        }
        .map { photo ->
            photo.copy(url = photo.url.replace("http://localhost:", "http://10.0.2.2:"))
        }
}

private fun getFirstPhotoUrl(photos: List<PhotoDto>?): String? {
    return getSupportedPhotos(photos).firstOrNull()?.url
}

