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
import android.widget.RadioGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
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
import com.example.moneywise.utils.ReminderManager
import com.example.moneywise.utils.NotificationHelper

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val banqueViewModel: BanqueViewModel by viewModels()
    private val banqueNames = mutableListOf<String>()

    // 🔥 Gestionnaires pour le widget flottant et les notifications
    private lateinit var floatingWidgetManager: FloatingWidgetManager
    private lateinit var sessionManager: SessionManager

    @Inject
    lateinit var db: AppDatabase

    @Inject
    lateinit var reminderManager: ReminderManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    companion object {
        private const val TAG = "MainActivity"
        // 🔥 ID constants pour les menus
        private const val MENU_FLOATING_WIDGET = 9999
        private const val MENU_NOTIFICATIONS = 9998
        // 🔥 Codes de requête pour les permissions
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1002

        // 🔥 Constantes d'intervalle pour éviter les erreurs de référence
        private const val THREE_HOURS = 3 * 60 * 60 * 1000L  // 3 heures en millisecondes
        private const val SIX_HOURS = 6 * 60 * 60 * 1000L    // 6 heures en millisecondes
        private const val TWELVE_HOURS = 12 * 60 * 60 * 1000L // 12 heures en millisecondes
        private const val ONE_DAY = 24 * 60 * 60 * 1000L     // 24 heures en millisecondes
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Appliquer le thème avant super.onCreate()
        ThemeManager.initializeTheme(this)

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔥 Initialiser les gestionnaires
        floatingWidgetManager = FloatingWidgetManager(this)
        sessionManager = SessionManager(this)

        // Vérifier les permissions
        checkAndRequestPermissions()

        setSupportActionBar(binding.appBarMain.toolbar)
        observeUserBalance()
        setupNavigation()

        // 🔥 Démarrer automatiquement les services si activés
        checkAndStartServices()
    }

    // 🔥 NOUVELLE MÉTHODE: Vérifier et demander toutes les permissions
    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        // Permissions SMS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECEIVE_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_SMS)
        }

        // Permission de notification pour Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), Constants.SMS_PERMISSION_REQUEST_CODE)
        }
    }

    // 🔥 NOUVELLE MÉTHODE: Démarrer automatiquement les services
    private fun checkAndStartServices() {
        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "👤 Utilisateur connecté, vérification des services")

            // Démarrer le widget flottant si activé
            Handler(Looper.getMainLooper()).postDelayed({
                floatingWidgetManager.startFloatingWidgetIfEnabled()
            }, 1500)

            // Démarrer les rappels si activés
            Handler(Looper.getMainLooper()).postDelayed({
                reminderManager.restartRemindersIfEnabled()
            }, 2000)
        }
    }

    // 🔥 AJOUT CRUCIAL: Gestion du retour des permissions
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (floatingWidgetManager.hasOverlayPermission()) {
                    Log.d(TAG, "✅ Permission d'overlay accordée")
                    floatingWidgetManager.resetPermissionPreferences()
                    Toast.makeText(this, "✅ Permission accordée ! Le widget peut maintenant être activé.", Toast.LENGTH_LONG).show()

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
    }

    override fun onResume() {
        super.onResume()

        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "🔄 onResume - Vérification des services")
            floatingWidgetManager.startFloatingWidgetIfEnabled()
            reminderManager.restartRemindersIfEnabled()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            Constants.SMS_PERMISSION_REQUEST_CODE -> {
                val smsGranted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
                if (smsGranted) {
                    Toast.makeText(this, "Permissions accordées", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Certaines permissions sont nécessaires pour le bon fonctionnement", Toast.LENGTH_LONG).show()
                }
            }
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "✅ Permission de notification accordée", Toast.LENGTH_SHORT).show()
                    // Proposer d'activer les rappels maintenant que la permission est accordée
                    showReminderIntervalDialog(true)
                } else {
                    Toast.makeText(this, "❌ Permission de notification refusée. Les rappels ne fonctionneront pas.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupNavigation() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_main)

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

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
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
                    if (navController.currentDestination?.id != R.id.nav_emprunt) {
                        try {
                            navController.navigate(R.id.nav_emprunt)
                        } catch (e: Exception) {
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

        navController.addOnDestinationChangedListener { _, destination, _ ->
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
                            val transaction = Transaction(
                                type = type,
                                montants = amount.toString(),
                                date = date,
                                id_utilisateur = user.id,
                                id_banque = bankId
                            )

                            db.transactionDao().insertTransaction(transaction)

                            val newBalance = when (type) {
                                "Dépôt" -> user.solde + amount
                                "Retrait" -> user.solde - amount
                                else -> user.solde
                            }
                            db.utilisateurDao().update(user.copy(solde = newBalance))

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
        updateThemeIcon(menu)

        // 🔥 Ajouter les menus du widget flottant et des notifications
        menu.add(Menu.NONE, MENU_FLOATING_WIDGET, Menu.NONE, "Widget flottant")
            .setIcon(R.drawable.ic_account_balance_wallet)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        menu.add(Menu.NONE, MENU_NOTIFICATIONS, Menu.NONE, "Rappels")
            .setIcon(R.drawable.ic_notifications)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.theme_toggle -> {
                ThemeManager.toggleTheme(this)
                recreate()
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
            MENU_FLOATING_WIDGET -> {
                showFloatingWidgetDialog()
                true
            }
            // 🔥 NOUVEAU: Gestion des rappels avec sélection d'intervalle
            MENU_NOTIFICATIONS -> {
                showNotificationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 🔥 NOUVEAU DIALOGUE: Contrôle des notifications de rappel avec sélection d'intervalle
    private fun showNotificationDialog() {
        val hasPermission = notificationHelper.hasNotificationPermission()
        val isEnabled = reminderManager.areRemindersEnabled()

        if (!hasPermission) {
            // Demander la permission d'abord
            showPermissionRequestDialog()
            return
        }

        // Si la permission est accordée, montrer les options de rappel
        val title = "🔔 Rappels MoneyWise"
        val currentInterval = reminderManager.formatInterval(reminderManager.getCurrentInterval())
        val message = if (isEnabled) {
            "Rappels actifs - Intervalle: $currentInterval\n\nRecevez des rappels pour vos emprunts, acquittements et projets."
        } else {
            "Activez les rappels pour être alerté de vos emprunts, acquittements et projets nécessitant votre attention."
        }

        val positiveButtonText = if (isEnabled) "🛑 Arrêter rappels" else "🚀 Activer rappels"

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ ->
                if (isEnabled) {
                    // Arrêter les rappels
                    reminderManager.stopReminders()
                    showToast("🛑 Rappels arrêtés")
                } else {
                    // Activer les rappels avec sélection d'intervalle
                    showReminderIntervalDialog(true)
                }
            }
            .setNeutralButton(if (isEnabled) "⚙️ Modifier intervalle" else null) { _, _ ->
                if (isEnabled) {
                    showReminderIntervalDialog(false)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // 🔥 NOUVELLE MÉTHODE: Dialogue de demande de permission
    private fun showPermissionRequestDialog() {
        val title = "🔐 Permission requise"
        val message = "Pour recevoir des notifications de rappel, MoneyWise a besoin de la permission d'envoyer des notifications.\n\nCette permission est nécessaire pour vous alerter des emprunts, acquittements et projets nécessitant votre attention."

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("🔐 Accorder permission") { _, _ ->
                requestNotificationPermission()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // 🔥 NOUVELLE MÉTHODE: Dialogue de sélection d'intervalle de rappel
    private fun showReminderIntervalDialog(isActivatingReminders: Boolean = false) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reminder_interval, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radio_group_interval)
        val tvCurrentInterval = dialogView.findViewById<TextView>(R.id.tv_current_interval)

        // Afficher l'intervalle actuel
        val currentInterval = reminderManager.getCurrentInterval()
        tvCurrentInterval.text = "Intervalle actuel : ${reminderManager.formatInterval(currentInterval)}"

        // Sélectionner le bouton radio correspondant à l'intervalle actuel
        when (currentInterval) {
            THREE_HOURS -> radioGroup.check(R.id.radio_3_hours)
            SIX_HOURS -> radioGroup.check(R.id.radio_6_hours)
            TWELVE_HOURS -> radioGroup.check(R.id.radio_12_hours)
            ONE_DAY -> radioGroup.check(R.id.radio_24_hours)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("✅ Confirmer") { _, _ ->
                // Récupérer l'intervalle sélectionné
                val selectedInterval = when (radioGroup.checkedRadioButtonId) {
                    R.id.radio_3_hours -> THREE_HOURS
                    R.id.radio_6_hours -> SIX_HOURS
                    R.id.radio_12_hours -> TWELVE_HOURS
                    R.id.radio_24_hours -> ONE_DAY
                    else -> SIX_HOURS
                }

                // Appliquer l'intervalle
                reminderManager.setReminderInterval(selectedInterval)

                // Si on active les rappels, les démarrer
                if (isActivatingReminders) {
                    reminderManager.startReminders()
                    showToast("✅ Rappels activés - Intervalle: ${reminderManager.formatInterval(selectedInterval)}")
                } else {
                    showToast("🕐 Intervalle modifié: ${reminderManager.formatInterval(selectedInterval)}")
                }
            }
            .setNegativeButton("❌ Annuler", null)
            .setCancelable(false)
            .create()

        dialog.show()
    }

    // 🔥 NOUVELLE MÉTHODE: Demander la permission de notification
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Pour les versions antérieures, la permission est automatiquement accordée
            showToast("✅ Permission accordée")
            showReminderIntervalDialog(true)
        }
    }

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
                        floatingWidgetManager.stopFloatingWidget()
                        showToast("🛑 Widget flottant arrêté")
                    } else {
                        floatingWidgetManager.startFloatingWidget()
                        showToast("🚀 Widget flottant activé")
                    }
                } else {
                    floatingWidgetManager.forceRequestOverlayPermission()
                    showPermissionGuide()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

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

    // 🔥 NOUVELLE MÉTHODE: Afficher un toast
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
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
        // Les services continuent à fonctionner en arrière-plan
    }
}