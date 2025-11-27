package com.example.mobile.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream

/**
 * Convert a list of image URIs to MultipartBody.Part for upload
 */
fun urisToMultipartParts(context: Context, uris: List<Uri>): List<MultipartBody.Part> {
    return uris.mapNotNull { uri ->
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes != null) {
                val fileName = getFileName(context, uri) ?: "photo_${System.currentTimeMillis()}.jpg"
                val requestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("photos", fileName, requestBody)
            } else null
        } catch (e: Exception) {
            android.util.Log.e("MultipartUtils", "Error converting URI to multipart: $uri", e)
            null
        }
    }
}

/**
 * Get filename from URI
 */
private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = it.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result
}

/**
 * Create RequestBody from string
 */
fun String.toRequestBody(): RequestBody {
    return this.toRequestBody("text/plain".toMediaTypeOrNull())
}

/**
 * Convert a list of strings to MultipartBody.Part list for types
 */
fun stringsToMultipartParts(key: String, values: List<String>): List<MultipartBody.Part> {
    return values.map { value ->
        MultipartBody.Part.createFormData(key, value)
    }
}

