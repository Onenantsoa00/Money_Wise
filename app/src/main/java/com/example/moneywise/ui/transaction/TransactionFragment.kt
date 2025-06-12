package com.example.moneywise.ui.transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Transaction
import com.example.moneywise.databinding.FragmentTransactionBinding
import com.example.moneywise.expenses.TransactionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TransactionFragment : Fragment() {

    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var db: AppDatabase

    private val transactionViewModel: TransactionViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter
    private var showAllTransactions = false

    // Variables pour le filtrage
    private var currentTransactions: List<Transaction> = emptyList()

    // Enum pour les types de tri
    enum class SortType {
        TYPE_A_Z,
        TYPE_Z_A,
        MONTANT_CROISSANT,
        MONTANT_DECROISSANT,
        DATE_RECENT,
        DATE_ANCIEN
    }

    private var currentSortType = SortType.DATE_RECENT

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            db.transactionDao().getAllTransaction().collect { transactions ->
                currentTransactions = transactions
                updateSummary(transactions)
                applySorting(transactions)
            }
        }
    }

    // Appliquer le tri
    private fun applySorting(transactions: List<Transaction>) {
        val sortedList = when (currentSortType) {
            SortType.TYPE_A_Z -> transactions.sortedBy { it.type.lowercase() }
            SortType.TYPE_Z_A -> transactions.sortedByDescending { it.type.lowercase() }
            SortType.MONTANT_CROISSANT -> transactions.sortedBy {
                it.montants.replace(",", "").replace(" ", "").toDoubleOrNull() ?: 0.0
            }
            SortType.MONTANT_DECROISSANT -> transactions.sortedByDescending {
                it.montants.replace(",", "").replace(" ", "").toDoubleOrNull() ?: 0.0
            }
            SortType.DATE_RECENT -> transactions.sortedByDescending { it.date }
            SortType.DATE_ANCIEN -> transactions.sortedBy { it.date }
        }

        updateTransactionsList(sortedList)
    }

    private fun updateTransactionsList(transactions: List<Transaction>) {
        val transactionsToShow = if (showAllTransactions) {
            transactions
        } else {
            transactions.take(3)
        }
        transactionAdapter.submitList(transactionsToShow)

        // Masquer le bouton s'il n'y a pas assez de transactions
        binding.btnSeeAllTransactions.visibility =
            if (currentTransactions.size > 3 && !showAllTransactions) View.VISIBLE else View.GONE
    }

    private fun updateSummary(transactions: List<Transaction>) {
        val deposits = transactions.filter { it.type == "Dépôt" }.sumOf { it.montants.toDouble() }
        val withdrawals = transactions.filter { it.type == "Retrait" }.sumOf { it.montants.toDouble() }
        val balance = deposits - withdrawals

        binding.apply {
            textDepositAmount.text = "+${formatAmount(deposits)}"
            textWithdrawalAmount.text = "-${formatAmount(withdrawals)}"
            textBalanceAmount.text = if (balance >= 0) "+${formatAmount(balance)}" else "-${formatAmount(-balance)}"
        }
    }

    private fun formatAmount(amount: Double): String {
        return String.format(Locale.getDefault(), "%.0f", amount)
    }

    private fun setupClickListeners() {
        binding.fabAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }

        binding.btnSeeAllTransactions.setOnClickListener {
            showAllTransactions = true
            binding.btnSeeAllTransactions.visibility = View.GONE
            // Réappliquer le tri avec toutes les transactions
            applySorting(currentTransactions)
        }

        // Bouton Filtrer
        binding.btnFilterTransaction.setOnClickListener {
            showFilterDialog()
        }
    }

    // Afficher la modal de filtrage
    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_filter_transaction, null)

        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupFilterTransactions)

        // Sélectionner le tri actuel
        when (currentSortType) {
            SortType.TYPE_A_Z -> radioGroup.check(R.id.radioTypeAZTransaction1)
            SortType.TYPE_Z_A -> radioGroup.check(R.id.radioTypeZATransaction1)
            SortType.MONTANT_CROISSANT -> radioGroup.check(R.id.radioMontantCroissantTransaction1)
            SortType.MONTANT_DECROISSANT -> radioGroup.check(R.id.radioMontantDecroissantTransaction1)
            SortType.DATE_RECENT -> radioGroup.check(R.id.radioDateRecentTransaction1)
            SortType.DATE_ANCIEN -> radioGroup.check(R.id.radioDateAncienTransaction1)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("✅ Appliquer") { _, _ ->
                val selectedSortType = when (radioGroup.checkedRadioButtonId) {
                    R.id.radioTypeAZTransaction1 -> SortType.TYPE_A_Z
                    R.id.radioTypeZATransaction1 -> SortType.TYPE_Z_A
                    R.id.radioMontantCroissantTransaction1 -> SortType.MONTANT_CROISSANT
                    R.id.radioMontantDecroissantTransaction1 -> SortType.MONTANT_DECROISSANT
                    R.id.radioDateRecentTransaction1 -> SortType.DATE_RECENT
                    R.id.radioDateAncienTransaction1 -> SortType.DATE_ANCIEN
                    else -> SortType.DATE_RECENT
                }

                currentSortType = selectedSortType

                // Réappliquer le tri avec les données actuelles
                applySorting(currentTransactions)

                // Mettre à jour le texte du bouton pour indiquer le tri actuel
                updateFilterButtonText()
            }
            .setNegativeButton("❌ Annuler", null)
            .show()
    }

    // Mettre à jour le texte du bouton
    private fun updateFilterButtonText() {
        val filterText = when (currentSortType) {
            SortType.TYPE_A_Z -> "Type A→Z"
            SortType.TYPE_Z_A -> "Type Z→A"
            SortType.MONTANT_CROISSANT -> "Montant ↑"
            SortType.MONTANT_DECROISSANT -> "Montant ↓"
            SortType.DATE_RECENT -> "Date ↓"
            SortType.DATE_ANCIEN -> "Date ↑"
        }

        binding.btnFilterTransaction.text = filterText
    }

    // Ajout de la validation des champs
    private fun showAddTransactionDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)

        // Récupérer les références des champs et layouts
        val layoutType = dialogView.findViewById<TextInputLayout>(R.id.layout_transaction_type)
        val layoutAmount = dialogView.findViewById<TextInputLayout>(R.id.layout_transaction_amount)
        val layoutDate = dialogView.findViewById<TextInputLayout>(R.id.layout_transaction_date)
        val layoutBank = dialogView.findViewById<TextInputLayout>(R.id.layout_transaction_bank)

        // Suppression de "Transfert" des types de transaction
        val transactionTypes = arrayOf("Dépôt", "Retrait")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, transactionTypes)
        val typeDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.transactionTypeDropdown)
        typeDropdown.setAdapter(typeAdapter)

        val dateEditText = dialogView.findViewById<TextInputEditText>(R.id.transactionDate)
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateEditText.setText(dateFormat.format(calendar.time))

        dateEditText.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    dateEditText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Utilisation directe de la base de données
        lifecycleScope.launchWhenStarted {
            val banks = db.banqueDao().getAllBanques().first().map { it.nom }
            if (banks.isNotEmpty()) {
                val bankAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, banks)
                val bankDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.transactionBankDropdown)
                bankDropdown.setAdapter(bankAdapter)
                bankDropdown.setText(banks.first(), false)
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("💰 Ajouter une transaction")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { _, _ ->
                // La validation sera faite dans l'override du bouton
            }
            .setNegativeButton("Annuler", null)
            .create()

        dialog.show()

        // Override du bouton positif pour la validation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            // Récupérer les valeurs
            val type = typeDropdown.text.toString()
            val amountText = dialogView.findViewById<TextInputEditText>(R.id.transactionAmount).text.toString()
            val date = calendar.time
            val bankName = dialogView.findViewById<AutoCompleteTextView>(R.id.transactionBankDropdown).text.toString()

            // Effacer les erreurs précédentes
            clearErrors(layoutType, layoutAmount, layoutDate, layoutBank)

            // Valider les champs
            var isValid = true

            // 1. Validation du type
            if (type.isBlank()) {
                layoutType.error = "❌ Le type est obligatoire"
                isValid = false
            }

            // 2. Validation du montant
            if (amountText.isBlank()) {
                layoutAmount.error = "❌ Le montant est obligatoire"
                isValid = false
            } else {
                val amount = amountText.toDoubleOrNull()
                if (amount == null) {
                    layoutAmount.error = "❌ Montant invalide"
                    isValid = false
                } else if (amount <= 0) {
                    layoutAmount.error = "❌ Le montant doit être supérieur à 0"
                    isValid = false
                }
            }

            // 3. Validation de la date
            if (dateEditText.text.toString().isBlank()) {
                layoutDate.error = "❌ La date est obligatoire"
                isValid = false
            }

            // 4. Validation de la banque
            if (bankName.isBlank()) {
                layoutBank.error = "❌ La banque est obligatoire"
                isValid = false
            }

            // Si tous les champs sont valides, procéder à l'ajout
            if (isValid) {
                val amount = amountText.toDouble()

                lifecycleScope.launch {
                    try {
                        // Utilisation directe de la base de données comme dans MainActivity
                        val currentUser = db.utilisateurDao().getFirstUtilisateur()
                        currentUser?.let { user ->
                            val bankId = if (bankName.isNotBlank()) {
                                db.banqueDao().getBanqueByNom(bankName)?.id ?: 0
                            } else {
                                0
                            }

                            // Créer la transaction
                            val transaction = Transaction(
                                type = type,
                                montants = amount.toString(),
                                date = date,
                                id_utilisateur = user.id,
                                id_banque = bankId
                            )

                            // Insérer la transaction
                            db.transactionDao().insertTransaction(transaction)

                            // Mettre à jour le solde utilisateur
                            val newBalance = when (type) {
                                "Dépôt" -> user.solde + amount
                                "Retrait" -> user.solde - amount
                                else -> user.solde
                            }
                            db.utilisateurDao().update(user.copy(solde = newBalance))

                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "✅ Transaction ajoutée avec succès", Toast.LENGTH_SHORT).show()
                                // Réinitialiser l'affichage après ajout
                                showAllTransactions = false
                                dialog.dismiss()
                            }
                        } ?: run {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "❌ Utilisateur non trouvé", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "❌ Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    // Effacer les erreurs
    private fun clearErrors(vararg layouts: TextInputLayout) {
        layouts.forEach { it.error = null }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
