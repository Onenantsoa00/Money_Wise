package com.example.moneywise.ui.project

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneywise.R
import com.example.moneywise.data.entity.Projet
import com.example.moneywise.databinding.FragmentProjetBinding
import java.util.Calendar

class ProjectFragment : Fragment() {

    private var _binding: FragmentProjetBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProjectViewModel
    private lateinit var projectAdapter: ProjectAdapter

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

        // Observer la liste des projets
        viewModel.projects.observe(viewLifecycleOwner) { projects ->
            projectAdapter.submitList(projects)
        }

        // Observer les événements de navigation
        viewModel.navigateToProjectDetail.observe(viewLifecycleOwner) { projectId ->
            projectId?.let {
                // Naviguer vers les détails du projet (à implémenter)
                // findNavController().navigate(...)
                viewModel.onProjectDetailComplete()
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddProject.setOnClickListener {
            showAddProjectDialog()
        }

        binding.tvViewAll.setOnClickListener {
            // Naviguer vers une vue complète des projets (à implémenter)
            // findNavController().navigate(...)
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
                    // La progression est calculée automatiquement dans le ViewModel
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