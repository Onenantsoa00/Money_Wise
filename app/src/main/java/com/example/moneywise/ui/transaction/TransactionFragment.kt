package com.example.moneywise.ui.transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
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
                updateSummary(transactions)
                updateTransactionsList(transactions)
            }
        }
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
            if (transactions.size > 3 && !showAllTransactions) View.VISIBLE else View.GONE
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
            // Recharger les transactions
            lifecycleScope.launchWhenStarted {
                db.transactionDao().getAllTransaction().collect { transactions ->
                    transactionAdapter.submitList(transactions)
                }
            }
        }
    }

    private fun showAddTransactionDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)

        val transactionTypes = arrayOf("Dépôt", "Retrait", "Transfert")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, transactionTypes)
        val typeDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.transactionTypeDropdown)
        typeDropdown.setAdapter(typeAdapter)

        val dateEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.transactionDate)
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

        lifecycleScope.launchWhenStarted {
            val banks = transactionViewModel.getBanks()
            if (banks.isNotEmpty()) {
                val bankAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, banks)
                val bankDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.transactionBankDropdown)
                bankDropdown.setAdapter(bankAdapter)
                bankDropdown.setText(banks.first(), false)
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ajouter une transaction")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { dialog, _ ->
                val type = typeDropdown.text.toString()
                val amountText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.transactionAmount).text.toString()
                val date = calendar.time

                if (type.isBlank() || amountText.isBlank()) {
                    Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amount = amountText.toDoubleOrNull() ?: run {
                    Toast.makeText(requireContext(), "Montant invalide", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launchWhenStarted {
                    val currentUser = transactionViewModel.getCurrentUser()
                    currentUser?.let { user ->
                        val bankName = dialogView.findViewById<AutoCompleteTextView>(R.id.transactionBankDropdown).text.toString()
                        val bankId = if (bankName.isNotBlank()) {
                            db.banqueDao().getBanqueByNom(bankName)?.id ?: 0
                        } else {
                            0
                        }

                        transactionViewModel.addTransaction(
                            type = type,
                            amount = amount,
                            date = date,
                            userId = user.id,
                            bankId = if (bankId != 0) bankId else null,
                            onSuccess = {
                                Toast.makeText(requireContext(), "Transaction ajoutée", Toast.LENGTH_SHORT).show()
                                // Réinitialiser l'affichage après ajout
                                showAllTransactions = false
                            },
                            onError = { error ->
                                Toast.makeText(requireContext(), "Erreur: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } ?: run {
                        Toast.makeText(requireContext(), "Utilisateur non trouvé", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}