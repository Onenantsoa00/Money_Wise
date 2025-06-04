package com.example.moneywise.utils

/**
 * Classe utilitaire pour la validation des champs d'authentification
 */
object ValidationHelper {

    /**
     * Valide l'email
     * Règles : doit contenir "@" et "."
     */
    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isEmpty() -> ValidationResult(false, "L'email est requis")
            !email.contains("@") -> ValidationResult(false, "L'email doit contenir '@'")
            !email.contains(".") -> ValidationResult(false, "L'email doit contenir '.'")
            !email.contains("@") || !email.contains(".") -> ValidationResult(false, "Format d'email invalide")
            email.indexOf("@") > email.lastIndexOf(".") -> ValidationResult(false, "Format d'email invalide")
            else -> ValidationResult(true, "")
        }
    }

    /**
     * Valide le mot de passe
     * Règles : plus de 8 caractères, au moins un chiffre et une lettre
     */
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult(false, "Le mot de passe est requis")
            password.length <= 8 -> ValidationResult(false, "Le mot de passe doit contenir plus de 8 caractères")
            !password.any { it.isDigit() } -> ValidationResult(false, "Le mot de passe doit contenir au moins un chiffre")
            !password.any { it.isLetter() } -> ValidationResult(false, "Le mot de passe doit contenir au moins une lettre")
            else -> ValidationResult(true, "")
        }
    }

    /**
     * Valide le nom
     * Règles : requis et première lettre en majuscule
     */
    fun validateNom(nom: String): ValidationResult {
        return when {
            nom.isEmpty() -> ValidationResult(false, "Le nom est requis")
            !isProperCase(nom) -> ValidationResult(false, "Le nom doit commencer par une majuscule")
            else -> ValidationResult(true, "")
        }
    }

    /**
     * Valide le prénom
     * Règles : optionnel mais si renseigné, première lettre en majuscule
     */
    fun validatePrenom(prenom: String): ValidationResult {
        return when {
            prenom.isEmpty() -> ValidationResult(true, "") // Prénom optionnel
            !isProperCase(prenom) -> ValidationResult(false, "Le prénom doit commencer par une majuscule")
            else -> ValidationResult(true, "")
        }
    }

    /**
     * Valide la confirmation du mot de passe
     */
    fun validatePasswordConfirmation(password: String, confirmPassword: String): ValidationResult {
        return when {
            confirmPassword.isEmpty() -> ValidationResult(false, "Veuillez confirmer le mot de passe")
            password != confirmPassword -> ValidationResult(false, "Les mots de passe ne correspondent pas")
            else -> ValidationResult(true, "")
        }
    }

    /**
     * Convertit le texte en format "Title Case" (première lettre majuscule, reste minuscule)
     * Gère aussi les noms composés séparés par des espaces ou des tirets
     */
    fun toProperCase(text: String): String {
        if (text.isEmpty()) return text

        return text.split(" ", "-").joinToString(" ") { word ->
            if (word.isEmpty()) {
                word
            } else {
                word.lowercase().replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }
            }
        }.replace(" - ", "-") // Restaurer les tirets pour les noms composés
    }

    /**
     * Vérifie si le texte est au format "Title Case"
     */
    private fun isProperCase(text: String): Boolean {
        if (text.isEmpty()) return true

        // Vérifier chaque mot séparé par espace ou tiret
        val words = text.split(" ", "-")
        return words.all { word ->
            if (word.isEmpty()) {
                true
            } else {
                // Premier caractère doit être majuscule, le reste minuscule
                word[0].isUpperCase() && word.drop(1).all { it.isLowerCase() || !it.isLetter() }
            }
        }
    }

    /**
     * Classe pour encapsuler le résultat de validation
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String
    )
}