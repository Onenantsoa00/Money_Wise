package com.example.moneywise.ui.historique

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.databinding.FragmentHistoriqueBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

class HistoriqueFragment : Fragment() {

    private var _binding: FragmentHistoriqueBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HistoriqueViewModel
    private lateinit var adapter: HistoriqueAdapter

    // 🔥 Enum pour les types de tri
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
        _binding = FragmentHistoriqueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        viewModel = ViewModelProvider(this, HistoriqueViewModelFactory(database))
            .get(HistoriqueViewModel::class.java)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        loadResumeData()
    }

    private fun setupRecyclerView() {
        binding.recyclerHistorique.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = HistoriqueAdapter(emptyList()).also {
                this@HistoriqueFragment.adapter = it
            }
        }
    }

    private fun setupObservers() {
        viewModel.allHistorique.observe(viewLifecycleOwner) { historiques ->
            applySorting(historiques)
        }
    }

    // 🔥 Configuration des listeners
    private fun setupClickListeners() {
        // Bouton pour remonter en haut
        binding.fabScrollTop.setOnClickListener {
            binding.nestedScrollView.smoothScrollTo(0, 0)
        }

        // 🔥 Bouton Filtrer
        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }
    }

    // 🔥 Afficher la modal de filtrage
    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_filter_historique, null)

        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupFilter)

        // Sélectionner le tri actuel
        when (currentSortType) {
            SortType.TYPE_A_Z -> radioGroup.check(R.id.radioTypeAZ)
            SortType.TYPE_Z_A -> radioGroup.check(R.id.radioTypeZA)
            SortType.MONTANT_CROISSANT -> radioGroup.check(R.id.radioMontantCroissant)
            SortType.MONTANT_DECROISSANT -> radioGroup.check(R.id.radioMontantDecroissant)
            SortType.DATE_RECENT -> radioGroup.check(R.id.radioDateRecent)
            SortType.DATE_ANCIEN -> radioGroup.check(R.id.radioDateAncien)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("✅ Appliquer") { _, _ ->
                val selectedSortType = when (radioGroup.checkedRadioButtonId) {
                    R.id.radioTypeAZ -> SortType.TYPE_A_Z
                    R.id.radioTypeZA -> SortType.TYPE_Z_A
                    R.id.radioMontantCroissant -> SortType.MONTANT_CROISSANT
                    R.id.radioMontantDecroissant -> SortType.MONTANT_DECROISSANT
                    R.id.radioDateRecent -> SortType.DATE_RECENT
                    R.id.radioDateAncien -> SortType.DATE_ANCIEN
                    else -> SortType.DATE_RECENT
                }

                currentSortType = selectedSortType

                // Réappliquer le tri avec les données actuelles
                viewModel.allHistorique.value?.let { historiques ->
                    applySorting(historiques)
                }

                // Mettre à jour le texte du bouton pour indiquer le tri actuel
                updateFilterButtonText()
            }
            .setNegativeButton("❌ Annuler", null)
            .show()
    }

    // 🔥 Appliquer le tri
    private fun applySorting(historiques: List<com.example.moneywise.data.entity.Historique>) {
        val sortedList = when (currentSortType) {
            SortType.TYPE_A_Z -> historiques.sortedBy { it.typeTransaction }
            SortType.TYPE_Z_A -> historiques.sortedByDescending { it.typeTransaction }
            SortType.MONTANT_CROISSANT -> historiques.sortedBy {
                it.montant.toString().replace(",", "")
                    .replace(" ", "")
                    .replace("MGA", "")
                    .trim()
                    .toDoubleOrNull() ?: 0.0
            }
            SortType.MONTANT_DECROISSANT -> historiques.sortedByDescending {
                it.montant.toString().replace(",", "")
                    .replace(" ", "")
                    .replace("MGA", "")
                    .trim()
                    .toDoubleOrNull() ?: 0.0
            }
            SortType.DATE_RECENT -> historiques.sortedByDescending { it.dateHeure }
            SortType.DATE_ANCIEN -> historiques.sortedBy { it.dateHeure }
        }

        adapter.updateList(sortedList)
    }

    // 🔥 Mettre à jour le texte du bouton
    private fun updateFilterButtonText() {
        val filterText = when (currentSortType) {
            SortType.TYPE_A_Z -> "Type A→Z"
            SortType.TYPE_Z_A -> "Type Z→A"
            SortType.MONTANT_CROISSANT -> "Montant ↑"
            SortType.MONTANT_DECROISSANT -> "Montant ↓"
            SortType.DATE_RECENT -> "Date ↓"
            SortType.DATE_ANCIEN -> "Date ↑"
        }

        binding.btnFilter.text = filterText
    }

    private fun loadResumeData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (credits, debits, solde) = viewModel.getResumeHistorique()
                withContext(Dispatchers.Main) {
                    val formatter = NumberFormat.getInstance(Locale.getDefault())
                    binding.textCredit.text = "+${formatter.format(credits)} MGA"
                    binding.textDebit.text = "-${formatter.format(debits)} MGA"
                    binding.textSolde.text = if (solde >= 0) "+${formatter.format(solde)} MGA" else "${formatter.format(solde)} MGA"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.textCredit.text = "+0 MGA"
                    binding.textDebit.text = "-0 MGA"
                    binding.textSolde.text = "0 MGA"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
