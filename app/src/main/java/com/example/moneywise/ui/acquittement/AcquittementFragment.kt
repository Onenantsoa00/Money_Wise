package com.example.moneywise.ui.acquittement

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Acquittement
import com.example.moneywise.databinding.FragmentAcquittementBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AcquittementFragment : Fragment() {

    private var _binding: FragmentAcquittementBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AcquittementViewModel
    private lateinit var adapter: AcquittementAdapter
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val numberFormat = NumberFormat.getInstance(Locale.getDefault())

    // 🔥 Variables pour le filtrage
    private var currentAcquittements: List<Acquittement> = emptyList()

    // 🔥 Enum pour les types de tri
    enum class SortType {
        NOM_A_Z,
        NOM_Z_A,
        MONTANT_CROISSANT,
        MONTANT_DECROISSANT,
        DATE_REMISE_RECENT,
        DATE_REMISE_ANCIEN
    }

    private var currentSortType = SortType.DATE_REMISE_RECENT

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAcquittementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        val acquittementDao = database.AcquittementDao()

        viewModel = ViewModelProvider(
            this,
            AcquittementViewModelFactory(acquittementDao, database)
        ).get(AcquittementViewModel::class.java)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = AcquittementAdapter(emptyList()) { acquittement ->
            viewModel.rembourserAcquittement(acquittement)
        }

        binding.recyclerAcquittements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            adapter = this@AcquittementFragment.adapter
        }
    }

    private fun setupObservers() {
        viewModel.allAcquittements.observe(viewLifecycleOwner) { acquittements ->
            currentAcquittements = acquittements
            applySorting(acquittements)
            updateResumes(acquittements)
        }
    }

    // 🔥 Configuration des listeners
    private fun setupClickListeners() {
        binding.fabAddAcquittement.setOnClickListener {
            showAddAcquittementDialog()
        }

        // 🔥 Bouton Filtrer
        binding.btnFilterAcquittement.setOnClickListener {
            showFilterDialog()
        }
    }

    // 🔥 Afficher la modal de filtrage
    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_filter_acquittement, null)

        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupFilterAcquittement)

        // Sélectionner le tri actuel
        when (currentSortType) {
            SortType.NOM_A_Z -> radioGroup.check(R.id.radioNomAZAcquittement)
            SortType.NOM_Z_A -> radioGroup.check(R.id.radioNomZAAcquittement)
            SortType.MONTANT_CROISSANT -> radioGroup.check(R.id.radioMontantCroissantAcquittement)
            SortType.MONTANT_DECROISSANT -> radioGroup.check(R.id.radioMontantDecroissantAcquittement)
            SortType.DATE_REMISE_RECENT -> radioGroup.check(R.id.radioDateRemiseRecent)
            SortType.DATE_REMISE_ANCIEN -> radioGroup.check(R.id.radioDateRemiseAncien)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("✅ Appliquer") { _, _ ->
                val selectedSortType = when (radioGroup.checkedRadioButtonId) {
                    R.id.radioNomAZAcquittement -> SortType.NOM_A_Z
                    R.id.radioNomZAAcquittement -> SortType.NOM_Z_A
                    R.id.radioMontantCroissantAcquittement -> SortType.MONTANT_CROISSANT
                    R.id.radioMontantDecroissantAcquittement -> SortType.MONTANT_DECROISSANT
                    R.id.radioDateRemiseRecent -> SortType.DATE_REMISE_RECENT
                    R.id.radioDateRemiseAncien -> SortType.DATE_REMISE_ANCIEN
                    else -> SortType.DATE_REMISE_RECENT
                }

                currentSortType = selectedSortType

                // Réappliquer le tri avec les données actuelles
                applySorting(currentAcquittements)

                // Mettre à jour le texte du bouton pour indiquer le tri actuel
                updateFilterButtonText()
            }
            .setNegativeButton("❌ Annuler", null)
            .show()
    }

    // 🔥 Appliquer le tri
    private fun applySorting(acquittements: List<Acquittement>) {
        val sortedList = when (currentSortType) {
            SortType.NOM_A_Z -> acquittements.sortedBy { it.personne_acquittement.lowercase() }
            SortType.NOM_Z_A -> acquittements.sortedByDescending { it.personne_acquittement.lowercase() }
            SortType.MONTANT_CROISSANT -> acquittements.sortedBy { it.montant }
            SortType.MONTANT_DECROISSANT -> acquittements.sortedByDescending { it.montant }
            SortType.DATE_REMISE_RECENT -> acquittements.sortedByDescending { it.date_remise_crédit }
            SortType.DATE_REMISE_ANCIEN -> acquittements.sortedBy { it.date_remise_crédit }
        }

        adapter.updateList(sortedList)
    }

    // 🔥 Mettre à jour le texte du bouton
    private fun updateFilterButtonText() {
        val filterText = when (currentSortType) {
            SortType.NOM_A_Z -> "Nom A→Z"
            SortType.NOM_Z_A -> "Nom Z→A"
            SortType.MONTANT_CROISSANT -> "Montant ↑"
            SortType.MONTANT_DECROISSANT -> "Montant ↓"
            SortType.DATE_REMISE_RECENT -> "Remise ↓"
            SortType.DATE_REMISE_ANCIEN -> "Remise ↑"
        }

        binding.btnFilterAcquittement.text = filterText
    }

    private fun updateResumes(acquittements: List<Acquittement>) {
        val totalRecu = acquittements.sumOf { it.montant }
        val nombrePersonnes = acquittements.size
        val dernierRemboursement = acquittements.maxByOrNull { it.date_remise_crédit }?.date_remise_crédit

        binding.textTotalRecu.text = "${numberFormat.format(totalRecu)} MGA"
        binding.textNombrePersonnes.text = nombrePersonnes.toString()
        binding.textDernierRemboursement.text = dernierRemboursement?.let { dateFormat.format(it) } ?: "N/A"
    }

    // 🔥 NOUVELLE MÉTHODE: Dialogue d'ajout d'acquittement avec validation
    private fun showAddAcquittementDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_acquittement, null)

        // Récupérer les références des champs
        val layoutNom = dialogView.findViewById<TextInputLayout>(R.id.layout_nom)
        val layoutContact = dialogView.findViewById<TextInputLayout>(R.id.layout_contact)
        val layoutMontant = dialogView.findViewById<TextInputLayout>(R.id.layout_montant)
        val layoutDateCredit = dialogView.findViewById<TextInputLayout>(R.id.layout_date_credit)
        val layoutDateRemise = dialogView.findViewById<TextInputLayout>(R.id.layout_date_remise)

        val editNom = dialogView.findViewById<TextInputEditText>(R.id.edit_nom)
        val editContact = dialogView.findViewById<TextInputEditText>(R.id.edit_contact)
        val editMontant = dialogView.findViewById<TextInputEditText>(R.id.edit_montant)
        val editDateCredit = dialogView.findViewById<TextInputEditText>(R.id.edit_date_credit)
        val editDateRemise = dialogView.findViewById<TextInputEditText>(R.id.edit_date_remise)
        val textSoldeDisponible = dialogView.findViewById<TextView>(R.id.text_solde_disponible)

        // Configurer les sélecteurs de date
        editDateCredit.setOnClickListener { showDatePicker(editDateCredit) }
        editDateRemise.setOnClickListener { showDatePicker(editDateRemise) }

        // Initialiser la date de crédit à aujourd'hui
        val today = Calendar.getInstance()
        editDateCredit.setText(dateFormat.format(today.time))

        // 🔥 Charger et afficher le solde utilisateur
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(requireContext())
            val currentUser = database.utilisateurDao().getFirstUtilisateur()
            val soldeDisponible = currentUser?.solde ?: 0.0

            withContext(Dispatchers.Main) {
                textSoldeDisponible.text = "💰 Solde disponible : ${numberFormat.format(soldeDisponible)} MGA"
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("💳 Nouvel Acquittement")
            .setView(dialogView)
            .setPositiveButton("💾 Enregistrer") { _, _ ->
                // La validation sera faite ici
            }
            .setNegativeButton("❌ Annuler", null)
            .create()

        dialog.show()

        // 🔥 Override du bouton positif pour la validation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            lifecycleScope.launch {
                if (validateAcquittementForm(
                        layoutNom, layoutContact, layoutMontant,
                        layoutDateCredit, layoutDateRemise,
                        editNom, editContact, editMontant,
                        editDateCredit, editDateRemise
                    )) {
                    // Si la validation passe, traiter l'enregistrement
                    processAcquittementSave(
                        editNom, editContact, editMontant,
                        editDateCredit, editDateRemise,
                        dialog
                    )
                }
            }
        }
    }

    // 🔥 NOUVELLE MÉTHODE: Validation complète du formulaire
    private suspend fun validateAcquittementForm(
        layoutNom: TextInputLayout,
        layoutContact: TextInputLayout,
        layoutMontant: TextInputLayout,
        layoutDateCredit: TextInputLayout,
        layoutDateRemise: TextInputLayout,
        editNom: TextInputEditText,
        editContact: TextInputEditText,
        editMontant: TextInputEditText,
        editDateCredit: TextInputEditText,
        editDateRemise: TextInputEditText
    ): Boolean {
        var isValid = true

        // Effacer les erreurs précédentes
        withContext(Dispatchers.Main) {
            clearErrors(layoutNom, layoutContact, layoutMontant, layoutDateCredit, layoutDateRemise)
        }

        // 1. Validation du nom
        val nom = editNom.text.toString().trim()
        when {
            nom.isEmpty() -> {
                withContext(Dispatchers.Main) {
                    layoutNom.error = "❌ Le nom est obligatoire"
                }
                isValid = false
            }
            !nom.first().isUpperCase() -> {
                withContext(Dispatchers.Main) {
                    layoutNom.error = "❌ Le nom doit commencer par une majuscule"
                }
                isValid = false
            }
            nom.length < 2 -> {
                withContext(Dispatchers.Main) {
                    layoutNom.error = "❌ Le nom doit contenir au moins 2 caractères"
                }
                isValid = false
            }
        }

        // 2. Validation du contact
        val contact = editContact.text.toString().trim()
        when {
            contact.isEmpty() -> {
                withContext(Dispatchers.Main) {
                    layoutContact.error = "❌ Le contact est obligatoire"
                }
                isValid = false
            }
            contact.length < 8 -> {
                withContext(Dispatchers.Main) {
                    layoutContact.error = "❌ Le contact doit contenir au moins 8 chiffres"
                }
                isValid = false
            }
            !contact.matches(Regex("^[+]?[0-9\\s-()]+$")) -> {
                withContext(Dispatchers.Main) {
                    layoutContact.error = "❌ Format de contact invalide"
                }
                isValid = false
            }
        }

        // 3. Validation du montant avec vérification du solde
        val montantText = editMontant.text.toString().trim()
        when {
            montantText.isEmpty() -> {
                withContext(Dispatchers.Main) {
                    layoutMontant.error = "❌ Le montant est obligatoire"
                }
                isValid = false
            }
            else -> {
                try {
                    val montant = montantText.toDouble()
                    when {
                        montant <= 0 -> {
                            withContext(Dispatchers.Main) {
                                layoutMontant.error = "❌ Le montant doit être supérieur à 0"
                            }
                            isValid = false
                        }
                        montant > 100_000_000 -> {
                            withContext(Dispatchers.Main) {
                                layoutMontant.error = "❌ Le montant est trop élevé"
                            }
                            isValid = false
                        }
                        else -> {
                            // 🔥 Vérification du solde utilisateur
                            val database = AppDatabase.getDatabase(requireContext())
                            val currentUser = database.utilisateurDao().getFirstUtilisateur()
                            val soldeDisponible = currentUser?.solde ?: 0.0

                            if (montant > soldeDisponible) {
                                withContext(Dispatchers.Main) {
                                    layoutMontant.error = "❌ Montant supérieur au solde disponible (${numberFormat.format(soldeDisponible)} MGA)"
                                }
                                isValid = false
                            }
                        }
                    }
                } catch (e: NumberFormatException) {
                    withContext(Dispatchers.Main) {
                        layoutMontant.error = "❌ Montant invalide"
                    }
                    isValid = false
                }
            }
        }

        // 4. Validation de la date de crédit
        val dateCreditText = editDateCredit.text.toString().trim()
        if (dateCreditText.isEmpty()) {
            withContext(Dispatchers.Main) {
                layoutDateCredit.error = "❌ La date de crédit est obligatoire"
            }
            isValid = false
        }

        // 5. Validation de la date de remise
        val dateRemiseText = editDateRemise.text.toString().trim()
        when {
            dateRemiseText.isEmpty() -> {
                withContext(Dispatchers.Main) {
                    layoutDateRemise.error = "❌ La date de remise est obligatoire"
                }
                isValid = false
            }
            else -> {
                try {
                    val dateCredit = dateFormat.parse(dateCreditText)
                    val dateRemise = dateFormat.parse(dateRemiseText)
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                    when {
                        dateRemise == null -> {
                            withContext(Dispatchers.Main) {
                                layoutDateRemise.error = "❌ Format de date invalide"
                            }
                            isValid = false
                        }
                        dateRemise.before(today) -> {
                            withContext(Dispatchers.Main) {
                                layoutDateRemise.error = "❌ La date ne peut pas être dans le passé"
                            }
                            isValid = false
                        }
                        dateCredit != null && dateRemise.before(dateCredit) -> {
                            withContext(Dispatchers.Main) {
                                layoutDateRemise.error = "❌ Doit être après la date de crédit"
                            }
                            isValid = false
                        }
                        dateCredit != null && dateRemise == dateCredit -> {
                            withContext(Dispatchers.Main) {
                                layoutDateRemise.error = "❌ Doit être différente de la date de crédit"
                            }
                            isValid = false
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        layoutDateRemise.error = "❌ Format de date invalide"
                    }
                    isValid = false
                }
            }
        }

        return isValid
    }

    // 🔥 NOUVELLE MÉTHODE: Effacer les erreurs
    private fun clearErrors(vararg layouts: TextInputLayout) {
        layouts.forEach { it.error = null }
    }

    // 🔥 NOUVELLE MÉTHODE: Traiter l'enregistrement
    private fun processAcquittementSave(
        editNom: TextInputEditText,
        editContact: TextInputEditText,
        editMontant: TextInputEditText,
        editDateCredit: TextInputEditText,
        editDateRemise: TextInputEditText,
        dialog: AlertDialog
    ) {
        lifecycleScope.launch {
            try {
                val nom = editNom.text.toString().trim()
                val contact = editContact.text.toString().trim()
                val montant = editMontant.text.toString().toDouble()
                val dateCredit = dateFormat.parse(editDateCredit.text.toString()) ?: Date()
                val dateRemise = dateFormat.parse(editDateRemise.text.toString()) ?: Date()

                when (val result = viewModel.insertAcquittement(
                    nom, contact, montant, dateCredit, dateRemise
                )) {
                    is AcquittementViewModel.AcquittementResult.Success -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "✅ Acquittement enregistré avec succès",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                        }
                    }
                    is AcquittementViewModel.AcquittementResult.Error -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "❌ ${result.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "❌ Erreur: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showDatePicker(editText: TextInputEditText) {
        val calendar = Calendar.getInstance()

        // Si le champ contient déjà une date, l'utiliser comme date initiale
        try {
            val currentDate = dateFormat.parse(editText.text.toString())
            if (currentDate != null) {
                calendar.time = currentDate
            }
        } catch (e: Exception) {
            // Utiliser la date actuelle si parsing échoue
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, day)
                }
                editText.setText(dateFormat.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
