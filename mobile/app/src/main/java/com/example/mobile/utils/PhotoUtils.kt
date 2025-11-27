package com.example.mobile.utils

import com.example.mobile.network.PhotoDto

/**
 * Filter supported photo formats and normalize URLs for Android.
 * - Removes HEIC format photos (not well supported on Android)
 * - Replaces localhost URLs with 10.0.2.2 (Android emulator host IP)
 */
fun getSupportedPhotos(photos: List<PhotoDto>?): List<PhotoDto> {
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

/**
 * Get the first supported photo URL for event cards.
 */
fun getFirstPhotoUrl(photos: List<PhotoDto>?): String? {
    return getSupportedPhotos(photos).firstOrNull()?.url
}

