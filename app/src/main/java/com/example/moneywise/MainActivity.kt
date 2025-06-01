package com.example.moneywise

import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Banque
import com.example.moneywise.databinding.ActivityMainBinding
import com.example.moneywise.expenses.BanqueViewModel
import com.example.moneywise.ui.Banque.BanqueAdapter
import com.example.moneywise.utils.FloatingWidgetManager
import com.example.moneywise.utils.ThemeManager
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.moneywise.data.entity.Transaction
import com.example.moneywise.utils.Constants
import com.example.moneywise.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val banqueViewModel: BanqueViewModel by viewModels()
    private val banqueNames = mutableListOf<String>()

    // 🔥 Gestionnaires pour le widget flottant
    private lateinit var floatingWidgetManager: FloatingWidgetManager
    private lateinit var sessionManager: SessionManager

    @Inject
    lateinit var db: AppDatabase

    companion object {
        private const val TAG = "MainActivity"
        // 🔥 ID constant pour le menu du widget flottant
        private const val MENU_FLOATING_WIDGET = 9999
        // 🔥 AJOUT: Code de requête pour les permissions d'overlay
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Appliquer le thème avant super.onCreate()
        ThemeManager.initializeTheme(this)

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔥 Initialiser les gestionnaires pour le widget flottant
        floatingWidgetManager = FloatingWidgetManager(this)
        sessionManager = SessionManager(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
                100)
        }

        setSupportActionBar(binding.appBarMain.toolbar)
        observeUserBalance()
        checkAndRequestSmsPermissions()
        setupNavigation()

        // 🔥 Démarrer automatiquement le widget si activé
        checkAndStartFloatingWidgetAutomatically()
    }

    // 🔥 AJOUT CRUCIAL: Gestion du retour des permissions d'overlay
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (floatingWidgetManager.hasOverlayPermission()) {
                Log.d(TAG, "✅ Permission d'overlay accordée")
                floatingWidgetManager.resetPermissionPreferences()
                Toast.makeText(this, "✅ Permission accordée ! Le widget peut maintenant être activé.", Toast.LENGTH_LONG).show()

                // 🔥 Démarrer automatiquement le widget après avoir accordé la permission
                Handler(Looper.getMainLooper()).postDelayed({
                    floatingWidgetManager.startFloatingWidget()
                }, 500)
            } else {
                Log.w(TAG, "❌ Permission d'overlay refusée")
                floatingWidgetManager.markPermissionDenied()
                Toast.makeText(this, "❌ Permission refusée. Le widget ne peut pas fonctionner.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 🔥 Démarre automatiquement le widget si les conditions sont remplies
    private fun checkAndStartFloatingWidgetAutomatically() {
        Log.d(TAG, "🔍 Vérification du démarrage automatique du widget")

        // Vérifier si l'utilisateur est connecté
        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "👤 Utilisateur connecté, vérification des conditions")

            // 🔥 AFFICHER les statistiques de permission
            Log.d(TAG, floatingWidgetManager.getPermissionStats())

            // 🔥 DÉMARRAGE AUTOMATIQUE: Si le widget était activé et permission accordée
            Handler(Looper.getMainLooper()).postDelayed({
                floatingWidgetManager.startFloatingWidgetIfEnabled()
            }, 1500) // Délai pour s'assurer que l'activité est prête
        } else {
            Log.d(TAG, "❌ Utilisateur non connecté, widget non démarré")
        }
    }

    // 🔥 Vérifier à nouveau les permissions au retour à l'application
    override fun onResume() {
        super.onResume()

        // 🔥 DÉMARRAGE AUTOMATIQUE au retour dans l'app
        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "🔄 onResume - Vérification du widget")
            floatingWidgetManager.startFloatingWidgetIfEnabled()
        }
    }

    private fun checkAndRequestSmsPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionsNeeded = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.RECEIVE_SMS)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_SMS)
            }

            if (permissionsNeeded.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), Constants.SMS_PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions SMS accordées", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Les permissions SMS sont nécessaires pour le traitement automatique", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupNavigation() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_main)

        // Configuration des destinations de niveau supérieur
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_profile,
                R.id.nav_projet,
                R.id.nav_historique,
                R.id.nav_emprunt,
                R.id.nav_acquittement,
                R.id.nav_transaction
            ),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        // Gestion personnalisée de la navigation du drawer
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Nettoyer la pile de navigation et aller au home
                    if (navController.currentDestination?.id != R.id.nav_home) {
                        navController.popBackStack(R.id.nav_home, false)
                        if (navController.currentDestination?.id != R.id.nav_home) {
                            navController.navigate(R.id.nav_home)
                        }
                    }
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_emprunt -> {
                    // Naviguer vers emprunt
                    if (navController.currentDestination?.id != R.id.nav_emprunt) {
                        try {
                            navController.navigate(R.id.nav_emprunt)
                        } catch (e: Exception) {
                            // Si la navigation échoue, essayer de nettoyer la pile d'abord
                            navController.popBackStack(R.id.nav_home, false)
                            navController.navigate(R.id.nav_emprunt)
                        }
                    }
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_profile -> {
                    if (navController.currentDestination?.id != R.id.nav_profile) {
                        try {
                            navController.navigate(R.id.nav_profile)
                        } catch (e: Exception) {
                            navController.popBackStack(R.id.nav_home, false)
                            navController.navigate(R.id.nav_profile)
                        }
                    }
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_projet -> {
                    if (navController.currentDestination?.id != R.id.nav_projet) {
                        try {
                            navController.navigate(R.id.nav_projet)
                        } catch (e: Exception) {
                            navController.popBackStack(R.id.nav_home, false)
                            navController.navigate(R.id.nav_projet)
                        }
                    }
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_historique -> {
                    if (navController.currentDestination?.id != R.id.nav_historique) {
                        try {
                            navController.navigate(R.id.nav_historique)
                        } catch (e: Exception) {
                            navController.popBackStack(R.id.nav_home, false)
                            navController.navigate(R.id.nav_historique)
                        }
                    }
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_acquittement -> {
                    if (navController.currentDestination?.id != R.id.nav_acquittement) {
                        try {
                            navController.navigate(R.id.nav_acquittement)
                        } catch (e: Exception) {
                            navController.popBackStack(R.id.nav_home, false)
                            navController.navigate(R.id.nav_acquittement)
                        }
                    }
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_transaction -> {
                    if (navController.currentDestination?.id != R.id.nav_transaction) {
                        try {
                            navController.navigate(R.id.nav_transaction)
                        } catch (e: Exception) {
                            navController.popBackStack(R.id.nav_home, false)
                            navController.navigate(R.id.nav_transaction)
                        }
                    }
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }
        }

        // Écouter les changements de destination pour mettre à jour l'état du drawer
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Mettre à jour l'élément sélectionné dans le drawer
            when (destination.id) {
                R.id.nav_home -> navView.setCheckedItem(R.id.nav_home)
                R.id.nav_profile -> navView.setCheckedItem(R.id.nav_profile)
                R.id.nav_projet -> navView.setCheckedItem(R.id.nav_projet)
                R.id.nav_historique -> navView.setCheckedItem(R.id.nav_historique)
                R.id.nav_emprunt -> navView.setCheckedItem(R.id.nav_emprunt)
                R.id.nav_acquittement -> navView.setCheckedItem(R.id.nav_acquittement)
                R.id.nav_transaction -> navView.setCheckedItem(R.id.nav_transaction)
            }
        }
    }

    private fun observeUserBalance() {
        lifecycleScope.launchWhenStarted {
            db.utilisateurDao().getAllUtilisateurs().collect { users ->
                users.firstOrNull()?.let { user ->
                    updateBalanceInToolbar(user.solde)

                    // 🔥 Mettre à jour le widget flottant quand le solde change
                    if (floatingWidgetManager.hasOverlayPermission() && floatingWidgetManager.isWidgetEnabled()) {
                        floatingWidgetManager.updateFloatingWidget()
                    }
                }
            }
        }
        val balanceText = binding.appBarMain.toolbar.findViewById<TextView>(R.id.textView3)
        balanceText.setOnClickListener {
            showAddTransactionDialog()
        }
    }

    private fun showAddTransactionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null)

        val transactionTypes = arrayOf("Dépôt", "Retrait", "Transfert")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, transactionTypes)
        val typeDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.transactionTypeDropdown)
        typeDropdown.setAdapter(typeAdapter)

        val dateEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.transactionDate)
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateEditText.setText(dateFormat.format(calendar.time))

        dateEditText.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    dateEditText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        lifecycleScope.launch {
            val banks = db.banqueDao().getAllBanques().first().map { it.nom }
            if (banks.isNotEmpty()) {
                val bankAdapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, banks)
                val bankDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.transactionBankDropdown)
                bankDropdown.setAdapter(bankAdapter)
                bankDropdown.setText(banks.first(), false)
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Ajouter une transaction")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { dialog, _ ->
                val type = typeDropdown.text.toString()
                val amountText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.transactionAmount).text.toString()
                val date = calendar.time

                if (type.isBlank() || amountText.isBlank()) {
                    Toast.makeText(this@MainActivity, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amount = amountText.toDoubleOrNull() ?: run {
                    Toast.makeText(this@MainActivity, "Montant invalide", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val currentUser = db.utilisateurDao().getFirstUtilisateur()
                    currentUser?.let { user ->
                        val bankName = dialogView.findViewById<AutoCompleteTextView>(R.id.transactionBankDropdown).text.toString()
                        val bankId = if (bankName.isNotBlank()) {
                            db.banqueDao().getBanqueByNom(bankName)?.id ?: 0
                        } else {
                            0
                        }

                        try {
                            // Créer la transaction
                            val transaction = Transaction(
                                type = type,
                                montants = amount.toString(),
                                date = date,
                                id_utilisateur = user.id,
                                id_banque = bankId
                            )

                            // Insérer la transaction
                            db.transactionDao().insertTransaction(transaction)

                            // Mettre à jour le solde de l'utilisateur
                            val newBalance = when (type) {
                                "Dépôt" -> user.solde + amount
                                "Retrait" -> user.solde - amount
                                else -> user.solde
                            }
                            db.utilisateurDao().update(user.copy(solde = newBalance))

                            // 🔥 Mettre à jour le widget flottant après une transaction
                            if (floatingWidgetManager.hasOverlayPermission() && floatingWidgetManager.isWidgetEnabled()) {
                                floatingWidgetManager.updateFloatingWidget()
                            }

                            Toast.makeText(this@MainActivity, "Transaction ajoutée", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Toast.makeText(this@MainActivity, "Utilisateur non trouvé", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun updateBalanceInToolbar(balance: Double) {
        val balanceText = binding.appBarMain.toolbar.findViewById<TextView>(R.id.textView3)
        val formatter = NumberFormat.getInstance(Locale.getDefault())
        balanceText.text = "${formatter.format(balance)} MGA"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        // Mettre à jour l'icône du thème
        updateThemeIcon(menu)

        // 🔥 Ajouter le menu du widget flottant
        menu.add(Menu.NONE, MENU_FLOATING_WIDGET, Menu.NONE, "Widget flottant")
            .setIcon(R.drawable.ic_account_balance_wallet)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Gestion du toggle de thème
            R.id.theme_toggle -> {
                ThemeManager.toggleTheme(this)
                recreate() // Recréer l'activité pour appliquer le nouveau thème
                true
            }
            R.id.Banque -> {
                showAddBanqueDialog()
                true
            }
            R.id.banque_setting -> {
                showBanqueSettingsDialog()
                true
            }
            // 🔥 Gestion du widget flottant
            MENU_FLOATING_WIDGET -> {
                showFloatingWidgetDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 🔥 DIALOGUE SIMPLIFIÉ SELON VOTRE DEMANDE INITIALE
    private fun showFloatingWidgetDialog() {
        val hasPermission = floatingWidgetManager.hasOverlayPermission()
        val isEnabled = floatingWidgetManager.isWidgetEnabled()

        val title = "Widget Flottant"
        val message = if (hasPermission) {
            "Contrôlez l'affichage du widget flottant qui montre votre solde en temps réel."
        } else {
            "Permission requise pour afficher le widget par-dessus les autres applications."
        }

        val positiveButtonText = if (hasPermission) {
            if (isEnabled) "🛑 Arrêter widget" else "🚀 Activer widget"
        } else {
            "🔐 Accorder permission"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ ->
                if (hasPermission) {
                    if (isEnabled) {
                        // Arrêter le widget
                        floatingWidgetManager.stopFloatingWidget()
                        Toast.makeText(this, "🛑 Widget flottant arrêté", Toast.LENGTH_SHORT).show()
                    } else {
                        // Activer le widget
                        floatingWidgetManager.startFloatingWidget()
                        Toast.makeText(this, "🚀 Widget flottant activé", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Demander la permission
                    floatingWidgetManager.forceRequestOverlayPermission()
                    showPermissionGuide()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // 🔥 Guide pour accorder la permission
    private fun showPermissionGuide() {
        val guide = """
            📱 Pour activer le widget flottant :
            
            1️⃣ Trouvez "MoneyWise" dans la liste
            2️⃣ Activez "Autoriser l'affichage par-dessus d'autres applications"
            3️⃣ Revenez dans l'application
            4️⃣ Le widget sera disponible
            
            ⚠️ Cette permission est nécessaire pour afficher votre solde par-dessus toutes les applications.
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("🔐 Guide de Permission")
            .setMessage(guide)
            .setPositiveButton("Compris", null)
            .show()
    }

    // 🔥 GARDER TOUTES VOS MÉTHODES AVANCÉES EXISTANTES
    private fun showWidgetStats() {
        val stats = floatingWidgetManager.getPermissionStats()
        MaterialAlertDialogBuilder(this)
            .setTitle("📊 Statistiques du Widget")
            .setMessage(stats)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAdvancedSettings() {
        val options = arrayOf(
            "🔄 Réinitialiser les préférences",
            "🔧 Forcer le redémarrage",
            "🧹 Nettoyer le cache",
            "🔍 Mode debug"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("⚙️ Paramètres Avancés")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Réinitialiser les préférences
                        floatingWidgetManager.resetPermissionPreferences()
                        Toast.makeText(this, "🔄 Préférences réinitialisées", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        // Forcer le redémarrage
                        floatingWidgetManager.stopFloatingWidget()
                        Handler(Looper.getMainLooper()).postDelayed({
                            floatingWidgetManager.startFloatingWidgetWithPermissionRequest()
                            Toast.makeText(this, "🔧 Redémarrage forcé", Toast.LENGTH_SHORT).show()
                        }, 1000)
                    }
                    2 -> {
                        // Nettoyer le cache (simulation)
                        Toast.makeText(this, "🧹 Cache nettoyé", Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        // Mode debug
                        val debugInfo = """
                            🔍 Informations de Debug:
                            
                            ${floatingWidgetManager.getPermissionStats()}
                            
                            📱 Système: Android ${Build.VERSION.RELEASE}
                            🏗️ SDK: ${Build.VERSION.SDK_INT}
                            📦 App: ${packageName}
                        """.trimIndent()

                        MaterialAlertDialogBuilder(this)
                            .setTitle("🔍 Mode Debug")
                            .setMessage(debugInfo)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("Retour", null)
            .show()
    }

    private fun showActivationGuide() {
        val guide = """
            🎯 Guide complet d'activation du Widget Flottant:
            
            📋 ÉTAPES:
            1️⃣ Menu → Widget flottant → Accorder la permission
            2️⃣ Dans les paramètres: Activer "Affichage par-dessus d'autres apps"
            3️⃣ Revenir dans MoneyWise
            4️⃣ Le widget apparaît automatiquement
            
            ✨ FONCTIONNALITÉS:
            • 💰 Affiche votre solde en temps réel
            • 📱 Visible dans toutes les applications
            • 🎮 Déplaçable et redimensionnable
            • 🔄 Mise à jour automatique
            
            🔧 CONTRÔLES:
            • Glisser la barre du haut pour déplacer
            • Clic sur le widget pour ouvrir l'app
            • Boutons minimiser/fermer disponibles
            
            ⚡ Le widget se lance automatiquement à chaque connexion une fois activé !
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("📖 Guide Complet du Widget")
            .setMessage(guide)
            .setPositiveButton("Commencer", { _, _ ->
                floatingWidgetManager.forceRequestOverlayPermission()
            })
            .setNegativeButton("Plus tard", null)
            .show()
    }

    // Méthode pour mettre à jour l'icône du thème
    private fun updateThemeIcon(menu: Menu) {
        val themeItem = menu.findItem(R.id.theme_toggle)
        if (ThemeManager.isDarkMode(this)) {
            themeItem.setIcon(R.drawable.ic_light_mode)
            themeItem.title = "Mode clair"
        } else {
            themeItem.setIcon(R.drawable.ic_dark_mode)
            themeItem.title = "Mode sombre"
        }
    }

    // Mettre à jour l'icône quand le menu est préparé
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        updateThemeIcon(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    private fun showBanqueSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_banque_settings, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewBanques)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = BanqueAdapter(
            onDeleteClick = { banque ->
                banqueViewModel.deleteBanque(banque)
            },
            onUpdateClick = { banque ->
                showUpdateBanqueDialog(banque)
            }
        )

        recyclerView.adapter = adapter

        lifecycleScope.launch {
            banqueViewModel.allBanques.collectLatest { banques ->
                adapter.submitList(banques)
            }
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Fermer", null)
            .show()
    }

    private fun showUpdateBanqueDialog(banque: Banque) {
        val editText = EditText(this).apply {
            setText(banque.nom)
            hint = "Nouveau nom de la banque"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Modifier la banque")
            .setView(editText)
            .setPositiveButton("Valider") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val updatedBanque = banque.copy(nom = newName)
                    banqueViewModel.updateBanque(updatedBanque)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showAddBanqueDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_banque, null)
        val inputLayout = dialogView.findViewById<LinearLayout>(R.id.inputs_layout)
        val firstEditText = EditText(this).apply {
            hint = "Nom de la banque"
        }
        inputLayout.addView(firstEditText)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Ajouter des banques")
            .setView(dialogView)
            .setPositiveButton("Valider") { _, _ ->
                saveBanqueNames(inputLayout)
            }
            .setNeutralButton("Ajouter un autre") { dialogInterface, _ ->
                val newEditText = EditText(this).apply {
                    hint = "Nom de la banque"
                }
                inputLayout.addView(newEditText)
                dialogInterface.dismiss()
                showAddBanqueDialogWithExistingInputs(inputLayout)
            }
            .setNegativeButton("Annuler", null)
            .create()

        dialog.show()
    }

    private fun showAddBanqueDialogWithExistingInputs(inputLayout: LinearLayout) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_banque, null)
        val newInputLayout = dialogView.findViewById<LinearLayout>(R.id.inputs_layout)

        for (i in 0 until inputLayout.childCount) {
            val child = inputLayout.getChildAt(i)
            if (child is EditText) {
                val newEditText = EditText(this).apply {
                    setText(child.text)
                    hint = "Nom de la banque"
                }
                newInputLayout.addView(newEditText)
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Ajouter des banques")
            .setView(dialogView)
            .setPositiveButton("Valider") { _, _ ->
                saveBanqueNames(newInputLayout)
            }
            .setNeutralButton("Ajouter un autre") { dialogInterface, _ ->
                val newEditText = EditText(this).apply {
                    hint = "Nom de la banque"
                }
                newInputLayout.addView(newEditText)
                dialogInterface.dismiss()
                showAddBanqueDialogWithExistingInputs(newInputLayout)
            }
            .setNegativeButton("Annuler", null)
            .create()

        dialog.show()
    }

    private fun saveBanqueNames(inputLayout: LinearLayout) {
        for (i in 0 until inputLayout.childCount) {
            val child = inputLayout.getChildAt(i)
            if (child is EditText) {
                val name = child.text.toString().trim()
                if (name.isNotEmpty()) {
                    banqueViewModel.insertBanque(name)
                }
            }
        }
        banqueNames.clear()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            // Gérer le bouton retour pour revenir au home si on n'y est pas
            if (navController.currentDestination?.id != R.id.nav_home) {
                navController.popBackStack(R.id.nav_home, false)
                if (navController.currentDestination?.id != R.id.nav_home) {
                    navController.navigate(R.id.nav_home)
                }
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ne pas arrêter le widget à la destruction de l'activité
        // car il doit continuer à fonctionner en arrière-plan
    }
}