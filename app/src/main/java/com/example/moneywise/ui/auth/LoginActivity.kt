package com.example.moneywise.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.moneywise.MainActivity
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.databinding.ActivityLoginBinding
import com.example.moneywise.utils.SessionManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(AppDatabase.getDatabase(this).utilisateurDao())
    }

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

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (validateInputs(email, password)) {
                viewModel.loginUser(email, password) { result ->
                    result.fold(
                        onSuccess = { user ->
                            // Enregistrer la session de l'utilisateur
                            sessionManager.createLoginSession(user.id, user.email)

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
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_required)
            return false
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            return false
        }
        return true
    }
}