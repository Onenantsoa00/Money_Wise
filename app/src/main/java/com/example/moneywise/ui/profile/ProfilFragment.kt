package com.example.moneywise.ui.profile

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfilViewModel by viewModels {
        ProfilViewModelFactory(requireActivity().application)
    }

    private var currentPhotoUri: Uri? = null

    // Launcher pour les permissions multiples
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        }

        when {
            cameraGranted && storageGranted -> {
                // Toutes les permissions accordées, montrer le dialog
                showAvatarSelectionDialog()
            }
            cameraGranted -> {
                // Seulement la caméra accordée
                takePhoto()
            }
            storageGranted -> {
                // Seulement le stockage accordé
                imagePickerLauncher.launch("image/*")
            }
            else -> {
                Toast.makeText(requireContext(), "Permissions refusées", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher pour sélectionner une image depuis la galerie
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("ProfilFragment", "Image sélectionnée: $it")
            viewModel.updateAvatar(it)
            loadAvatar(it.toString())
        }
    }

    // Launcher pour prendre une photo avec la caméra
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        Log.d("ProfilFragment", "Photo prise avec succès: $success")
        if (success) {
            currentPhotoUri?.let { uri ->
                Log.d("ProfilFragment", "URI de la photo: $uri")
                viewModel.updateAvatar(uri)
                loadAvatar(uri.toString())
            }
        } else {
            Toast.makeText(requireContext(), "Échec de la prise de photo", Toast.LENGTH_SHORT).show()
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
        // Observer pour les données utilisateur
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

        // Observer pour les erreurs
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

        // Observer pour le chargement
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    // Vous pouvez afficher un indicateur de chargement ici si nécessaire
                    // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }

        // Observers pour les statistiques dynamiques
        viewModel.transactionsCount.observe(viewLifecycleOwner) { count ->
            binding.transactionsCountText.text = count?.toString() ?: "0"
        }

        viewModel.unfinishedProjectsCount.observe(viewLifecycleOwner) { count ->
            binding.projectsCountText.text = count?.toString() ?: "0"
        }

        viewModel.remindersCount.observe(viewLifecycleOwner) { count ->
            binding.remindersCountText.text = count?.toString() ?: "0"
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
            showLogoutConfirmationDialog()
        }

        // Click listener pour changer l'avatar
        binding.profileImage.setOnClickListener {
            checkPermissionsAndShowDialog()
        }
    }

    private fun checkPermissionsAndShowDialog() {
        val permissionsToRequest = mutableListOf<String>()

        // Vérifier la permission caméra
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        // Vérifier la permission de stockage selon la version Android
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), storagePermission)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(storagePermission)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            showAvatarSelectionDialog()
        }
    }

    private fun showAvatarSelectionDialog() {
        val options = arrayOf("Galerie", "Appareil photo", "Supprimer l'avatar")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Changer l'avatar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        Log.d("ProfilFragment", "Sélection galerie")
                        imagePickerLauncher.launch("image/*")
                    }
                    1 -> {
                        Log.d("ProfilFragment", "Sélection appareil photo")
                        takePhoto()
                    }
                    2 -> {
                        Log.d("ProfilFragment", "Suppression avatar")
                        viewModel.updateAvatar(null)
                        loadAvatar(null)
                    }
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun takePhoto() {
        try {
            // Créer un fichier pour stocker la photo
            val photoFile = File(
                requireContext().getExternalFilesDir("Pictures"),
                "profile_${System.currentTimeMillis()}.jpg"
            )

            // Créer le répertoire s'il n'existe pas
            photoFile.parentFile?.mkdirs()

            Log.d("ProfilFragment", "Chemin du fichier photo: ${photoFile.absolutePath}")

            // Créer l'URI avec FileProvider
            currentPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )

            Log.d("ProfilFragment", "URI de la photo: $currentPhotoUri")

            // Lancer l'appareil photo
            cameraLauncher.launch(currentPhotoUri)

        } catch (e: Exception) {
            Log.e("ProfilFragment", "Erreur lors de la création du fichier photo", e)
            Toast.makeText(
                requireContext(),
                "Erreur lors de l'ouverture de l'appareil photo: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showUserInfoDialog() {
        val user = viewModel.currentUser.value ?: return

        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedDate = dateFormatter.format(user.dateCreation)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Informations Personnelles")
            .setMessage(
                "Nom: ${user.nom}\n" +
                        "Prénom: ${user.prenom}\n" +
                        "Email: ${user.email}\n" +
                        "Solde: ${String.format("%.2f", user.solde)} MGA\n" +
                        "Date de création: $formattedDate"
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

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Modifier le profil")
            .setView(dialogBinding.root)
            .setPositiveButton("Enregistrer") { _, _ ->
                val nom = dialogBinding.etNom.text.toString().trim()
                val prenom = dialogBinding.etPrenom.text.toString().trim()
                val email = dialogBinding.etEmail.text.toString().trim()

                // Validation des champs
                if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Tous les champs sont obligatoires",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(
                        requireContext(),
                        "Format d'email invalide",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val updatedUser = user.copy(
                    nom = nom,
                    prenom = prenom,
                    email = email
                )

                lifecycleScope.launch {
                    val result = viewModel.updateUser(updatedUser)
                    if (result.isSuccess) {
                        Toast.makeText(
                            requireContext(),
                            "Profil mis à jour avec succès",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Erreur lors de la mise à jour",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Déconnexion")
            .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
            .setPositiveButton("Oui") { _, _ ->
                viewModel.logout()
                AuthHelper.logout(requireContext())
                Toast.makeText(requireContext(), "Déconnecté avec succès", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}