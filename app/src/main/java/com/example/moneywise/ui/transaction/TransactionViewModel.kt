package com.example.moneywise.ui.transaction

import androidx.lifecycle.ViewModel
import com.example.moneywise.R

class TransactionViewModel : ViewModel() {
    // Données statiques pour les transactions
    val transactions = listOf(
        Transaction("Dépôt", "15/06/2023", "+500,000 MGA", true, R.drawable.ic_depot),
        Transaction("Retrait", "14/06/2023", "-150,000 MGA", false, R.drawable.ic_retrait),
        Transaction("Transfert", "13/06/2023", "-200,000 MGA", false, R.drawable.ic_transfer)
    )

    data class Transaction(
        val type: String,
        val date: String,
        val montant: String,
        val isDepot: Boolean,
        val iconRes: Int
    )
}