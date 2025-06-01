package com.example.moneywise.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.moneywise.MainActivity
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class FloatingBalanceService : Service() {

    companion object {
        private const val TAG = "FloatingBalanceService"
        const val ACTION_START_FLOATING = "START_FLOATING"
        const val ACTION_STOP_FLOATING = "STOP_FLOATING"
        const val ACTION_UPDATE_BALANCE = "UPDATE_BALANCE"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "floating_widget_channel"
    }

    @Inject
    lateinit var database: AppDatabase

    private lateinit var sessionManager: SessionManager
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var isFloatingViewVisible = false
    private var isMinimized = false

    // Coroutines
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var balanceJob: Job? = null

    // Views
    private var tvBalance: TextView? = null
    private var tvUserName: TextView? = null
    private var contentLayout: LinearLayout? = null
    private var minimizedLayout: LinearLayout? = null
    private var dragHandle: LinearLayout? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 Service créé")

        sessionManager = SessionManager(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📨 Commande reçue: ${intent?.action}")

        // 🔥 CORRECTION: Vérifier que l'intent n'est pas null
        val action = intent?.action ?: ""

        when (action) {
            ACTION_START_FLOATING -> {
                startForegroundService()
                showFloatingWidget()
            }
            ACTION_STOP_FLOATING -> {
                hideFloatingWidget()
                stopSelf()
            }
            ACTION_UPDATE_BALANCE -> {
                updateBalance()
            }
            else -> {
                Log.w(TAG, "⚠️ Action inconnue ou intent null: $action")
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Widget Flottant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service pour le widget flottant MoneyWise"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Widget MoneyWise")
            .setContentText("Widget flottant actif")
            .setSmallIcon(R.drawable.ic_account_balance_wallet)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "🔔 Service en premier plan démarré")
    }

    private fun showFloatingWidget() {
        if (isFloatingViewVisible) {
            Log.d(TAG, "⚠️ Widget déjà visible")
            return
        }

        try {
            // Créer la vue flottante
            floatingView = LayoutInflater.from(this).inflate(R.layout.view_floating_balance, null)

            // Initialiser les vues
            initializeViews()

            // Configurer les paramètres de la fenêtre
            val layoutParams = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 200
            }

            // Ajouter la vue à la fenêtre
            windowManager.addView(floatingView, layoutParams)
            isFloatingViewVisible = true

            // Configurer les interactions
            setupTouchListeners(layoutParams)
            setupClickListeners()

            // Démarrer l'observation du solde
            startBalanceObservation()

            Log.d(TAG, "✅ Widget flottant affiché")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de l'affichage du widget", e)
        }
    }

    private fun initializeViews() {
        floatingView?.let { view ->
            tvBalance = view.findViewById(R.id.tvBalance)
            tvUserName = view.findViewById(R.id.tvUserName)
            contentLayout = view.findViewById(R.id.contentLayout)
            minimizedLayout = view.findViewById(R.id.minimizedLayout)
            dragHandle = view.findViewById(R.id.dragHandle)
        }
    }

    private fun setupClickListeners() {
        // Clic sur le widget pour ouvrir l'app
        floatingView?.setOnClickListener {
            openMainActivity()
        }

        // Bouton minimiser
        floatingView?.findViewById<ImageView>(R.id.btnMinimize)?.setOnClickListener {
            toggleMinimize()
        }

        // Bouton fermer
        floatingView?.findViewById<ImageView>(R.id.btnClose)?.setOnClickListener {
            hideFloatingWidget()
            stopSelf()
        }
    }

    private fun setupTouchListeners(layoutParams: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        dragHandle?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()

                    // 🔥 CORRECTION: Vérifier que la vue existe avant de la mettre à jour
                    floatingView?.let { view ->
                        try {
                            windowManager.updateViewLayout(view, layoutParams)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Erreur lors de la mise à jour de la position", e)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleMinimize() {
        isMinimized = !isMinimized

        if (isMinimized) {
            contentLayout?.visibility = View.GONE
            minimizedLayout?.visibility = View.VISIBLE
            Log.d(TAG, "📦 Widget minimisé")
        } else {
            contentLayout?.visibility = View.VISIBLE
            minimizedLayout?.visibility = View.GONE
            Log.d(TAG, "📖 Widget restauré")
        }
    }

    private fun openMainActivity() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            Log.d(TAG, "🚀 Application principale ouverte")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de l'ouverture de l'application", e)
        }
    }

    private fun startBalanceObservation() {
        balanceJob?.cancel()
        balanceJob = serviceScope.launch {
            try {
                database.utilisateurDao().getAllUtilisateurs().collectLatest { users ->
                    users.firstOrNull()?.let { user ->
                        updateBalanceUI(user.solde, user.nom)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de l'observation du solde", e)
            }
        }
    }

    private fun updateBalance() {
        Log.d(TAG, "🔄 Mise à jour manuelle du solde demandée")
        // La mise à jour se fait automatiquement via l'observation
    }

    private fun updateBalanceUI(balance: Double, userName: String) {
        serviceScope.launch {
            try {
                val formatter = NumberFormat.getInstance(Locale.getDefault())
                tvBalance?.text = "${formatter.format(balance)} MGA"
                tvUserName?.text = "Salut, $userName!"
                Log.d(TAG, "💰 Solde mis à jour: ${formatter.format(balance)} MGA")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de la mise à jour de l'UI", e)
            }
        }
    }

    private fun hideFloatingWidget() {
        try {
            floatingView?.let { view ->
                windowManager.removeView(view)
                floatingView = null
                isFloatingViewVisible = false
                Log.d(TAG, "🚫 Widget flottant masqué")
            }

            balanceJob?.cancel()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors du masquage du widget", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideFloatingWidget()
        balanceJob?.cancel()
        Log.d(TAG, "💀 Service détruit")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}