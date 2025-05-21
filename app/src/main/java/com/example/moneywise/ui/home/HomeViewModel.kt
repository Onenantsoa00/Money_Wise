package com.example.moneywise.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Acquittement
import com.example.moneywise.data.entity.Banque
import com.example.moneywise.data.entity.Emprunt
import com.example.moneywise.data.entity.Projet
import com.example.moneywise.data.entity.Transaction
import com.example.moneywise.data.entity.Utilisateur
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.*

class HomeViewModel(private val db: AppDatabase) : ViewModel() {

    // Récupère les 5 emprunts non remboursés les plus récents
    val empruntsRecents: LiveData<List<Emprunt>> = db.empruntDao().getRecentEmpruntsNonRembourses()

    // Récupère les 5 acquittements les plus récents
    val acquittementsRecents: LiveData<List<Acquittement>> = db.AcquittementDao().getRecentAcquittements()

    // Solde de l'utilisateur
    val soldeUtilisateur: LiveData<Double?> = db.utilisateurDao().getSoldeUtilisateur()

    // Nom de l'utilisateur
    val nomUtilisateur: LiveData<String?> = db.utilisateurDao().getNomUtilisateur()

    // Pour récupérer les projets
    val projetsRecents: LiveData<List<Projet>> = db.ProjetDao().getRecentProjects()

    // Pour les transactions récentes
    val transactionsRecentes: LiveData<List<Transaction>> = db.transactionDao().getRecentTransactions()

    // Banques disponibles (converti en LiveData)
    private val _banks = MutableLiveData<List<String>>()
    val banks: LiveData<List<String>> = _banks

    init {
        loadBanks()
    }

    // Charge les banques depuis la base de données
    private fun loadBanks() {
        viewModelScope.launch {
            db.banqueDao().getAllBanques().collect { banksList ->
                _banks.value = banksList.map { it.nom }
            }
        }
    }

    // Récupère l'utilisateur courant
    private suspend fun getCurrentUser(): Utilisateur? {
        return db.utilisateurDao().getFirstUtilisateur()
    }

    // Ajoute une transaction
    fun addTransaction(
        type: String,
        amount: Double,
        date: Date,
        bankName: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val user = getCurrentUser()
                user?.let { utilisateur ->
                    // Récupérer l'ID de la banque si spécifiée
                    var bankId = 0
                    bankName?.let { name ->
                        val bank = db.banqueDao().getBanqueByNom(name)
                        bankId = bank?.id ?: 0
                    }

                    // Créer la transaction
                    val transaction = Transaction(
                        type = type,
                        montants = amount.toString(),
                        date = date,
                        id_utilisateur = utilisateur.id,
                        id_banque = bankId
                    )

                    // Insérer la transaction
                    db.transactionDao().insertTransaction(transaction)

                    // Mettre à jour le solde de l'utilisateur
                    val newBalance = when (type) {
                        "Dépôt" -> utilisateur.solde + amount
                        "Retrait" -> utilisateur.solde - amount
                        else -> utilisateur.solde
                    }
                    db.utilisateurDao().update(utilisateur.copy(solde = newBalance))

                    onSuccess()
                } ?: run {
                    onError("Aucun utilisateur trouvé")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Une erreur est survenue")
            }
        }
    }

    // Rafraîchit les données
    fun refreshData() {
        viewModelScope.launch {
            loadBanks()
            // Les LiveData se mettront à jour automatiquement
        }
    }
}