package com.example.moneywise.ui.project

import android.app.DatePickerDialog
import android.app.ProgressDialog.show
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.moneywise.R
import com.example.moneywise.databinding.FragmentProjetBinding
import java.util.Calendar

class ProjectFragment : Fragment() {

    private var _binding: FragmentProjetBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProjectViewModel

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
        setupClickListeners()
    }


    private fun setupClickListeners() {
        binding.fabAddProject.setOnClickListener {
            showAddProjectDialog()
        }
    }

    private fun showAddProjectDialog(){
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_projet, null)
        val editNom = dialogView.findViewById<EditText>(R.id.edit_nom)
        val editMontantNecessaire = dialogView.findViewById<EditText>(R.id.edit_montant_necessaire)
        val editMontantActuel = dialogView.findViewById<EditText>(R.id.edit_montant_actuel)
        val editProgression = dialogView.findViewById<EditText>(R.id.edit_progress)
        val editDateLimite = dialogView.findViewById<EditText>(R.id.edit_date_limite)

        editDateLimite.setOnClickListener{showDatePicker(editDateLimite)}

        AlertDialog.Builder(requireContext())
            .setTitle("Ajouter un Projet")
            .setView(dialogView)
            .setPositiveButton("Enregistrer") { dialog, _ ->
                val projet = ProjectViewModel.Project(

                    nom = editNom.text.toString(),
                    montantNecessaire = editMontantNecessaire.text.toString(),
                    montantActuel = editMontantActuel.text.toString(),
                    progression = editProgression.text.toString().toIntOrNull()?:0,
                    dateLimite = editDateLimite.text.toString()
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