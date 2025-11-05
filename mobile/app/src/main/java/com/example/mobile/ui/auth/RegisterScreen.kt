package com.example.mobile.ui.auth

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.mobile.auth.AuthRepository
import com.example.mobile.network.ApiService
import com.example.mobile.network.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException

@Composable
fun RegisterScreen(
    api: ApiService,
    authRepo: AuthRepository,
    modifier: Modifier = Modifier,
    onRegister: (firstName: String, lastName: String, email: String, password: String) -> Unit = { _, _, _, _ -> },
    onNavigateToLogin: () -> Unit = {},
) {
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var submitAttempted by rememberSaveable { mutableStateOf(false) }

    val emailValid = email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val passwordMinLengthOk = password.length >= 8
    val passwordsMatch = password.isNotBlank() && confirm.isNotBlank() && password == confirm

    val formValid = firstName.isNotBlank() && lastName.isNotBlank() && emailValid && passwordMinLengthOk && passwordsMatch

    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }

    var isLoading by rememberSaveable { mutableStateOf(false) }
    var registerError by rememberSaveable { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Créer un compte", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("*", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(4.dp))
                Text("Champs obligatoires", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))

            // Prénom / Nom (deux champs côte à côte si possible)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = submitAttempted && firstName.isBlank(),
                    supportingText = {
                        if (submitAttempted && firstName.isBlank()) {
                            Text("Champ obligatoire")
                        }
                    },
                    label = { RequiredLabel("Prénom") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = submitAttempted && lastName.isBlank(),
                    supportingText = {
                        if (submitAttempted && lastName.isBlank()) {
                            Text("Champ obligatoire")
                        }
                    },
                    label = { RequiredLabel("Nom") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = (submitAttempted && email.isBlank()) || (email.isNotBlank() && !emailValid),
                supportingText = {
                    when {
                        submitAttempted && email.isBlank() -> Text("Champ obligatoire")
                        email.isNotBlank() && !emailValid -> Text("Email invalide")
                    }
                },
                label = { RequiredLabel("Email") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = (submitAttempted && password.isBlank()) || (password.isNotBlank() && !passwordMinLengthOk),
                supportingText = {
                    when {
                        submitAttempted && password.isBlank() -> Text("Champ obligatoire")
                        password.isNotBlank() && !passwordMinLengthOk -> Text("Au moins 8 caractères")
                    }
                },
                label = { RequiredLabel("Mot de passe") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "Masquer le mot de passe" else "Afficher le mot de passe"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = (submitAttempted && confirm.isBlank()) || (confirm.isNotBlank() && !passwordsMatch),
                supportingText = {
                    when {
                        submitAttempted && confirm.isBlank() -> Text("Champ obligatoire")
                        confirm.isNotBlank() && !passwordsMatch -> Text("Les mots de passe ne correspondent pas")
                    }
                },
                label = { RequiredLabel("Confirmation de mot de passe") },
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                        Icon(
                            imageVector = if (confirmVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (confirmVisible) "Masquer le mot de passe" else "Afficher le mot de passe"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(Modifier.height(16.dp))

            if (registerError.isNotBlank()) {
                Text(registerError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    submitAttempted = true
                    registerError = ""
                    if (formValid && !isLoading) {
                        isLoading = true
                        val displayName = "${firstName.trim()} ${lastName.trim()}"
                        coroutineScope.launch {
                            try {
                                val resp = withContext(Dispatchers.IO) { api.register(RegisterRequest(displayName, email.trim(), password)) }
                                val userJson = JSONObject().apply {
                                    put("id", resp.user.id)
                                    put("email", resp.user.email)
                                    put("displayName", resp.user.displayName)
                                }.toString()
                                withContext(Dispatchers.IO) { authRepo.saveToken(resp.access_token, userJson) }
                                onRegister(firstName.trim(), lastName.trim(), email.trim(), password)
                            } catch (e: HttpException) {
                                val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                                val msg = if (!body.isNullOrBlank()) {
                                    try { JSONObject(body).optString("message", body) } catch (_: Exception) { body }
                                } else e.message()
                                registerError = msg ?: "Erreur lors de l'inscription"
                            } catch (e: Exception) {
                                registerError = e.localizedMessage ?: "Erreur réseau"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = formValid && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Création..." else "Créer mon compte")
            }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Se connecter") }

            Spacer(Modifier.height(24.dp))
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
