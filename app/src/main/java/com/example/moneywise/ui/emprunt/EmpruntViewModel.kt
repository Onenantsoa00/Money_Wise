package com.example.moneywise.ui.emprunt

import androidx.lifecycle.ViewModel

class EmpruntViewModel : ViewModel() {
    // Données statiques pour les emprunts
    val emprunts = listOf(
        Emprunt("Jean Dupont", "034 12 345 67", "250,000 MGA", "10/06/2023", "10/07/2023"),
        Emprunt("Marie Rakoto", "032 98 765 43", "150,000 MGA", "05/06/2023", "05/07/2023")
    )

    data class Emprunt(
        val nom: String,
        val contact: String,
        val montant: String,
        val dateEmprunt: String,
        val dateRemboursement: String
    )
}