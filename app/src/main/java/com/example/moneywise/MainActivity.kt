package com.example.moneywise

import android.app.DatePickerDialog
import android.os.Bundle
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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Banque
import com.example.moneywise.databinding.ActivityMainBinding
import com.example.moneywise.databinding.ItemBanqueBinding
import com.example.moneywise.expenses.BanqueViewModel
import com.example.moneywise.ui.Banque.BanqueAdapter
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import com.example.moneywise.data.entity.Transaction
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private val banqueViewModel: BanqueViewModel by viewModels()
    private val banqueNames = mutableListOf<String>()

    @Inject
    lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        observeUserBalance()

        setupNavigation()
    }

    private fun setupNavigation() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_profile,
                R.id.nav_projet,
                R.id.nav_historique,
                R.id.nav_emprunt,
                R.id.nav_acquittement
            ),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun observeUserBalance() {
        lifecycleScope.launchWhenStarted {
            db.utilisateurDao().getAllUtilisateurs().collect { users ->
                users.firstOrNull()?.let { user ->
                    updateBalanceInToolbar(user.solde)
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
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.Banque -> {
                showAddBanqueDialog()
                true
            }
            R.id.banque_setting -> {
                showBanqueSettingsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Fermer", null)
            .show()
    }

    private fun showUpdateBanqueDialog(banque: Banque) {
        val editText = EditText(this).apply {
            setText(banque.nom)
            hint = "Nouveau nom de la banque"
        }

        AlertDialog.Builder(this)
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

        val dialog = AlertDialog.Builder(this)
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

        val dialog = AlertDialog.Builder(this)
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
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}