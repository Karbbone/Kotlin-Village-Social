package com.example.mobile.cities

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mobile.network.ApiService
import com.example.mobile.network.CityDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

private val Context.cityStore by preferencesDataStore(name = "cities_cache")

enum class LoadSource { NONE, CACHE, NETWORK }
data class CitiesStatus(val source: LoadSource, val count: Int, val error: String? = null)

class CitiesRepository(private val context: Context, private val api: ApiService) {
    private val CITIES_JSON = stringPreferencesKey("cities_json")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _cities = MutableStateFlow<List<CityDto>>(emptyList())
    val cities: StateFlow<List<CityDto>> = _cities

    private val _status = MutableStateFlow(CitiesStatus(LoadSource.NONE, 0, null))
    val status: StateFlow<CitiesStatus> = _status

    private val isRefreshing = AtomicBoolean(false)

    fun ensureLoaded() {
        scope.launch {
            var fetchedFromNetwork = false
            // Try load from DataStore first
            val json = runCatching { context.cityStore.data.first()[CITIES_JSON] }.getOrNull()
            if (!json.isNullOrBlank()) {
                runCatching { decode(json) }
                    .onSuccess { list ->
                        if (list.isNotEmpty()) {
                            _cities.value = list
                            _status.value = CitiesStatus(LoadSource.CACHE, list.size, null)
                            Log.d("CitiesRepository", "Loaded ${list.size} cities from cache")
                            val sample = list.take(5).joinToString(
                                separator = ", ",
                                transform = { "${it.name} (${it.postalCode})" }
                            )
                            Log.i("CitiesRepository", "Cities sample (CACHE): ${sample}")
                        }
                    }
                    .onFailure { e ->
                        Log.w("CitiesRepository", "Failed to decode cached cities: ${e.message}", e)
                        _status.value = CitiesStatus(LoadSource.NONE, 0, "Cache illisible: ${e.message}")
                    }
            }
            // If empty, fetch from API and persist
            if (_cities.value.isEmpty()) {
                if (!isNetworkAvailable(context)) {
                    val msg = "Pas de connexion internet (Wi‑Fi/data indisponible)"
                    Log.e("CitiesRepository", msg)
                    _status.value = CitiesStatus(LoadSource.NONE, 0, msg)
                    return@launch
                }
                val result = runCatching { api.getCities() }
                result.onSuccess { remote ->
                    if (remote.isNotEmpty()) {
                        _cities.value = remote
                        _status.value = CitiesStatus(LoadSource.NETWORK, remote.size, null)
                        Log.d("CitiesRepository", "Fetched ${remote.size} cities from network, saving to cache")
                        val sample = remote.take(5).joinToString(
                            separator = ", ",
                            transform = { "${it.name} (${it.postalCode})" }
                        )
                        Log.i("CitiesRepository", "Cities sample (NETWORK): ${sample}")
                        context.cityStore.edit { prefs ->
                            prefs[CITIES_JSON] = encode(remote)
                        }
                        fetchedFromNetwork = true
                    } else {
                        val msg = "La liste des villes est vide (GET /cities/)."
                        Log.w("CitiesRepository", msg)
                        _status.value = CitiesStatus(LoadSource.NONE, 0, msg)
                    }
                }.onFailure { e ->
                    val reason = e.message ?: e::class.java.simpleName
                    val hint = if (reason.contains("Unable to resolve host") || reason.contains("ENETUNREACH", true)) {
                        "Impossible de résoudre l'hôte (DNS). Vérifiez la connexion, le VPN/proxy, ou réessayez plus tard."
                    } else {
                        null
                    }
                    val msg = "Échec du chargement des villes depuis https://mobile.maillet.bzh/cities/: ${reason}${if (hint != null) "\n${hint}" else ""}"
                    Log.e("CitiesRepository", msg, e)
                    if (_cities.value.isEmpty()) {
                        _status.value = CitiesStatus(LoadSource.NONE, 0, msg)
                    }
                }
            }
            // Always try a background refresh at startup to update cache/state
            if (!fetchedFromNetwork) {
                refreshInBackground()
            }
        }
    }

    fun refreshInBackground() {
        if (!isRefreshing.compareAndSet(false, true)) return
        scope.launch {
            try {
                val remote = runCatching { api.getCities() }.getOrElse { throw it }
                if (remote.isNotEmpty()) {
                    _cities.value = remote
                    _status.value = CitiesStatus(LoadSource.NETWORK, remote.size, null)
                    Log.d("CitiesRepository", "Background refresh fetched ${remote.size} cities, updating cache")
                    context.cityStore.edit { prefs ->
                        prefs[CITIES_JSON] = encode(remote)
                    }
                } else {
                    Log.w("CitiesRepository", "Background refresh returned empty list")
                }
            } catch (e: Exception) {
                Log.w("CitiesRepository", "Background refresh failed: ${e.message}")
            } finally {
                isRefreshing.set(false)
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        // Gracefully handle missing permission to avoid crashes
        val hasPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_NETWORK_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return true // fallback: assume network available, let the call fail gracefully
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nw = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(nw) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (_: SecurityException) {
            true // do not crash; allow code to attempt network and handle errors via Retrofit
        } catch (_: Throwable) {
            false
        }
    }

    private fun encode(list: List<CityDto>): String {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("postalCode", c.postalCode)
                }
            )
        }
        return arr.toString()
    }

    private fun decode(json: String): List<CityDto> {
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            CityDto(
                id = o.optInt("id"),
                name = o.optString("name"),
                postalCode = o.optString("postalCode")
            )
        }
    }
}
