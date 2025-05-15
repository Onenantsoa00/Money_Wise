package com.example.moneywise.ui.acquittement

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
import com.example.moneywise.data.entity.Acquittement
import com.example.moneywise.databinding.FragmentAcquittementBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AcquittementFragment : Fragment() {

    private var _binding: FragmentAcquittementBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AcquittementViewModel
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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

        val acquittementDao = AppDatabase.getDatabase(requireContext()).AcquittementDao()
        viewModel = ViewModelProvider(this, AcquittementViewModelFactory(acquittementDao))
            .get(AcquittementViewModel::class.java)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        binding.recyclerAcquittements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            // Adapter sera mis à jour dans setupObservers()
        }
    }

    private fun setupObservers() {
        viewModel.allAcquittements.observe(viewLifecycleOwner) { acquittements ->
            // Mettre à jour le RecyclerView
            val adapter = AcquittementAdapter(acquittements)
            binding.recyclerAcquittements.adapter = adapter

            // Mettre à jour les résumés
            updateResumes(acquittements)
        }
    }

    private fun updateResumes(acquittements: List<Acquittement>) {
        val totalRecu = acquittements.sumOf { it.montant }
        val nombrePersonnes = acquittements.size
        val dernierRemboursement = acquittements.maxByOrNull { it.date_remise_crédit }?.date_remise_crédit

        binding.textTotalRecu.text = "$totalRecu MGA"
        binding.textNombrePersonnes.text = nombrePersonnes.toString()
        binding.textDernierRemboursement.text = dernierRemboursement?.let { dateFormat.format(it) } ?: "N/A"
    }

    private fun setupClickListeners() {
        binding.fabAddAcquittement.setOnClickListener {
            showAddAcquittementDialog()
        }
    }

    private fun setupUI() {
        // Ici vous pouvez peupler votre UI avec les données du ViewModel
    }

    private fun showAddAcquittementDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_acquittement, null)

        val editNom = dialogView.findViewById<EditText>(R.id.edit_nom)
        val editContact = dialogView.findViewById<EditText>(R.id.edit_contact)
        val editMontant = dialogView.findViewById<EditText>(R.id.edit_montant)
        val editDateCredit = dialogView.findViewById<EditText>(R.id.edit_date_credit)
        val editDateRemise = dialogView.findViewById<EditText>(R.id.edit_date_remise)

        // Configurer les sélecteurs de date
        editDateCredit.setOnClickListener { showDatePicker(editDateCredit) }
        editDateRemise.setOnClickListener { showDatePicker(editDateRemise) }

        AlertDialog.Builder(requireContext())
            .setTitle("Nouvel acquittement")
            .setView(dialogView)
            .setPositiveButton("Enregistrer") { dialog, _ ->
                try {
                    val nom = editNom.text.toString()
                    val contact = editContact.text.toString()
                    val montant = editMontant.text.toString().toDouble()
                    val dateCredit = dateFormat.parse(editDateCredit.text.toString()) ?: Date()
                    val dateRemise = dateFormat.parse(editDateRemise.text.toString()) ?: Date()

                    // Validation des champs
                    if (nom.isBlank() || contact.isBlank() || montant <= 0) {
                        Toast.makeText(requireContext(), "Veuillez remplir tous les champs correctement", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    viewModel.insertAcquittement(nom, contact, montant, dateCredit, dateRemise)
                    Toast.makeText(requireContext(), "Acquittement enregistré avec succès", Toast.LENGTH_SHORT).show()
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