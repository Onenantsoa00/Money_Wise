package com.example.moneywise.ui.emprunt

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Emprunt
import com.example.moneywise.databinding.FragmentEmpruntBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class EmpruntFragment : Fragment() {

    private var _binding: FragmentEmpruntBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EmpruntViewModel by viewModels()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val numberFormat = NumberFormat.getInstance(Locale.getDefault())

    // 🔥 Variables pour le filtrage
    private lateinit var empruntAdapter: EmpruntAdapter
    private var currentEmprunts: List<Emprunt> = emptyList()

    // 🔥 Enum pour les types de tri
    enum class SortType {
        NOM_A_Z,
        NOM_Z_A,
        MONTANT_CROISSANT,
        MONTANT_DECROISSANT,
        DATE_REMBOURSEMENT_RECENT,
        DATE_REMBOURSEMENT_ANCIEN
    }

    private var currentSortType = SortType.DATE_REMBOURSEMENT_RECENT

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmpruntBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        empruntAdapter = EmpruntAdapter(emptyList()) { emprunt ->
            // Gérer le clic sur le bouton Rembourser
            AlertDialog.Builder(requireContext())
                .setTitle("Confirmer le remboursement")
                .setMessage("Voulez-vous vraiment marquer cet emprunt comme remboursé?")
                .setPositiveButton("Oui") { _, _ ->
                    viewModel.rembourserEmprunt(
                        emprunt,
                        onSuccess = {
                            Toast.makeText(
                                requireContext(),
                                "Emprunt remboursé avec succès",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onError = { error ->
                            Toast.makeText(
                                requireContext(),
                                "Erreur: $error",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
                .setNegativeButton("Non", null)
                .show()
        }

        binding.recyclerEmprunts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            adapter = empruntAdapter
        }
    }

    private fun setupObservers() {
        viewModel.empruntsNonRembourses.observe(viewLifecycleOwner) { emprunts ->
            currentEmprunts = emprunts
            applySorting(emprunts)
            updateResumes(emprunts)
        }
    }

    // 🔥 Configuration des listeners
    private fun setupClickListeners() {
        binding.fabAddEmprunt.setOnClickListener {
            showAddEmpruntDialog()
        }

        // 🔥 Bouton Filtrer
        binding.btnFilterEmprunt.setOnClickListener {
            showFilterDialog()
        }
    }

    // 🔥 Afficher la modal de filtrage
    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_filter_emprunt, null)

        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupFilterEmprunt)

        // Sélectionner le tri actuel
        when (currentSortType) {
            SortType.NOM_A_Z -> radioGroup.check(R.id.radioNomAZ)
            SortType.NOM_Z_A -> radioGroup.check(R.id.radioNomZA)
            SortType.MONTANT_CROISSANT -> radioGroup.check(R.id.radioMontantCroissantEmprunt)
            SortType.MONTANT_DECROISSANT -> radioGroup.check(R.id.radioMontantDecroissantEmprunt)
            SortType.DATE_REMBOURSEMENT_RECENT -> radioGroup.check(R.id.radioDateRemboursementRecent)
            SortType.DATE_REMBOURSEMENT_ANCIEN -> radioGroup.check(R.id.radioDateRemboursementAncien)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("✅ Appliquer") { _, _ ->
                val selectedSortType = when (radioGroup.checkedRadioButtonId) {
                    R.id.radioNomAZ -> SortType.NOM_A_Z
                    R.id.radioNomZA -> SortType.NOM_Z_A
                    R.id.radioMontantCroissantEmprunt -> SortType.MONTANT_CROISSANT
                    R.id.radioMontantDecroissantEmprunt -> SortType.MONTANT_DECROISSANT
                    R.id.radioDateRemboursementRecent -> SortType.DATE_REMBOURSEMENT_RECENT
                    R.id.radioDateRemboursementAncien -> SortType.DATE_REMBOURSEMENT_ANCIEN
                    else -> SortType.DATE_REMBOURSEMENT_RECENT
                }

                currentSortType = selectedSortType

                // Réappliquer le tri avec les données actuelles
                applySorting(currentEmprunts)

                // Mettre à jour le texte du bouton pour indiquer le tri actuel
                updateFilterButtonText()
            }
            .setNegativeButton("❌ Annuler", null)
            .show()
    }

    // 🔥 Appliquer le tri
    private fun applySorting(emprunts: List<Emprunt>) {
        val sortedList = when (currentSortType) {
            SortType.NOM_A_Z -> emprunts.sortedBy { it.nom_emprunte.lowercase() }
            SortType.NOM_Z_A -> emprunts.sortedByDescending { it.nom_emprunte.lowercase() }
            SortType.MONTANT_CROISSANT -> emprunts.sortedBy { it.montant }
            SortType.MONTANT_DECROISSANT -> emprunts.sortedByDescending { it.montant }
            SortType.DATE_REMBOURSEMENT_RECENT -> emprunts.sortedByDescending { it.date_remboursement }
            SortType.DATE_REMBOURSEMENT_ANCIEN -> emprunts.sortedBy { it.date_remboursement }
        }

        empruntAdapter.updateList(sortedList)
    }

    // 🔥 Mettre à jour le texte du bouton
    private fun updateFilterButtonText() {
        val filterText = when (currentSortType) {
            SortType.NOM_A_Z -> "Nom A→Z"
            SortType.NOM_Z_A -> "Nom Z→A"
            SortType.MONTANT_CROISSANT -> "Montant ↑"
            SortType.MONTANT_DECROISSANT -> "Montant ↓"
            SortType.DATE_REMBOURSEMENT_RECENT -> "Échéance ↓"
            SortType.DATE_REMBOURSEMENT_ANCIEN -> "Échéance ↑"
        }

        binding.btnFilterEmprunt.text = filterText
    }

    private fun updateResumes(emprunts: List<Emprunt>) {
        val totalEmprunte = emprunts.sumOf { it.montant }
        val nombreEmprunteurs = emprunts.size
        val prochaineEcheance = emprunts.minByOrNull { it.date_remboursement }?.date_remboursement

        binding.textTotalEmprunte.text = "${numberFormat.format(totalEmprunte)} MGA"
        binding.textNombreEmprunteurs.text = nombreEmprunteurs.toString()
        binding.textProchaineEcheance.text = prochaineEcheance?.let { dateFormat.format(it) } ?: "N/A"
    }

    // 🔥 NOUVELLE MÉTHODE: Dialogue d'ajout d'emprunt avec validation
    private fun showAddEmpruntDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_emprunt, null)

        // Récupérer les références des champs
        val layoutNom = dialogView.findViewById<TextInputLayout>(R.id.layout_nom)
        val layoutContact = dialogView.findViewById<TextInputLayout>(R.id.layout_contact)
        val layoutMontant = dialogView.findViewById<TextInputLayout>(R.id.layout_montant)
        val layoutDateEmprunt = dialogView.findViewById<TextInputLayout>(R.id.layout_date_emprunt)
        val layoutDateRemboursement = dialogView.findViewById<TextInputLayout>(R.id.layout_date_remboursement)

        val editNom = dialogView.findViewById<TextInputEditText>(R.id.edit_nom)
        val editContact = dialogView.findViewById<TextInputEditText>(R.id.edit_contact)
        val editMontant = dialogView.findViewById<TextInputEditText>(R.id.edit_montant)
        val editDateEmprunt = dialogView.findViewById<TextInputEditText>(R.id.edit_date_emprunt)
        val editDateRemboursement = dialogView.findViewById<TextInputEditText>(R.id.edit_date_remboursement)

        // Configurer les sélecteurs de date
        editDateEmprunt.setOnClickListener { showDatePicker(editDateEmprunt) }
        editDateRemboursement.setOnClickListener { showDatePicker(editDateRemboursement) }

        // Initialiser la date d'emprunt à aujourd'hui
        val today = Calendar.getInstance()
        editDateEmprunt.setText(dateFormat.format(today.time))

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("📝 Ajouter un Emprunt")
            .setView(dialogView)
            .setPositiveButton("💾 Enregistrer") { _, _ ->
                // La validation sera faite ici
            }
            .setNegativeButton("❌ Annuler", null)
            .create()

        dialog.show()

        // 🔥 Override du bouton positif pour la validation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (validateEmpruntForm(
                    layoutNom, layoutContact, layoutMontant,
                    layoutDateEmprunt, layoutDateRemboursement,
                    editNom, editContact, editMontant,
                    editDateEmprunt, editDateRemboursement
                )) {
                // Si la validation passe, traiter l'enregistrement
                processEmpruntSave(
                    editNom, editContact, editMontant,
                    editDateEmprunt, editDateRemboursement,
                    dialog
                )
            }
        }
    }

    // 🔥 NOUVELLE MÉTHODE: Validation complète du formulaire
    private fun validateEmpruntForm(
        layoutNom: TextInputLayout,
        layoutContact: TextInputLayout,
        layoutMontant: TextInputLayout,
        layoutDateEmprunt: TextInputLayout,
        layoutDateRemboursement: TextInputLayout,
        editNom: TextInputEditText,
        editContact: TextInputEditText,
        editMontant: TextInputEditText,
        editDateEmprunt: TextInputEditText,
        editDateRemboursement: TextInputEditText
    ): Boolean {
        var isValid = true

        // Effacer les erreurs précédentes
        clearErrors(layoutNom, layoutContact, layoutMontant, layoutDateEmprunt, layoutDateRemboursement)

        // 1. Validation du nom
        val nom = editNom.text.toString().trim()
        when {
            nom.isEmpty() -> {
                layoutNom.error = "❌ Le nom est obligatoire"
                isValid = false
            }
            !nom.first().isUpperCase() -> {
                layoutNom.error = "❌ Le nom doit commencer par une majuscule"
                isValid = false
            }
            nom.length < 2 -> {
                layoutNom.error = "❌ Le nom doit contenir au moins 2 caractères"
                isValid = false
            }
        }

        // 2. Validation du contact
        val contact = editContact.text.toString().trim()
        when {
            contact.isEmpty() -> {
                layoutContact.error = "❌ Le contact est obligatoire"
                isValid = false
            }
            contact.length < 8 -> {
                layoutContact.error = "❌ Le contact doit contenir au moins 8 chiffres"
                isValid = false
            }
            !contact.matches(Regex("^[+]?[0-9\\s-()]+$")) -> {
                layoutContact.error = "❌ Format de contact invalide"
                isValid = false
            }
        }

        // 3. Validation du montant
        val montantText = editMontant.text.toString().trim()
        when {
            montantText.isEmpty() -> {
                layoutMontant.error = "❌ Le montant est obligatoire"
                isValid = false
            }
            else -> {
                try {
                    val montant = montantText.toDouble()
                    when {
                        montant <= 0 -> {
                            layoutMontant.error = "❌ Le montant doit être supérieur à 0"
                            isValid = false
                        }
                        montant > 100_000_000 -> {
                            layoutMontant.error = "❌ Le montant est trop élevé"
                            isValid = false
                        }
                    }
                } catch (e: NumberFormatException) {
                    layoutMontant.error = "❌ Montant invalide"
                    isValid = false
                }
            }
        }

        // 4. Validation de la date d'emprunt
        val dateEmpruntText = editDateEmprunt.text.toString().trim()
        if (dateEmpruntText.isEmpty()) {
            layoutDateEmprunt.error = "❌ La date d'emprunt est obligatoire"
            isValid = false
        }

        // 5. Validation de la date de remboursement
        val dateRemboursementText = editDateRemboursement.text.toString().trim()
        when {
            dateRemboursementText.isEmpty() -> {
                layoutDateRemboursement.error = "❌ La date de remboursement est obligatoire"
                isValid = false
            }
            else -> {
                try {
                    val dateEmprunt = dateFormat.parse(dateEmpruntText)
                    val dateRemboursement = dateFormat.parse(dateRemboursementText)
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                    when {
                        dateRemboursement == null -> {
                            layoutDateRemboursement.error = "❌ Format de date invalide"
                            isValid = false
                        }
                        dateRemboursement.before(today) -> {
                            layoutDateRemboursement.error = "❌ La date ne peut pas être dans le passé"
                            isValid = false
                        }
                        dateEmprunt != null && dateRemboursement.before(dateEmprunt) -> {
                            layoutDateRemboursement.error = "❌ Doit être après la date d'emprunt"
                            isValid = false
                        }
                        dateEmprunt != null && dateRemboursement == dateEmprunt -> {
                            layoutDateRemboursement.error = "❌ Doit être différente de la date d'emprunt"
                            isValid = false
                        }
                    }
                } catch (e: Exception) {
                    layoutDateRemboursement.error = "❌ Format de date invalide"
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
    private fun processEmpruntSave(
        editNom: TextInputEditText,
        editContact: TextInputEditText,
        editMontant: TextInputEditText,
        editDateEmprunt: TextInputEditText,
        editDateRemboursement: TextInputEditText,
        dialog: AlertDialog
    ) {
        try {
            val nom = editNom.text.toString().trim()
            val contact = editContact.text.toString().trim()
            val montant = editMontant.text.toString().toDouble()
            val dateEmprunt = dateFormat.parse(editDateEmprunt.text.toString()) ?: Date()
            val dateRemboursement = dateFormat.parse(editDateRemboursement.text.toString()) ?: Date()

            viewModel.ajouterEmprunt(
                nom = nom,
                contact = contact,
                montant = montant,
                dateEmprunt = dateEmprunt,
                dateRemboursement = dateRemboursement,
                onSuccess = {
                    Toast.makeText(
                        requireContext(),
                        "✅ Emprunt enregistré avec succès",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                },
                onError = { error ->
                    Toast.makeText(
                        requireContext(),
                        "❌ Erreur: $error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "❌ Erreur: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
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
