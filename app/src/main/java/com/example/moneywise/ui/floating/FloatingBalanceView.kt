package com.example.moneywise.ui.floating

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.moneywise.MainActivity
import com.example.moneywise.R
import com.example.moneywise.databinding.ViewFloatingBalanceBinding
import com.example.moneywise.services.FloatingBalanceService
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

class FloatingBalanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewFloatingBalanceBinding
    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // Variables pour le drag & drop
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    companion object {
        private const val TAG = "FloatingBalanceView"
    }

    init {
        try {
            Log.d(TAG, "Initialisation de FloatingBalanceView")
            binding = ViewFloatingBalanceBinding.inflate(LayoutInflater.from(context), this, true)
            setupView()
            setupClickListeners()
            setupDragAndDrop()
            Log.d(TAG, "FloatingBalanceView initialisée avec succès")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation de FloatingBalanceView", e)
            throw e
        }
    }

    private fun setupView() {
        // Animation d'apparition plus douce avec transparence
        alpha = 0f
        scaleX = 0.9f
        scaleY = 0.9f

        animate()
            .alpha(0.92f)  // Transparence légère
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(250)
            .start()
    }

    private fun setupClickListeners() {
        // Gestion des clics sur les ImageView
        binding.root.setOnClickListener {
            if (!isDragging) {
                openMainApp()
            }
        }

        binding.btnClose.setOnClickListener {
            Log.d(TAG, "Bouton fermer cliqué")
            hideWithAnimation()
        }

        binding.btnMinimize.setOnClickListener {
            Log.d(TAG, "Bouton minimiser cliqué")
            toggleMinimized()
        }
    }

    private fun setupDragAndDrop() {
        binding.dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY

                    if (abs(deltaX) > 8 || abs(deltaY) > 8) {  // Seuil réduit pour plus de réactivité
                        isDragging = true
                        layoutParams?.x = initialX + deltaX.toInt()
                        layoutParams?.y = initialY + deltaY.toInt()
                        windowManager?.updateViewLayout(this, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        snapToEdge()
                    }
                    postDelayed({ isDragging = false }, 100)
                    true
                }
                else -> false
            }
        }
    }

    private fun snapToEdge() {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val currentX = layoutParams?.x ?: 0

        val targetX = if (currentX < screenWidth / 2) {
            10 // Snap plus proche du bord gauche
        } else {
            screenWidth - width - 10 // Snap plus proche du bord droit
        }

        ObjectAnimator.ofInt(currentX, targetX).apply {
            duration = 150  // Animation plus rapide
            addUpdateListener { animation ->
                layoutParams?.x = animation.animatedValue as Int
                windowManager?.updateViewLayout(this@FloatingBalanceView, layoutParams)
            }
            start()
        }
    }

    fun setWindowManager(wm: WindowManager, params: WindowManager.LayoutParams) {
        windowManager = wm
        layoutParams = params
    }

    fun updateBalance(balance: Double) {
        try {
            val formatter = NumberFormat.getNumberInstance(Locale.FRANCE)
            val formattedBalance = formatter.format(balance)

            binding.tvBalance.text = "$formattedBalance MGA"

            // Animation de mise à jour plus subtile
            binding.tvBalance.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(100)
                .withEndAction {
                    binding.tvBalance.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()

            // Changer la couleur selon le solde
            val color = when {
                balance > 100000 -> ContextCompat.getColor(context, R.color.green_500)
                balance > 50000 -> ContextCompat.getColor(context, R.color.orange_500)
                else -> ContextCompat.getColor(context, R.color.red_500)
            }
            binding.balanceIndicator.setColorFilter(color)

            Log.d(TAG, "Solde mis à jour: $formattedBalance MGA")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la mise à jour du solde", e)
        }
    }

    fun updateUserName(name: String) {
        try {
            // Nom plus court pour économiser l'espace
            val shortName = if (name.length > 8) "${name.take(8)}..." else name
            binding.tvUserName.text = "Salut, $shortName!"
            Log.d(TAG, "Nom utilisateur mis à jour: $shortName")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la mise à jour du nom", e)
        }
    }

    private var isMinimized = false

    private fun toggleMinimized() {
        isMinimized = !isMinimized

        if (isMinimized) {
            binding.contentLayout.visibility = View.GONE
            binding.minimizedLayout.visibility = View.VISIBLE
            binding.btnMinimize.setImageResource(R.drawable.ic_expand)
        } else {
            binding.contentLayout.visibility = View.VISIBLE
            binding.minimizedLayout.visibility = View.GONE
            binding.btnMinimize.setImageResource(R.drawable.ic_minimize)
        }

        // Animation plus douce pour le mode minimisé
        animate()
            .scaleX(if (isMinimized) 0.8f else 1f)
            .scaleY(if (isMinimized) 0.8f else 1f)
            .alpha(if (isMinimized) 0.85f else 0.92f)  // Plus transparent en mode minimisé
            .setDuration(150)
            .start()
    }

    private fun openMainApp() {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)

            // Animation plus subtile au clic
            animate()
                .scaleX(0.98f)
                .scaleY(0.98f)
                .setDuration(80)
                .withEndAction {
                    animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(80)
                        .start()
                }
                .start()

            Log.d(TAG, "Application principale ouverte")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'ouverture de l'application", e)
        }
    }

    private fun hideWithAnimation() {
        animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(150)  // Animation plus rapide
            .withEndAction {
                try {
                    val intent = Intent(context, FloatingBalanceService::class.java).apply {
                        action = FloatingBalanceService.ACTION_STOP_FLOATING
                    }
                    context.startService(intent)
                    Log.d(TAG, "Widget fermé avec animation")
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la fermeture du widget", e)
                }
            }
            .start()
    }
}
