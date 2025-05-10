package com.example.moneywise.ui.acquittement

import androidx.lifecycle.ViewModel

class AcquittementViewModel : ViewModel() {
    // Données statiques pour les acquittements
    val acquittements = listOf(
        Acquittement("Jean Rakoto", "034 12 345 67", "500,000 MGA", "15/06/2023", "20/06/2023"),
        Acquittement("Marie Randria", "032 98 765 43", "300,000 MGA", "10/06/2023", "15/06/2023")
    )

    data class Acquittement(
        val nom: String,
        val contact: String,
        val montant: String,
        val dateCredit: String,
        val dateRemise: String
    )
}