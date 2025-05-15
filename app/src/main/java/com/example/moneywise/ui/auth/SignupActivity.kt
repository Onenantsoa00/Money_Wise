package com.example.moneywise.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.databinding.ActivitySignupBinding
import com.google.android.material.snackbar.Snackbar

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(AppDatabase.getDatabase(this).utilisateurDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignUp.setOnClickListener {
            val nom = binding.etNom.text?.toString() ?: "" // Utilisez le safe call et l'opérateur elvis
            val prenom = binding.etPrenom.text?.toString() ?: ""
            val email = binding.etEmail.text?.toString() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            val confirmPassword = binding.etConfirmPassword.text?.toString() ?: ""
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSignUp.setOnClickListener {
            val nom = binding.etNom.text.toString()
            val prenom = binding.etPrenom.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (validateInputs(nom, prenom, email, password, confirmPassword)) {
                viewModel.registerUser(nom, prenom, email, password) { result ->
                    result.fold(
                        onSuccess = {
                            Snackbar.make(binding.root, "Inscription réussie!", Snackbar.LENGTH_LONG).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        },
                        onFailure = { e ->
                            Snackbar.make(binding.root, e.message ?: "Erreur d'inscription", Snackbar.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun validateInputs(
        nom: String,
        prenom: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        if (nom.isEmpty()) {
            binding.tilNom.error = getString(R.string.error_name_required)
            isValid = false
        }
        if (prenom.isEmpty()) {
            binding.tilPrenom.error = getString(R.string.error_firstname_required)
            isValid = false
        }
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_required)
            isValid = false
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            isValid = false
        }
        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch)
            isValid = false
        }
        if (!binding.cbTerms.isChecked) {
            Snackbar.make(binding.root, "Veuillez accepter les conditions", Snackbar.LENGTH_LONG).show()
            isValid = false
        }

        return isValid
    }
}