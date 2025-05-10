package com.example.moneywise.ui.historique

import androidx.lifecycle.ViewModel

class HistoriqueViewModel : ViewModel() {
    // Données statiques pour l'historique
    val transactions = listOf(
        Transaction("Dépot", "+500,000", "15/06/2023", "Vente produit A", colorGreen = true),
        Transaction("Retrait", "-150,000", "14/06/2023", "Achat matériel", colorGreen = false)
    )

    data class Transaction(
        val type: String,
        val montant: String,
        val date: String,
        val motif: String,
        val colorGreen: Boolean
    )
}