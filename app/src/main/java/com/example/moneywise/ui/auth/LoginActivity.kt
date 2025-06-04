package com.example.moneywise.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.moneywise.MainActivity
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.databinding.ActivityLoginBinding
import com.example.moneywise.utils.SessionManager
import com.example.moneywise.utils.ValidationHelper
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialiser le gestionnaire de session
        sessionManager = SessionManager(this)

        // Vérifier si l'utilisateur est déjà connecté
        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setupValidation()
        setupListeners()
    }

    private fun setupValidation() {
        // Validation en temps réel pour l'email
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val email = s.toString()
                val result = ValidationHelper.validateEmail(email)
                binding.tilEmail.error = if (result.isValid) null else result.errorMessage
            }
        })

        // Validation en temps réel pour le mot de passe
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                val result = ValidationHelper.validatePassword(password)
                binding.tilPassword.error = if (result.isValid) null else result.errorMessage
            }
        })
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateAllInputs(email, password)) {
                // Désactiver le bouton pendant la connexion
                binding.btnLogin.isEnabled = false
                binding.btnLogin.text = "Connexion..."

                viewModel.loginUser(email, password) { result ->
                    // Réactiver le bouton
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Se connecter"

                    result.fold(
                        onSuccess = { user ->
                            // Enregistrer la session de l'utilisateur
                            sessionManager.createLoginSession(user.id, user.email)

                            Snackbar.make(binding.root, "Connexion réussie !", Snackbar.LENGTH_SHORT).show()

                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        },
                        onFailure = { e ->
                            Snackbar.make(binding.root, e.message ?: "Erreur de connexion", Snackbar.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // 🔥 NOUVEAU: Gestion du mot de passe oublié
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun validateAllInputs(email: String, password: String): Boolean {
        var isValid = true

        // Valider l'email
        val emailResult = ValidationHelper.validateEmail(email)
        if (!emailResult.isValid) {
            binding.tilEmail.error = emailResult.errorMessage
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        // Valider le mot de passe
        val passwordResult = ValidationHelper.validatePassword(password)
        if (!passwordResult.isValid) {
            binding.tilPassword.error = passwordResult.errorMessage
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }
}