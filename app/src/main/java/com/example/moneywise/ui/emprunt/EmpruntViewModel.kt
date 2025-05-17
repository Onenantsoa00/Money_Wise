package com.example.moneywise.ui.emprunt

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Emprunt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class EmpruntViewModel @Inject constructor(
    private val db: AppDatabase
) : ViewModel() {
    val allEmprunts: LiveData<List<Emprunt>> = db.empruntDao().getAllEmprunt()
    val empruntsNonRembourses: LiveData<List<Emprunt>> = db.empruntDao().getEmpruntsNonRembourses()

    fun ajouterEmprunt(
        nom: String,
        contact: String,
        montant: Double,
        dateEmprunt: Date,
        dateRemboursement: Date,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val nouvelEmprunt = Emprunt(
                    nom_emprunte = nom,
                    contacte = contact,
                    montant = montant,
                    date_emprunt = dateEmprunt,
                    date_remboursement = dateRemboursement,
                    estRembourse = false
                )

                db.empruntDao().insertEmprunt(nouvelEmprunt)

                db.utilisateurDao().getFirstUtilisateur()?.let { user ->
                    val newBalance = user.solde + montant
                    db.utilisateurDao().update(user.copy(solde = newBalance))
                }

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun rembourserEmprunt(
        emprunt: Emprunt,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                db.empruntDao().updateRemboursementStatus(emprunt.id, true)

                db.utilisateurDao().getFirstUtilisateur()?.let { user ->
                    val newBalance = user.solde - emprunt.montant
                    db.utilisateurDao().update(user.copy(solde = newBalance))
                }

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur inconnue")
            }
        }
    }
}