package com.example.moneywise.ui.acquittement

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
import com.example.moneywise.databinding.FragmentAcquittementBinding
import java.util.Calendar

class AcquittementFragment : Fragment() {

    private var _binding: FragmentAcquittementBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AcquittementViewModel

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

        viewModel = ViewModelProvider(this).get(AcquittementViewModel::class.java)
        setupUI()
        setupClickListeners()
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

        // Configurer les sélecteurs de date (exemple)
        editDateCredit.setOnClickListener { showDatePicker(editDateCredit) }
        editDateRemise.setOnClickListener { showDatePicker(editDateRemise) }

        AlertDialog.Builder(requireContext())
            .setTitle("Nouvel acquittement")
            .setView(dialogView)
            .setPositiveButton("Enregistrer") { dialog, _ ->
                val acquittement = AcquittementViewModel.Acquittement(
                    nom = editNom.text.toString(),
                    contact = editContact.text.toString(),
                    montant = editMontant.text.toString(),
                    dateCredit = editDateCredit.text.toString(),
                    dateRemise = editDateRemise.text.toString()
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