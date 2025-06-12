package com.example.moneywise.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.moneywise.databinding.ActivityForgotPasswordBinding
import com.example.moneywise.utils.BiometricHelper
import com.example.moneywise.utils.ValidationHelper
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executor
import androidx.biometric.BiometricManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var userEmail: String = ""

    // Utilisation de Hilt avec @HiltViewModel
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBiometric()
        setupValidation()
        setupListeners()
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this as FragmentActivity,
            executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED -> {
                            Snackbar.make(binding.root, "Authentification annulée", Snackbar.LENGTH_SHORT).show()
                        }
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                            // Pas de biométrie configurée, permettre le changement direct
                            showPasswordResetForm()
                        }
                        else -> {
                            Snackbar.make(binding.root, "Erreur d'authentification: $errString", Snackbar.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Snackbar.make(binding.root, "Authentification réussie !", Snackbar.LENGTH_SHORT).show()
                    showPasswordResetForm()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Snackbar.make(binding.root, "Authentification échouée", Snackbar.LENGTH_SHORT).show()
                }
            })
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

        // Validation en temps réel pour le nouveau mot de passe
        binding.etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                val result = ValidationHelper.validatePassword(password)
                binding.tilNewPassword.error = if (result.isValid) null else result.errorMessage

                // Revalider la confirmation si elle existe
                val confirmPassword = binding.etConfirmNewPassword.text.toString()
                if (confirmPassword.isNotEmpty()) {
                    val confirmResult = ValidationHelper.validatePasswordConfirmation(password, confirmPassword)
                    binding.tilConfirmNewPassword.error = if (confirmResult.isValid) null else confirmResult.errorMessage
                }
            }
        })

        // Validation en temps réel pour la confirmation du nouveau mot de passe
        binding.etConfirmNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = binding.etNewPassword.text.toString()
                val confirmPassword = s.toString()
                val result = ValidationHelper.validatePasswordConfirmation(password, confirmPassword)
                binding.tilConfirmNewPassword.error = if (result.isValid) null else result.errorMessage
            }
        })
    }

    private fun setupListeners() {
        binding.btnVerifyEmail.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            if (validateEmail(email)) {
                userEmail = email
                checkUserAndProceedWithAuth(email)
            }
        }

        binding.btnResetPassword.setOnClickListener {
            val newPassword = binding.etNewPassword.text.toString()
            val confirmPassword = binding.etConfirmNewPassword.text.toString()

            if (validatePasswordInputs(newPassword, confirmPassword)) {
                resetPasswordMethod(userEmail, newPassword)
            }
        }

        binding.btnBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun checkUserAndProceedWithAuth(email: String) {
        binding.btnVerifyEmail.isEnabled = false
        binding.btnVerifyEmail.text = "Vérification..."

        // Utilisation de la méthode du ViewModel
        viewModel.checkUserExists(email) { exists ->
            binding.btnVerifyEmail.isEnabled = true
            binding.btnVerifyEmail.text = "Vérifier l'email"

            if (exists) {
                // Vérifier la disponibilité de l'authentification biométrique
                val biometricAvailability = BiometricHelper.isBiometricAvailable(this)
                val availableTypes = BiometricHelper.getAvailableAuthenticationTypes(this)

                when (biometricAvailability) {
                    BiometricHelper.BiometricAvailability.AVAILABLE -> {
                        // Authentification biométrique disponible
                        showBiometricPrompt(availableTypes)
                    }
                    BiometricHelper.BiometricAvailability.NONE_ENROLLED -> {
                        // Aucune biométrie configurée, permettre le changement direct
                        Snackbar.make(binding.root, "Aucune sécurité biométrique configurée. Accès direct autorisé.", Snackbar.LENGTH_LONG).show()
                        showPasswordResetForm()
                    }
                    else -> {
                        // Biométrie non disponible, permettre le changement direct
                        Snackbar.make(binding.root, "Authentification biométrique non disponible. Accès direct autorisé.", Snackbar.LENGTH_LONG).show()
                        showPasswordResetForm()
                    }
                }
            } else {
                binding.tilEmail.error = "Aucun compte trouvé avec cet email"
                Snackbar.make(binding.root, "Aucun compte trouvé avec cet email", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showBiometricPrompt(availableTypes: List<BiometricHelper.AuthenticationType>) {
        val message = BiometricHelper.getAuthenticationMessage(availableTypes)

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authentification requise")
            .setSubtitle("Confirmez votre identité pour réinitialiser le mot de passe")
            .setDescription(message)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showPasswordResetForm() {
        // Masquer le formulaire d'email et afficher le formulaire de réinitialisation
        binding.layoutEmailVerification.visibility = android.view.View.GONE
        binding.layoutPasswordReset.visibility = android.view.View.VISIBLE
    }

    // Renommage de la méthode pour éviter le conflit
    private fun resetPasswordMethod(email: String, newPassword: String) {
        binding.btnResetPassword.isEnabled = false
        binding.btnResetPassword.text = "Réinitialisation..."

        // Utilisation de la méthode du ViewModel
        viewModel.resetPassword(email, newPassword) { result ->
            binding.btnResetPassword.isEnabled = true
            binding.btnResetPassword.text = "Réinitialiser le mot de passe"

            result.fold(
                onSuccess = {
                    Snackbar.make(binding.root, "Mot de passe réinitialisé avec succès !", Snackbar.LENGTH_LONG).show()

                    // Rediriger vers la page de connexion après un délai
                    binding.root.postDelayed({
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }, 2000)
                },
                onFailure = { e ->
                    Snackbar.make(binding.root, e.message ?: "Erreur lors de la réinitialisation", Snackbar.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun validateEmail(email: String): Boolean {
        val result = ValidationHelper.validateEmail(email)
        if (!result.isValid) {
            binding.tilEmail.error = result.errorMessage
            return false
        } else {
            binding.tilEmail.error = null
            return true
        }
    }

    private fun validatePasswordInputs(newPassword: String, confirmPassword: String): Boolean {
        var isValid = true

        // Valider le nouveau mot de passe
        val passwordResult = ValidationHelper.validatePassword(newPassword)
        if (!passwordResult.isValid) {
            binding.tilNewPassword.error = passwordResult.errorMessage
            isValid = false
        } else {
            binding.tilNewPassword.error = null
        }

        // Valider la confirmation
        val confirmResult = ValidationHelper.validatePasswordConfirmation(newPassword, confirmPassword)
        if (!confirmResult.isValid) {
            binding.tilConfirmNewPassword.error = confirmResult.errorMessage
            isValid = false
        } else {
            binding.tilConfirmNewPassword.error = null
        }

        return isValid
    }
}