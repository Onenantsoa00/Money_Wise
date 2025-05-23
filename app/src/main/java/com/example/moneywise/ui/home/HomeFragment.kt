package com.example.moneywise.ui.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Projet
import com.example.moneywise.databinding.FragmentHomeBinding
import com.example.moneywise.ui.home.adapters.AcquittementHomeAdapter
import com.example.moneywise.ui.home.adapters.EmpruntHomeAdapter
import com.example.moneywise.ui.home.adapters.ProjetHomeAdapter
import com.example.moneywise.ui.transaction.TransactionHomeAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var empruntAdapter: EmpruntHomeAdapter
    private lateinit var acquittementAdapter: AcquittementHomeAdapter
    private lateinit var projetAdapter: ProjetHomeAdapter
    private lateinit var transactionAdapter: TransactionHomeAdapter
    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        viewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(database)
        ).get(HomeViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupAdapters()
        setupObservers()
        setupClickListeners()
        viewModel.refreshData()
    }

    private fun setupAdapters() {
        // Adapter pour les emprunts
        empruntAdapter = EmpruntHomeAdapter(emptyList())
        binding.recyclerEmprunts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = empruntAdapter
            setHasFixedSize(true)
        }

        // Adapter pour les acquittements
        acquittementAdapter = AcquittementHomeAdapter(emptyList())
        binding.recyclerAcquittements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = acquittementAdapter
            setHasFixedSize(true)
        }

        // Adapter pour les projets avec gestion des clics
        projetAdapter = ProjetHomeAdapter(emptyList()) { projet ->
            showInvestProjectDialog(projet)
        }
        binding.recyclerProjectsHome.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = projetAdapter
            setHasFixedSize(true)
        }

        // Adapter pour les transactions
        transactionAdapter = TransactionHomeAdapter(emptyList())
        binding.recyclerTransactionsHome.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
            setHasFixedSize(true)
        }

        // Masquer l'ancien HorizontalScrollView statique
        binding.horizontalScrollViewProjects.visibility = View.GONE
    }

    private fun setupObservers() {
        // Formatage de la devise
        val format = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 0
        }

        // Observateur pour le solde utilisateur
        viewModel.soldeUtilisateur.observe(viewLifecycleOwner) { solde ->
            solde?.let {
                binding.textSolde.text = "${format.format(it)} MGA"
            }
        }

        // Observateur pour le nom utilisateur
        viewModel.nomUtilisateur.observe(viewLifecycleOwner) { nom ->
            nom?.let {
                binding.textNomUtilisateurText.text = "Bonjour $it"
            }
        }

        // Observateur pour les emprunts récents
        viewModel.empruntsRecents.observe(viewLifecycleOwner) { emprunts ->
            emprunts?.let {
                empruntAdapter.updateList(it)
                binding.recyclerEmprunts.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        // Observateur pour les acquittements récents
        viewModel.acquittementsRecents.observe(viewLifecycleOwner) { acquittements ->
            acquittements?.let {
                acquittementAdapter.updateList(it)
                binding.recyclerAcquittements.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        // Observateur pour les projets récents
        viewModel.projetsRecents.observe(viewLifecycleOwner) { projets ->
            projets?.let {
                projetAdapter.updateList(it)
                binding.recyclerProjectsHome.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        // Observateur pour les transactions récentes
        viewModel.transactionsRecentes.observe(viewLifecycleOwner) { transactions ->
            transactions?.let {
                transactionAdapter.updateList(it)
                binding.recyclerTransactionsHome.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        // Bouton Add (Dépôt)
        binding.buttonAdd.setOnClickListener {
            showTransactionDialog("Dépôt")
        }

        // Bouton Send (Retrait)
        binding.buttonSend.setOnClickListener {
            showTransactionDialog("Retrait")
        }

        // Bouton Rembourser (Navigation vers Emprunt)
        binding.buttonRembourser.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_homeFragment_to_empruntFragment)
            } catch (e: Exception) {
                findNavController().navigate(R.id.nav_emprunt)
            }
        }

        // NOUVEAUX CLICK LISTENERS AJOUTÉS
        // Navigation vers TransactionFragment quand on clique sur "Voir tout" des transactions
        binding.voirToutTransactions.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_homeFragment_to_transactionFragment)
            } catch (e: Exception) {
                // Fallback si l'action n'existe pas
                findNavController().navigate(R.id.nav_transaction)
                Toast.makeText(requireContext(), "Navigation vers les transactions", Toast.LENGTH_SHORT).show()
            }
        }

        // Navigation vers ProjectFragment quand on clique sur "Voir tout" des projets
        binding.voirToutProjet.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_homeFragment_to_projectFragment)
            } catch (e: Exception) {
                // Fallback si l'action n'existe pas
                findNavController().navigate(R.id.nav_projet)
                Toast.makeText(requireContext(), "Navigation vers les projets", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTransactionDialog(fixedType: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)

        // Configurer le type de transaction fixe
        val typeDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.transactionTypeDropdown)
        typeDropdown.setText(fixedType)
        typeDropdown.isEnabled = false // Désactiver la modification

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

        // Configurer les banques
        val bankDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.transactionBankDropdown)

        viewModel.banks.observe(viewLifecycleOwner) { banks ->
            if (banks.isNotEmpty()) {
                val bankAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, banks)
                bankDropdown.setAdapter(bankAdapter)
                bankDropdown.setText(banks.first(), false)
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ajouter une transaction")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { dialog, _ ->
                val amountText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.transactionAmount).text.toString()
                val date = calendar.time
                val selectedBank = bankDropdown.text.toString()

                if (amountText.isBlank()) {
                    Toast.makeText(requireContext(), "Veuillez saisir un montant", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amount = amountText.toDoubleOrNull() ?: run {
                    Toast.makeText(requireContext(), "Montant invalide", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.addTransaction(
                    type = fixedType,
                    amount = amount,
                    date = date,
                    bankName = selectedBank,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Transaction ajoutée", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Toast.makeText(requireContext(), "Erreur: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // Nouvelle méthode pour afficher le dialogue d'investissement dans un projet
    private fun showInvestProjectDialog(projet: Projet) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_invest_project, null)

        // Récupérer les vues du dialogue
        val tvProjectTitle = dialogView.findViewById<TextView>(R.id.tvProjectTitle)
        val tvCurrentAmount = dialogView.findViewById<TextView>(R.id.tvCurrentAmount)
        val tvNeededAmount = dialogView.findViewById<TextView>(R.id.tvNeededAmount)
        val inputLayoutAmount = dialogView.findViewById<TextInputLayout>(R.id.inputLayoutAmount)
        val etInvestAmount = dialogView.findViewById<TextInputEditText>(R.id.etInvestAmount)
        val tvAvailableBalance = dialogView.findViewById<TextView>(R.id.tvAvailableBalance)

        // Configurer les informations du projet
        tvProjectTitle.text = "Investir dans ${projet.nom}"
        tvCurrentAmount.text = "${numberFormat.format(projet.montant_actuel)} MGA"
        tvNeededAmount.text = "${numberFormat.format(projet.montant_necessaire)} MGA"

        // Afficher le solde disponible
        viewModel.soldeUtilisateur.observe(viewLifecycleOwner) { solde ->
            solde?.let {
                tvAvailableBalance.text = "Solde disponible: ${numberFormat.format(it)} MGA"
            }
        }

        // Calculer le montant restant nécessaire
        val montantRestantNecessaire = projet.montant_necessaire - projet.montant_actuel

        // Ajouter un TextWatcher pour valider l'entrée en temps réel
        etInvestAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val montantText = s.toString()
                if (montantText.isEmpty()) {
                    inputLayoutAmount.error = null
                    return
                }

                val montant = montantText.toDoubleOrNull()
                if (montant == null) {
                    inputLayoutAmount.error = "Montant invalide"
                    return
                }

                // Vérifier si le montant est négatif
                if (montant <= 0) {
                    inputLayoutAmount.error = "Le montant doit être positif"
                    return
                }

                // Vérifier si le montant dépasse le solde disponible
                viewModel.soldeUtilisateur.value?.let { solde ->
                    if (montant > solde) {
                        inputLayoutAmount.error = "Montant supérieur au solde disponible"
                        return
                    }
                }

                // Vérifier si le montant dépasse ce qui est nécessaire
                if (montant > montantRestantNecessaire) {
                    inputLayoutAmount.error = "Montant supérieur à ce qui est nécessaire (${numberFormat.format(montantRestantNecessaire)} MGA)"
                    return
                }

                // Tout est valide
                inputLayoutAmount.error = null
            }
        })

        // Créer et afficher le dialogue
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Investissement")
            .setView(dialogView)
            .setPositiveButton("Investir", null) // On définit le comportement plus tard
            .setNegativeButton("Annuler", null)
            .create()

        dialog.show()

        // Configurer le bouton positif pour éviter qu'il ne ferme le dialogue en cas d'erreur
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val montantText = etInvestAmount.text.toString()

            if (montantText.isEmpty()) {
                inputLayoutAmount.error = "Veuillez saisir un montant"
                return@setOnClickListener
            }

            val montant = montantText.toDoubleOrNull()
            if (montant == null) {
                inputLayoutAmount.error = "Montant invalide"
                return@setOnClickListener
            }

            // Vérifier si le montant est négatif
            if (montant <= 0) {
                inputLayoutAmount.error = "Le montant doit être positif"
                return@setOnClickListener
            }

            // Vérifier si le montant dépasse le solde disponible
            viewModel.soldeUtilisateur.value?.let { solde ->
                if (montant > solde) {
                    inputLayoutAmount.error = "Montant supérieur au solde disponible"
                    return@setOnClickListener
                }
            }

            // Vérifier si le montant dépasse ce qui est nécessaire
            if (montant > montantRestantNecessaire) {
                inputLayoutAmount.error = "Montant supérieur à ce qui est nécessaire"
                return@setOnClickListener
            }

            // Tout est valide, procéder à l'investissement
            viewModel.investInProject(
                projetId = projet.id,
                montantInvestissement = montant,
                onSuccess = {
                    Toast.makeText(requireContext(), "Investissement réussi", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                },
                onError = { error ->
                    Toast.makeText(requireContext(), "Erreur: $error", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class HomeViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}