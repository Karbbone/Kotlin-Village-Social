package com.example.mobile.ui.login

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.mobile.auth.AuthRepository
import com.example.mobile.network.ApiService
import com.example.mobile.network.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException

@Composable
fun LoginScreen(
    api: ApiService,
    authRepo: AuthRepository,
    onNavigateToRegister: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    fun validateAndLogin() {
        val emailOk = email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val passOk = password.isNotEmpty()
        if (!emailOk) {
            errorText = "Veuillez saisir une adresse e-mail valide."
            return
        }
        if (!passOk) {
            errorText = "Le mot de passe est invalide."
            return
        }
        errorText = null
        if (isLoading) return
        isLoading = true
        coroutineScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { api.login(LoginRequest(email.trim(), password)) }
                val userJson = JSONObject().apply {
                    put("id", resp.user.id)
                    put("email", resp.user.email)
                    put("displayName", resp.user.displayName)
                }.toString()
                withContext(Dispatchers.IO) { authRepo.saveToken(resp.access_token, userJson) }
                // authRepo change will be observed by MainActivity and navigate/show snackbar
            } catch (e: HttpException) {
                val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                val msg = if (!body.isNullOrBlank()) {
                    try { JSONObject(body).optString("message", body) } catch (_: Exception) { body }
                } else e.message()
                errorText = msg ?: "Erreur lors de la connexion"
            } catch (e: Exception) {
                errorText = e.localizedMessage ?: "Erreur réseau"
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Connexion",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showPassword) "Masquer le mot de passe" else "Afficher le mot de passe"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { validateAndLogin() })
        )
        if (errorText != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorText!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { validateAndLogin() },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotBlank() && password.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Se connecter")
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Créer un compte")
        }
    }
}
