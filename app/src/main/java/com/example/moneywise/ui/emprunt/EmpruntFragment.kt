package com.example.moneywise.ui.emprunt

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class EmpruntFragment : Fragment() {

    private var _binding: FragmentEmpruntBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EmpruntViewModel by viewModels()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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
        binding.recyclerEmprunts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupObservers() {
        viewModel.empruntsNonRembourses.observe(viewLifecycleOwner) { emprunts ->
            binding.recyclerEmprunts.adapter = EmpruntAdapter(emprunts) { emprunt ->
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

            updateResumes(emprunts)
        }
    }

    private fun updateResumes(emprunts: List<Emprunt>) {
        val totalEmprunte = emprunts.sumOf { it.montant }
        val nombreEmprunteurs = emprunts.size
        val prochaineEcheance = emprunts.minByOrNull { it.date_remboursement }?.date_remboursement

        binding.textTotalEmprunte.text = "$totalEmprunte MGA"
        binding.textNombreEmprunteurs.text = nombreEmprunteurs.toString()
        binding.textProchaineEcheance.text = prochaineEcheance?.let { dateFormat.format(it) } ?: "N/A"
    }

    private fun setupClickListeners() {
        binding.fabAddEmprunt.setOnClickListener {
            showAddEmpruntDialog()
        }
    }

    private fun showAddEmpruntDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_emprunt, null)

        val editNom = dialogView.findViewById<EditText>(R.id.edit_nom)
        val editContact = dialogView.findViewById<EditText>(R.id.edit_contact)
        val editMontant = dialogView.findViewById<EditText>(R.id.edit_montant)
        val editDateEmprunt = dialogView.findViewById<EditText>(R.id.edit_date_emprunt)
        val editDateRemboursement = dialogView.findViewById<EditText>(R.id.edit_date_remboursement)

        // Configurer les sélecteurs de date
        editDateEmprunt.setOnClickListener { showDatePicker(editDateEmprunt) }
        editDateRemboursement.setOnClickListener { showDatePicker(editDateRemboursement) }

        AlertDialog.Builder(requireContext())
            .setTitle("Ajouter un Emprunt")
            .setView(dialogView)
            .setPositiveButton("Enregistrer") { dialog, _ ->
                try {
                    val nom = editNom.text.toString()
                    val contact = editContact.text.toString()
                    val montant = editMontant.text.toString().toDouble()
                    val dateEmprunt = dateFormat.parse(editDateEmprunt.text.toString()) ?: Date()
                    val dateRemboursement = dateFormat.parse(editDateRemboursement.text.toString()) ?: Date()

                    if (editNom.text.isBlank()) {
                        Toast.makeText(requireContext(), "Le nom est requis", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    viewModel.ajouterEmprunt(
                        nom = nom,
                        contact = contact,
                        montant = montant,
                        dateEmprunt = dateEmprunt,
                        dateRemboursement = dateRemboursement,
                        onSuccess = {
                            Toast.makeText(requireContext(), "Emprunt enregistré avec succès", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(requireContext(), "Erreur: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
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