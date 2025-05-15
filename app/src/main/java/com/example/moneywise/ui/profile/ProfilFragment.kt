package com.example.moneywise.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.moneywise.R
import com.example.moneywise.data.entity.Utilisateur
import com.example.moneywise.databinding.DialogEditProfileBinding
import com.example.moneywise.databinding.FragmentProfilBinding
import com.example.moneywise.utils.AuthHelper
import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collect

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfilViewModel by viewModels {
        ProfilViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.logoutButton.setOnClickListener {
            AuthHelper.logout(requireContext())
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentUser.collect { user ->
                    user?.let {
                        binding.profileName.text = "${it.nom} ${it.prenom}"
                        binding.profileEmail.text = it.email
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.personalInfoButton.setOnClickListener {
            showUserInfoDialog()
        }

        binding.editInfoButton.setOnClickListener {
            showEditProfileDialog()
        }

        binding.logoutButton.setOnClickListener {
            viewModel.logout()
            AuthHelper.logout(requireContext())
        }
    }

    private fun showUserInfoDialog() {
        val user = viewModel.currentUser.value ?: return

        AlertDialog.Builder(requireContext())
            .setTitle("Informations Personnelles")
            .setMessage(
                "Nom: ${user.nom}\n" +
                        "Prénom: ${user.prenom}\n" +
                        "Email: ${user.email}\n" +
                        "Solde: ${user.solde} MGA"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showEditProfileDialog() {
        val user = viewModel.currentUser.value ?: return
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)

        // Pré-remplir les champs
        dialogBinding.etNom.setText(user.nom)
        dialogBinding.etPrenom.setText(user.prenom)
        dialogBinding.etEmail.setText(user.email)

        AlertDialog.Builder(requireContext())
            .setTitle("Modifier le profil")
            .setView(dialogBinding.root)
            .setPositiveButton("Enregistrer") { _, _ ->
                val updatedUser = user.copy(
                    nom = dialogBinding.etNom.text.toString(),
                    prenom = dialogBinding.etPrenom.text.toString(),
                    email = dialogBinding.etEmail.text.toString()
                )

                lifecycleScope.launch {
                    viewModel.updateUser(updatedUser)
                    Toast.makeText(context, "Profil mis à jour", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}