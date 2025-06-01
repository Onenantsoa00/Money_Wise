package com.example.moneywise.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.moneywise.MainActivity
import com.example.moneywise.R
import com.example.moneywise.databinding.ActivitySplashBinding
import com.example.moneywise.ui.auth.LoginActivity
import com.example.moneywise.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var sessionManager: SessionManager
    private val SPLASH_DELAY = 2000L // 2 secondes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialiser le gestionnaire de session
        sessionManager = SessionManager(this)

        // Animer le logo
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        binding.ivLogo.startAnimation(fadeIn)
        binding.tvAppName.startAnimation(slideUp)

        // Délai avant de vérifier l'état de connexion
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStatus()
        }, SPLASH_DELAY)
    }

    private fun checkLoginStatus() {
        if (sessionManager.isLoggedIn()) {
            // L'utilisateur est déjà connecté, rediriger vers MainActivity
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // L'utilisateur n'est pas connecté, rediriger vers LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Terminer l'activité pour qu'elle ne soit pas accessible via le bouton retour
        finish()
    }
}