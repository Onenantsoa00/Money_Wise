package com.example.moneywise.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.databinding.ActivitySignupBinding
import com.example.moneywise.utils.ValidationHelper
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

        setupValidation()
        setupListeners()
    }

    private fun setupValidation() {
        // Validation en temps réel pour le nom avec conversion en "Title Case"
        binding.etNom.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val nom = s.toString()

                // Convertir automatiquement en "Title Case" (première lettre majuscule)
                val properCaseNom = ValidationHelper.toProperCase(nom)
                if (nom != properCaseNom) {
                    binding.etNom.removeTextChangedListener(this)
                    binding.etNom.setText(properCaseNom)
                    binding.etNom.setSelection(properCaseNom.length)
                    binding.etNom.addTextChangedListener(this)
                }

                val result = ValidationHelper.validateNom(properCaseNom)
                binding.tilNom.error = if (result.isValid) null else result.errorMessage
            }
        })

        // Validation en temps réel pour le prénom avec conversion en "Title Case"
        binding.etPrenom.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val prenom = s.toString()

                // Convertir automatiquement en "Title Case" si non vide
                if (prenom.isNotEmpty()) {
                    val properCasePrenom = ValidationHelper.toProperCase(prenom)
                    if (prenom != properCasePrenom) {
                        binding.etPrenom.removeTextChangedListener(this)
                        binding.etPrenom.setText(properCasePrenom)
                        binding.etPrenom.setSelection(properCasePrenom.length)
                        binding.etPrenom.addTextChangedListener(this)
                    }
                }

                val result = ValidationHelper.validatePrenom(prenom)
                binding.tilPrenom.error = if (result.isValid) null else result.errorMessage
            }
        })

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

                // Revalider la confirmation si elle existe
                val confirmPassword = binding.etConfirmPassword.text.toString()
                if (confirmPassword.isNotEmpty()) {
                    val confirmResult = ValidationHelper.validatePasswordConfirmation(password, confirmPassword)
                    binding.tilConfirmPassword.error = if (confirmResult.isValid) null else confirmResult.errorMessage
                }
            }
        })

        // Validation en temps réel pour la confirmation du mot de passe
        binding.etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = binding.etPassword.text.toString()
                val confirmPassword = s.toString()
                val result = ValidationHelper.validatePasswordConfirmation(password, confirmPassword)
                binding.tilConfirmPassword.error = if (result.isValid) null else result.errorMessage
            }
        })
    }

    private fun setupListeners() {
        binding.btnSignUp.setOnClickListener {
            val nom = binding.etNom.text.toString().trim()
            val prenom = binding.etPrenom.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (validateAllInputs(nom, prenom, email, password, confirmPassword)) {
                if (!binding.cbTerms.isChecked) {
                    Snackbar.make(binding.root, "Veuillez accepter les conditions d'utilisation", Snackbar.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                // Désactiver le bouton pendant l'inscription
                binding.btnSignUp.isEnabled = false
                binding.btnSignUp.text = "Inscription..."

                viewModel.registerUser(nom, prenom, email, password) { result ->
                    // Réactiver le bouton
                    binding.btnSignUp.isEnabled = true
                    binding.btnSignUp.text = "S'inscrire"

                    result.fold(
                        onSuccess = {
                            Snackbar.make(binding.root, "Inscription réussie ! Vous pouvez maintenant vous connecter.", Snackbar.LENGTH_LONG).show()

                            // Rediriger vers la page de connexion après un délai
                            binding.root.postDelayed({
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }, 1500)
                        },
                        onFailure = { e ->
                            when {
                                e.message?.contains("UNIQUE constraint failed") == true -> {
                                    binding.tilEmail.error = "Cet email est déjà utilisé"
                                    Snackbar.make(binding.root, "Cet email est déjà utilisé", Snackbar.LENGTH_LONG).show()
                                }
                                else -> {
                                    Snackbar.make(binding.root, e.message ?: "Erreur d'inscription", Snackbar.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.tvTerms.setOnClickListener {
            Snackbar.make(binding.root, "Conditions d'utilisation - Fonctionnalité à venir", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun validateAllInputs(
        nom: String,
        prenom: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        // Valider le nom
        val nomResult = ValidationHelper.validateNom(nom)
        if (!nomResult.isValid) {
            binding.tilNom.error = nomResult.errorMessage
            isValid = false
        } else {
            binding.tilNom.error = null
        }

        // Valider le prénom (optionnel)
        val prenomResult = ValidationHelper.validatePrenom(prenom)
        if (!prenomResult.isValid) {
            binding.tilPrenom.error = prenomResult.errorMessage
            isValid = false
        } else {
            binding.tilPrenom.error = null
        }

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

        // Valider la confirmation du mot de passe
        val confirmResult = ValidationHelper.validatePasswordConfirmation(password, confirmPassword)
        if (!confirmResult.isValid) {
            binding.tilConfirmPassword.error = confirmResult.errorMessage
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return isValid
    }
}