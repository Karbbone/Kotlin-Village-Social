package com.example.social

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Convert dp to px
        fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

        // Root layout
        val root = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        // Titre
        val title = TextView(this).apply {
            text = "Connexion"
            textSize = 24f
            setPadding(0, 0, 0, dp(12))
        }

        // Email
        val emailInput = EditText(this).apply {
            id = View.generateViewId()
            hint = "Email"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            // importantForAutofill requires API 26+ -> protéger l'appel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }

        // Mot de passe
        val passwordInput = EditText(this).apply {
            id = View.generateViewId()
            hint = "Mot de passe"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            transformationMethod = PasswordTransformationMethod.getInstance()
            // importantForAutofill requires API 26+ -> protéger l'appel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(20)
            }
        }

        // Bouton connexion
        val loginButton = Button(this).apply {
            id = View.generateViewId()
            text = "Se connecter"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Ajouter les vues
        root.addView(title)
        root.addView(emailInput)
        root.addView(passwordInput)
        root.addView(loginButton)

        // Appliquer le layout construit
        setContentView(root)

        // Gérer les insets (status/navigation bars)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Validation basique
        loginButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim() ?: ""
            val password = passwordInput.text?.toString() ?: ""

            when {
                email.isEmpty() -> Toast.makeText(this, "Veuillez saisir une adresse email", Toast.LENGTH_SHORT).show()
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> Toast.makeText(this, "Adresse email invalide", Toast.LENGTH_SHORT).show()
                password.length < 6 -> Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(this, "Connexion réussie (simulée)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}