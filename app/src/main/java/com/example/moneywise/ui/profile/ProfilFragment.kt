package com.example.moneywise.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.moneywise.R
import com.example.moneywise.data.entity.Utilisateur
import com.example.moneywise.databinding.DialogEditProfileBinding
import com.example.moneywise.databinding.FragmentProfilBinding
import com.example.moneywise.utils.AuthHelper
import kotlinx.coroutines.launch

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfilViewModel by viewModels {
        ProfilViewModelFactory(requireActivity().application)
    }

    // Launcher pour sélectionner une image depuis la galerie
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateAvatar(it)
        }
    }

    // Launcher pour prendre une photo avec la caméra
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // L'image a été sauvegardée dans l'URI fournie
            // Vous pouvez traiter l'image ici si nécessaire
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
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

                        // Charger l'avatar
                        loadAvatar(it.avatar)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { error ->
                    error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.clearErrorMessage()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    // Vous pouvez afficher un indicateur de chargement ici
                    // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun loadAvatar(avatarPath: String?) {
        Glide.with(this)
            .load(avatarPath ?: R.drawable.icon_acount_circulaire)
            .transform(CircleCrop())
            .placeholder(R.drawable.icon_acount_circulaire)
            .error(R.drawable.icon_acount_circulaire)
            .into(binding.profileImage)
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

        // Ajouter un click listener pour changer l'avatar
        binding.profileImage.setOnClickListener {
            showAvatarSelectionDialog()
        }
    }

    private fun showAvatarSelectionDialog() {
        val options = arrayOf("Galerie", "Caméra", "Supprimer l'avatar")

        AlertDialog.Builder(requireContext())
            .setTitle("Changer l'avatar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> imagePickerLauncher.launch("image/*")
                    1 -> {
                        // Pour la caméra, vous devrez créer un fichier temporaire
                        // et utiliser cameraLauncher.launch(uri)
                        Toast.makeText(requireContext(), "Fonctionnalité caméra à implémenter", Toast.LENGTH_SHORT).show()
                    }
                    2 -> viewModel.updateAvatar(null)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
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
                    val result = viewModel.updateUser(updatedUser)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Profil mis à jour", Toast.LENGTH_SHORT).show()
                    }
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