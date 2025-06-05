package com.example.moneywise.ui.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Projet
import com.example.moneywise.databinding.FragmentHomeBinding
import com.example.moneywise.ui.home.adapters.AcquittementHomeAdapter
import com.example.moneywise.ui.home.adapters.EmpruntHomeAdapter
import com.example.moneywise.ui.home.adapters.ProjetHomeAdapter
import com.example.moneywise.ui.transaction.TransactionHomeAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var empruntAdapter: EmpruntHomeAdapter
    private lateinit var acquittementAdapter: AcquittementHomeAdapter
    private lateinit var projetAdapter: ProjetHomeAdapter
    private lateinit var transactionAdapter: TransactionHomeAdapter
    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0
    }

    // Variables pour l'auto-scroll des transactions (vertical)
    private var autoScrollHandler: Handler? = null
    private var autoScrollRunnable: Runnable? = null
    private var isScrollingDown = true
    private var currentScrollPosition = 0
    private val SCROLL_DELAY = 2000L // 2 secondes entre chaque scroll
    private val SCROLL_DURATION = 1500 // Durée de l'animation de scroll

    // Variables pour l'auto-scroll des projets (horizontal)
    private var autoScrollProjectsHandler: Handler? = null
    private var autoScrollProjectsRunnable: Runnable? = null
    private var isScrollingRight = true
    private var currentProjectScrollPosition = 0
    private val PROJECT_SCROLL_DELAY = 2500L // 2.5 secondes entre chaque scroll
    private val PROJECT_SCROLL_DURATION = 1800 // Durée de l'animation de scroll

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        viewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(database)
        ).get(HomeViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupAdapters()
        setupObservers()
        setupClickListeners()
        viewModel.refreshData()
    }

    private fun setupAdapters() {
        // Adapter pour les emprunts
        empruntAdapter = EmpruntHomeAdapter(emptyList())
        binding.recyclerEmprunts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = empruntAdapter
            setHasFixedSize(true)
        }

        // Adapter pour les acquittements
        acquittementAdapter = AcquittementHomeAdapter(emptyList())
        binding.recyclerAcquittements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = acquittementAdapter
            setHasFixedSize(true)
        }

        // Adapter pour les projets avec gestion des clics et auto-scroll horizontal
        projetAdapter = ProjetHomeAdapter(emptyList()) { projet ->
            showInvestProjectDialog(projet)
        }
        binding.recyclerProjectsHome.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = projetAdapter
            setHasFixedSize(true)

            // Désactiver le scroll manuel pour éviter les conflits
            isNestedScrollingEnabled = false

            // Ajouter un listener pour détecter les changements de scroll horizontal
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    currentProjectScrollPosition = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                }
            })
        }

        // Adapter pour les transactions avec auto-scroll vertical
        transactionAdapter = TransactionHomeAdapter(emptyList())
        binding.recyclerTransactionsHome.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
            setHasFixedSize(true)

            // Désactiver le scroll manuel pour éviter les conflits
            isNestedScrollingEnabled = false

            // Ajouter un listener pour détecter les changements de scroll
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    currentScrollPosition = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                }
            })
        }

        // Masquer l'ancien HorizontalScrollView statique
        binding.horizontalScrollViewProjects.visibility = View.GONE
    }

    private fun setupObservers() {
        // Formatage de la devise
        val format = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 0
        }

        // Observateur pour le solde utilisateur
        viewModel.soldeUtilisateur.observe(viewLifecycleOwner) { solde ->
            solde?.let {
                binding.textSolde.text = "${format.format(it)} MGA"
            }
        }

        // Observateur pour le nom utilisateur
        viewModel.nomUtilisateur.observe(viewLifecycleOwner) { nom ->
            nom?.let {
                binding.textNomUtilisateurText.text = "Bonjour $it"
            }
        }

        // Observateur pour les emprunts récents
        viewModel.empruntsRecents.observe(viewLifecycleOwner) { emprunts ->
            emprunts?.let {
                empruntAdapter.updateList(it)
                binding.recyclerEmprunts.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        // Observateur pour les acquittements récents
        viewModel.acquittementsRecents.observe(viewLifecycleOwner) { acquittements ->
            acquittements?.let {
                acquittementAdapter.updateList(it)
                binding.recyclerAcquittements.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        // Observateur pour les projets récents avec démarrage de l'auto-scroll horizontal
        viewModel.projetsRecents.observe(viewLifecycleOwner) { projets ->
            projets?.let {
                projetAdapter.updateList(it)
                binding.recyclerProjectsHome.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE

                // Démarrer l'auto-scroll horizontal seulement s'il y a des projets
                if (it.isNotEmpty() && it.size > 1) {
                    startAutoScrollProjects()
                } else {
                    stopAutoScrollProjects()
                }
            }
        }

        // Observateur pour les transactions récentes avec démarrage de l'auto-scroll vertical
        viewModel.transactionsRecentes.observe(viewLifecycleOwner) { transactions ->
            transactions?.let {
                transactionAdapter.updateList(it)
                binding.recyclerTransactionsHome.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE

                // Démarrer l'auto-scroll seulement s'il y a des transactions
                if (it.isNotEmpty() && it.size > 1) {
                    startAutoScroll()
                } else {
                    stopAutoScroll()
                }
            }
        }
    }

    // Auto-scroll vertical pour les transactions
    private fun startAutoScroll() {
        stopAutoScroll() // Arrêter l'ancien scroll s'il existe

        autoScrollHandler = Handler(Looper.getMainLooper())
        autoScrollRunnable = object : Runnable {
            override fun run() {
                val recyclerView = binding.recyclerTransactionsHome
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                val adapter = recyclerView.adapter

                if (layoutManager != null && adapter != null && adapter.itemCount > 1) {
                    val totalItems = adapter.itemCount
                    val lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()
                    val firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition()

                    // Créer un smooth scroller personnalisé avec vitesse réduite
                    val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                        override fun getVerticalSnapPreference(): Int {
                            return SNAP_TO_START
                        }

                        override fun calculateTimeForScrolling(dx: Int): Int {
                            return SCROLL_DURATION
                        }
                    }

                    when {
                        // Si on scroll vers le bas et qu'on atteint la fin
                        isScrollingDown && lastVisiblePosition >= totalItems - 1 -> {
                            isScrollingDown = false
                            smoothScroller.targetPosition = 0
                            layoutManager.startSmoothScroll(smoothScroller)
                        }
                        // Si on scroll vers le haut et qu'on atteint le début
                        !isScrollingDown && firstVisiblePosition <= 0 -> {
                            isScrollingDown = true
                            smoothScroller.targetPosition = totalItems - 1
                            layoutManager.startSmoothScroll(smoothScroller)
                        }
                        // Continuer dans la direction actuelle
                        isScrollingDown -> {
                            val nextPosition = minOf(lastVisiblePosition + 1, totalItems - 1)
                            smoothScroller.targetPosition = nextPosition
                            layoutManager.startSmoothScroll(smoothScroller)
                        }
                        else -> {
                            val nextPosition = maxOf(firstVisiblePosition - 1, 0)
                            smoothScroller.targetPosition = nextPosition
                            layoutManager.startSmoothScroll(smoothScroller)
                        }
                    }
                }

                // Programmer le prochain scroll
                autoScrollHandler?.postDelayed(this, SCROLL_DELAY)
            }
        }

        // Démarrer le premier scroll après un délai initial
        autoScrollHandler?.postDelayed(autoScrollRunnable!!, SCROLL_DELAY)
    }

    private fun stopAutoScroll() {
        autoScrollRunnable?.let { runnable ->
            autoScrollHandler?.removeCallbacks(runnable)
        }
        autoScrollHandler = null
        autoScrollRunnable = null
    }

    // Auto-scroll horizontal pour les projets
    private fun startAutoScrollProjects() {
        stopAutoScrollProjects() // Arrêter l'ancien scroll s'il existe

        autoScrollProjectsHandler = Handler(Looper.getMainLooper())
        autoScrollProjectsRunnable = object : Runnable {
            override fun run() {
                val recyclerView = binding.recyclerProjectsHome
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                val adapter = recyclerView.adapter

                if (layoutManager != null && adapter != null && adapter.itemCount > 1) {
                    val totalItems = adapter.itemCount
                    val lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()
                    val firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition()

                    // Créer un smooth scroller personnalisé pour le scroll horizontal
                    val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                        override fun getHorizontalSnapPreference(): Int {
                            return SNAP_TO_START
                        }

                        override fun calculateTimeForScrolling(dx: Int): Int {
                            return PROJECT_SCROLL_DURATION
                        }
                    }

                    when {
                        // Si on scroll vers la droite et qu'on atteint la fin
                        isScrollingRight && lastVisiblePosition >= totalItems - 1 -> {
                            isScrollingRight = false
                            smoothScroller.targetPosition = 0
                            layoutManager.startSmoothScroll(smoothScroller)
                        }
                        // Si on scroll vers la gauche et qu'on atteint le début
                        !isScrollingRight && firstVisiblePosition <= 0 -> {
                            isScrollingRight = true
                            smoothScroller.targetPosition = totalItems - 1
                            layoutManager.startSmoothScroll(smoothScroller)
                        }
                        // Continuer dans la direction actuelle
                        isScrollingRight -> {
                            val nextPosition = minOf(lastVisiblePosition + 1, totalItems - 1)
                            smoothScroller.targetPosition = nextPosition
                            layoutManager.startSmoothScroll(smoothScroller)
                        }
                        else -> {
                            val nextPosition = maxOf(firstVisiblePosition - 1, 0)
                            smoothScroller.targetPosition = nextPosition
                            layoutManager.startSmoothScroll(smoothScroller)
                        }
                    }
                }

                // Programmer le prochain scroll
                autoScrollProjectsHandler?.postDelayed(this, PROJECT_SCROLL_DELAY)
            }
        }

        // Démarrer le premier scroll après un délai initial
        autoScrollProjectsHandler?.postDelayed(autoScrollProjectsRunnable!!, PROJECT_SCROLL_DELAY)
    }

    private fun stopAutoScrollProjects() {
        autoScrollProjectsRunnable?.let { runnable ->
            autoScrollProjectsHandler?.removeCallbacks(runnable)
        }
        autoScrollProjectsHandler = null
        autoScrollProjectsRunnable = null
    }

    private fun setupClickListeners() {
        // Bouton Add (Dépôt)
        binding.buttonAdd.setOnClickListener {
            showTransactionDialog("Dépôt")
        }

        // Bouton Send (Retrait)
        binding.buttonSend.setOnClickListener {
            showTransactionDialog("Retrait")
        }

        // Bouton Rembourser (Navigation vers Emprunt)
        binding.buttonRembourser.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_homeFragment_to_empruntFragment)
            } catch (e: Exception) {
                findNavController().navigate(R.id.nav_emprunt)
            }
        }

        // Navigation vers TransactionFragment quand on clique sur "Voir tout" des transactions
        binding.voirToutTransactions.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_homeFragment_to_transactionFragment)
            } catch (e: Exception) {
                findNavController().navigate(R.id.nav_transaction)
                Toast.makeText(requireContext(), "Navigation vers les transactions", Toast.LENGTH_SHORT).show()
            }
        }

        // Navigation vers ProjectFragment quand on clique sur "Voir tout" des projets
        binding.voirToutProjet.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_homeFragment_to_projectFragment)
            } catch (e: Exception) {
                findNavController().navigate(R.id.nav_projet)
                Toast.makeText(requireContext(), "Navigation vers les projets", Toast.LENGTH_SHORT).show()
            }
        }

        // Arrêter l'auto-scroll des transactions quand l'utilisateur touche la liste
        binding.recyclerTransactionsHome.setOnTouchListener { _, _ ->
            stopAutoScroll()
            // Redémarrer l'auto-scroll après 5 secondes d'inactivité
            autoScrollHandler = Handler(Looper.getMainLooper())
            autoScrollHandler?.postDelayed({
                if (transactionAdapter.itemCount > 1) {
                    startAutoScroll()
                }
            }, 5000)
            false // Permettre le traitement normal du touch
        }

        // Arrêter l'auto-scroll des projets quand l'utilisateur touche la liste
        binding.recyclerProjectsHome.setOnTouchListener { _, _ ->
            stopAutoScrollProjects()
            // Redémarrer l'auto-scroll après 5 secondes d'inactivité
            autoScrollProjectsHandler = Handler(Looper.getMainLooper())
            autoScrollProjectsHandler?.postDelayed({
                if (projetAdapter.itemCount > 1) {
                    startAutoScrollProjects()
                }
            }, 5000)
            false // Permettre le traitement normal du touch
        }
    }

    private fun showTransactionDialog(fixedType: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)

        // Configurer le type de transaction fixe
        val typeDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.transactionTypeDropdown)
        typeDropdown.setText(fixedType)
        typeDropdown.isEnabled = false // Désactiver la modification

        val dateEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.transactionDate)
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateEditText.setText(dateFormat.format(calendar.time))

        dateEditText.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    dateEditText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Configurer les banques
        val bankDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.transactionBankDropdown)

        viewModel.banks.observe(viewLifecycleOwner) { banks ->
            if (banks.isNotEmpty()) {
                val bankAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, banks)
                bankDropdown.setAdapter(bankAdapter)
                bankDropdown.setText(banks.first(), false)
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ajouter une transaction")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { dialog, _ ->
                val amountText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.transactionAmount).text.toString()
                val date = calendar.time
                val selectedBank = bankDropdown.text.toString()

                if (amountText.isBlank()) {
                    Toast.makeText(requireContext(), "Veuillez saisir un montant", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amount = amountText.toDoubleOrNull() ?: run {
                    Toast.makeText(requireContext(), "Montant invalide", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.addTransaction(
                    type = fixedType,
                    amount = amount,
                    date = date,
                    bankName = selectedBank,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Transaction ajoutée", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Toast.makeText(requireContext(), "Erreur: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // Nouvelle méthode pour afficher le dialogue d'investissement dans un projet
    private fun showInvestProjectDialog(projet: Projet) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_invest_project, null)

        // Récupérer les vues du dialogue
        val tvProjectTitle = dialogView.findViewById<TextView>(R.id.tvProjectTitle)
        val tvCurrentAmount = dialogView.findViewById<TextView>(R.id.tvCurrentAmount)
        val tvNeededAmount = dialogView.findViewById<TextView>(R.id.tvNeededAmount)
        val inputLayoutAmount = dialogView.findViewById<TextInputLayout>(R.id.inputLayoutAmount)
        val etInvestAmount = dialogView.findViewById<TextInputEditText>(R.id.etInvestAmount)
        val tvAvailableBalance = dialogView.findViewById<TextView>(R.id.tvAvailableBalance)

        // Configurer les informations du projet
        tvProjectTitle.text = "Investir dans ${projet.nom}"
        tvCurrentAmount.text = "${numberFormat.format(projet.montant_actuel)} MGA"
        tvNeededAmount.text = "${numberFormat.format(projet.montant_necessaire)} MGA"

        // Afficher le solde disponible
        viewModel.soldeUtilisateur.observe(viewLifecycleOwner) { solde ->
            solde?.let {
                tvAvailableBalance.text = "Solde disponible: ${numberFormat.format(it)} MGA"
            }
        }

        // Calculer le montant restant nécessaire
        val montantRestantNecessaire = projet.montant_necessaire - projet.montant_actuel

        // Ajouter un TextWatcher pour valider l'entrée en temps réel
        etInvestAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val montantText = s.toString()
                if (montantText.isEmpty()) {
                    inputLayoutAmount.error = null
                    return
                }

                val montant = montantText.toDoubleOrNull()
                if (montant == null) {
                    inputLayoutAmount.error = "Montant invalide"
                    return
                }

                // Vérifier si le montant est négatif
                if (montant <= 0) {
                    inputLayoutAmount.error = "Le montant doit être positif"
                    return
                }

                // Vérifier si le montant dépasse le solde disponible
                viewModel.soldeUtilisateur.value?.let { solde ->
                    if (montant > solde) {
                        inputLayoutAmount.error = "Montant supérieur au solde disponible"
                        return
                    }
                }

                // Vérifier si le montant dépasse ce qui est nécessaire
                if (montant > montantRestantNecessaire) {
                    inputLayoutAmount.error = "Montant supérieur à ce qui est nécessaire (${numberFormat.format(montantRestantNecessaire)} MGA)"
                    return
                }

                // Tout est valide
                inputLayoutAmount.error = null
            }
        })

        // Créer et afficher le dialogue
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Investissement")
            .setView(dialogView)
            .setPositiveButton("Investir", null) // On définit le comportement plus tard
            .setNegativeButton("Annuler", null)
            .create()

        dialog.show()

        // Configurer le bouton positif pour éviter qu'il ne ferme le dialogue en cas d'erreur
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val montantText = etInvestAmount.text.toString()

            if (montantText.isEmpty()) {
                inputLayoutAmount.error = "Veuillez saisir un montant"
                return@setOnClickListener
            }

            val montant = montantText.toDoubleOrNull()
            if (montant == null) {
                inputLayoutAmount.error = "Montant invalide"
                return@setOnClickListener
            }

            // Vérifier si le montant est négatif
            if (montant <= 0) {
                inputLayoutAmount.error = "Le montant doit être positif"
                return@setOnClickListener
            }

            // Vérifier si le montant dépasse le solde disponible
            viewModel.soldeUtilisateur.value?.let { solde ->
                if (montant > solde) {
                    inputLayoutAmount.error = "Montant supérieur au solde disponible"
                    return@setOnClickListener
                }
            }

            // Vérifier si le montant dépasse ce qui est nécessaire
            if (montant > montantRestantNecessaire) {
                inputLayoutAmount.error = "Montant supérieur à ce qui est nécessaire"
                return@setOnClickListener
            }

            // Tout est valide, procéder à l'investissement
            viewModel.investInProject(
                projetId = projet.id,
                montantInvestissement = montant,
                onSuccess = {
                    Toast.makeText(requireContext(), "Investissement réussi", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                },
                onError = { error ->
                    Toast.makeText(requireContext(), "Erreur: $error", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Redémarrer les auto-scrolls quand le fragment redevient visible
        if (transactionAdapter.itemCount > 1) {
            startAutoScroll()
        }
        if (projetAdapter.itemCount > 1) {
            startAutoScrollProjects()
        }
    }

    override fun onPause() {
        super.onPause()
        // Arrêter les auto-scrolls quand le fragment n'est plus visible
        stopAutoScroll()
        stopAutoScrollProjects()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoScroll()
        stopAutoScrollProjects()
        _binding = null
    }
}

class HomeViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}