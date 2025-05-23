package com.example.moneywise.ui.project

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneywise.R
import com.example.moneywise.data.entity.Projet
import com.example.moneywise.databinding.FragmentProjetBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class ProjectFragment : Fragment() {

    private var _binding: FragmentProjetBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProjectViewModel
    private lateinit var projectAdapter: ProjectAdapter
    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(ProjectViewModel::class.java)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        projectAdapter = ProjectAdapter { projet ->
            viewModel.onProjectClicked(projet)
        }

        binding.recyclerProjects.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = projectAdapter
        }
    }

    private fun observeViewModel() {
        // Observer les statistiques
        viewModel.activeProjects.observe(viewLifecycleOwner) { count ->
            binding.tvActiveProjectsCount.text = count.toString()
        }

        viewModel.ongoingProjects.observe(viewLifecycleOwner) { count ->
            binding.tvOngoingProjectsCount.text = count.toString()
        }

        viewModel.completedProjects.observe(viewLifecycleOwner) { count ->
            binding.tvCompletedProjectsCount.text = count.toString()
        }

        // Observer la liste des projets filtrée
        viewModel.projects.observe(viewLifecycleOwner) { projects ->
            projectAdapter.submitList(projects)
        }

        // Observer le filtre actuel pour mettre à jour l'UI
        viewModel.currentFilter.observe(viewLifecycleOwner) { filter ->
            updateFilterUI(filter)
            updateProjectsTitle(filter)
        }

        // Observer l'événement d'affichage du dialogue d'investissement
        viewModel.showInvestDialog.observe(viewLifecycleOwner) { projet ->
            projet?.let {
                showInvestProjectDialog(it)
                viewModel.onInvestDialogComplete()
            }
        }
    }

    private fun updateFilterUI(filter: ProjectFilter) {
        // Réinitialiser tous les styles
        resetFilterStyles()

        // Appliquer le style actif au filtre sélectionné
        when (filter) {
            ProjectFilter.ACTIVE -> highlightFilter(
                binding.cardActiveProjects,
                binding.tvActiveProjectsLabel
            )
            ProjectFilter.ONGOING -> highlightFilter(
                binding.cardOngoingProjects,
                binding.tvOngoingProjectsLabel
            )
            ProjectFilter.COMPLETED -> highlightFilter(
                binding.cardCompletedProjects,
                binding.tvCompletedProjectsLabel
            )
            ProjectFilter.ALL -> {
                // Style spécial pour "Voir tout"
                binding.tvViewAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_color))
                binding.tvViewAll.setBackgroundResource(R.drawable.bg_filter_selected)
            }
        }
    }

    private fun resetFilterStyles() {
        // Réinitialiser les cartes de statistiques
        binding.cardActiveProjects.strokeColor = ContextCompat.getColor(requireContext(), android.R.color.transparent)
        binding.cardOngoingProjects.strokeColor = ContextCompat.getColor(requireContext(), android.R.color.transparent)
        binding.cardCompletedProjects.strokeColor = ContextCompat.getColor(requireContext(), android.R.color.transparent)

        // Réinitialiser les labels
        binding.tvActiveProjectsLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        binding.tvOngoingProjectsLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        binding.tvCompletedProjectsLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))

        // Réinitialiser "Voir tout"
        binding.tvViewAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_color))
        binding.tvViewAll.background = null
    }

    private fun highlightFilter(card: com.google.android.material.card.MaterialCardView, label: TextView) {
        card.strokeColor = ContextCompat.getColor(requireContext(), R.color.primary_color)
        label.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_color))
        label.textSize = 15f
    }

    private fun updateProjectsTitle(filter: ProjectFilter) {
        val title = when (filter) {
            ProjectFilter.ALL -> "Tous les Projets"
            ProjectFilter.ACTIVE -> "Projets Actifs"
            ProjectFilter.ONGOING -> "Projets en Cours"
            ProjectFilter.COMPLETED -> "Projets Complétés"
        }
        binding.tvProjectsTitle.text = title
    }

    private fun setupClickListeners() {
        // Bouton d'ajout de projet
        binding.fabAddProject.setOnClickListener {
            showAddProjectDialog()
        }

        // Filtres cliquables
        binding.layoutActiveProjects.setOnClickListener {
            viewModel.showActiveProjects()
        }

        binding.layoutOngoingProjects.setOnClickListener {
            viewModel.showOngoingProjects()
        }

        binding.layoutCompletedProjects.setOnClickListener {
            viewModel.showCompletedProjects()
        }

        binding.tvViewAll.setOnClickListener {
            viewModel.showAllProjects()
        }
    }

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
        viewModel.userBalance.observe(viewLifecycleOwner) { solde ->
            tvAvailableBalance.text = "Solde disponible: ${numberFormat.format(solde)} MGA"
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
                viewModel.userBalance.value?.let { solde ->
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
            .setPositiveButton("Investir", null)
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
            viewModel.userBalance.value?.let { solde ->
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

    private fun showAddProjectDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_projet, null)
        val editNom = dialogView.findViewById<EditText>(R.id.edit_nom)
        val editMontantNecessaire = dialogView.findViewById<EditText>(R.id.edit_montant_necessaire)
        val editMontantActuel = dialogView.findViewById<EditText>(R.id.edit_montant_actuel)
        val editProgression = dialogView.findViewById<EditText>(R.id.edit_progress)
        val editDateLimite = dialogView.findViewById<EditText>(R.id.edit_date_limite)

        // Ajouter des TextWatcher pour calculer automatiquement la progression
        val progressionTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    val montantNecessaire = editMontantNecessaire.text.toString().toDoubleOrNull() ?: 0.0
                    val montantActuel = editMontantActuel.text.toString().toDoubleOrNull() ?: 0.0

                    if (montantNecessaire > 0) {
                        val progression = viewModel.calculateProgression(montantActuel, montantNecessaire)
                        // Mettre à jour le champ de progression sans déclencher le TextWatcher
                        editProgression.removeTextChangedListener(this)
                        editProgression.setText(progression.toString())
                        editProgression.addTextChangedListener(this)
                    }
                } catch (e: Exception) {
                    // Ignorer les erreurs de conversion
                }
            }
        }

        // Ajouter les TextWatcher aux champs de montant
        editMontantNecessaire.addTextChangedListener(progressionTextWatcher)
        editMontantActuel.addTextChangedListener(progressionTextWatcher)

        // Rendre le champ de progression en lecture seule puisqu'il est calculé automatiquement
        editProgression.isEnabled = false

        editDateLimite.setOnClickListener { showDatePicker(editDateLimite) }

        AlertDialog.Builder(requireContext())
            .setTitle("Ajouter un Projet")
            .setView(dialogView)
            .setPositiveButton("Enregistrer") { dialog, _ ->
                try {
                    val nom = editNom.text.toString()
                    val montantNecessaire = editMontantNecessaire.text.toString().toDoubleOrNull() ?: 0.0
                    val montantActuel = editMontantActuel.text.toString().toDoubleOrNull() ?: 0.0
                    val dateLimiteStr = editDateLimite.text.toString()

                    // Validation des données
                    if (nom.isBlank()) {
                        Toast.makeText(requireContext(), "Le nom du projet est requis", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    if (montantNecessaire <= 0) {
                        Toast.makeText(requireContext(), "Le montant nécessaire doit être supérieur à 0", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val dateLimite = viewModel.parseDate(dateLimiteStr)
                    if (dateLimite == null) {
                        Toast.makeText(requireContext(), "Format de date invalide", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Insérer le projet dans la base de données
                    viewModel.insertProjet(
                        nom = nom,
                        montantNecessaire = montantNecessaire,
                        montantActuel = montantActuel,
                        dateLimite = dateLimite
                    )

                    Toast.makeText(requireContext(), "Projet ajouté avec succès", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            editText.setText("$day/${month + 1}/$year")
        },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}