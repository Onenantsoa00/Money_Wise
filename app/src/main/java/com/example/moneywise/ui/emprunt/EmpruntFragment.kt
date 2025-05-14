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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Emprunt
import com.example.moneywise.databinding.FragmentEmpruntBinding
import java.text.SimpleDateFormat
import java.util.*

class EmpruntFragment : Fragment() {

    private var _binding: FragmentEmpruntBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: EmpruntViewModel
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private lateinit var adapter: EmpruntAdapter

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

        // Initialiser la base de données et le ViewModel
        val empruntDao = AppDatabase.getDatabase(requireContext()).empruntDao()
        viewModel = ViewModelProvider(this, EmpruntViewModelFactory(empruntDao)).get(EmpruntViewModel::class.java)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = EmpruntAdapter(emptyList())
        binding.recyclerEmprunts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@EmpruntFragment.adapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupObservers() {
        viewModel.allEmprunts.observe(viewLifecycleOwner) { emprunts ->
            adapter = EmpruntAdapter(emprunts)
            binding.recyclerEmprunts.adapter = adapter

            // Mettre à jour les résumés
            updateResumes(emprunts)
        }
    }

    private fun updateResumes(emprunts: List<Emprunt>) {
        val totalEmprunte = emprunts.sumOf { it.montant }
        val nombreEmprunteurs = emprunts.size
        val prochaineEcheance = emprunts.minByOrNull { it.date_remboursement }?.date_remboursement

        // Mettre à jour les vues avec ces valeurs
        // (vous devrez ajouter des IDs aux TextView dans votre layout)
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

                    // Validation des champs
                    if (nom.isBlank() || contact.isBlank() || montant <= 0) {
                        Toast.makeText(requireContext(), "Veuillez remplir tous les champs correctement", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    viewModel.insertEmprunt(nom, contact, montant, dateEmprunt, dateRemboursement)
                    Toast.makeText(requireContext(), "Emprunt enregistré avec succès", Toast.LENGTH_SHORT).show()
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