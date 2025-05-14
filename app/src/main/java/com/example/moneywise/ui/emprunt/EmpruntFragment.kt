package com.example.moneywise.ui.emprunt

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.moneywise.R
import com.example.moneywise.databinding.FragmentEmpruntBinding
import java.util.Calendar

class EmpruntFragment : Fragment() {

    private var _binding: FragmentEmpruntBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: EmpruntViewModel

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
        viewModel = ViewModelProvider(this).get(EmpruntViewModel::class.java)
        setupClickListeners()
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
                val emprunt = EmpruntViewModel.Emprunt(
                    nom = editNom.text.toString(),
                    contact = editContact.text.toString(),
                    montant = editMontant.text.toString(),
                    dateEmprunt = editDateEmprunt.text.toString(),
                    dateRemboursement = editDateRemboursement.text.toString()
                )
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