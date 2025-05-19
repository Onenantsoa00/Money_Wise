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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Acquittement
import com.example.moneywise.databinding.FragmentAcquittementBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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
            adapter.updateList(acquittements)
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

    private fun showAddAcquittementDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_acquittement, null)

        val editNom = dialogView.findViewById<EditText>(R.id.edit_nom)
        val editContact = dialogView.findViewById<EditText>(R.id.edit_contact)
        val editMontant = dialogView.findViewById<EditText>(R.id.edit_montant)
        val editDateCredit = dialogView.findViewById<EditText>(R.id.edit_date_credit)
        val editDateRemise = dialogView.findViewById<EditText>(R.id.edit_date_remise)

        editDateCredit.setOnClickListener { showDatePicker(editDateCredit) }
        editDateRemise.setOnClickListener { showDatePicker(editDateRemise) }

        AlertDialog.Builder(requireContext())
            .setTitle("Nouvel acquittement")
            .setView(dialogView)
            .setPositiveButton("Enregistrer") { dialog, _ ->
                try {
                    val nom = editNom.text.toString()
                    val contact = editContact.text.toString()
                    val montant = editMontant.text.toString().toDoubleOrNull() ?: 0.0
                    val dateCredit = editDateCredit.text.toString().let {
                        if (it.isNotBlank()) dateFormat.parse(it) else Date()
                    } ?: Date()
                    val dateRemise = editDateRemise.text.toString().let {
                        if (it.isNotBlank()) dateFormat.parse(it) else Date()
                    } ?: Date()

                    if (nom.isBlank() || contact.isBlank() || montant <= 0) {
                        Toast.makeText(requireContext(),
                            "Veuillez remplir tous les champs correctement",
                            Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Utilisation de Coroutines pour la gestion asynchrone
                    lifecycleScope.launch {
                        when (val result = viewModel.insertAcquittement(
                            nom, contact, montant, dateCredit, dateRemise
                        )) {
                            is AcquittementViewModel.AcquittementResult.Success -> {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Acquittement enregistré avec succès",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            is AcquittementViewModel.AcquittementResult.Error -> {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        requireContext(),
                                        result.message,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(),
                        "Erreur: ${e.message}",
                        Toast.LENGTH_SHORT).show()
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