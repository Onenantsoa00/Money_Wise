package com.example.moneywise.ui.components

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView

/**
 * TextView personnalisé qui permet un défilement horizontal du texte
 * lorsque celui-ci est trop long pour être affiché entièrement
 */
class MarqueeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "MarqueeTextView"
    }

    init {
        Log.d(TAG, "🎬 Initialisation du MarqueeTextView")
        setupMarquee()
    }

    private fun setupMarquee() {
        // Configurer le mode de défilement
        ellipsize = TextUtils.TruncateAt.MARQUEE
        marqueeRepeatLimit = -1  // -1 signifie répéter indéfiniment
        isSingleLine = true
        isSelected = true  // Important: active le défilement
        isFocusable = true
        isFocusableInTouchMode = true

        Log.d(TAG, "✅ Configuration marquee terminée")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "📎 Vue attachée à la fenêtre")
        startMarquee()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "📎 Vue détachée de la fenêtre")
    }

    private fun startMarquee() {
        post {
            isSelected = true
            Log.d(TAG, "🚀 Démarrage du marquee - Texte: ${text}")
            Log.d(TAG, "📏 Largeur de la vue: ${width}, Largeur du texte: ${paint.measureText(text.toString())}")
        }
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        isSelected = true  // Garde le défilement actif même sans focus
        Log.d(TAG, "🎯 Focus changé: $focused, isSelected: $isSelected")
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        isSelected = true  // Garde le défilement actif même si la fenêtre perd le focus
        Log.d(TAG, "🪟 Focus fenêtre changé: $hasWindowFocus, isSelected: $isSelected")
    }

    override fun isFocused(): Boolean {
        return true  // Force le focus pour maintenir le défilement
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        Log.d(TAG, "📝 Nouveau texte défini: $text")
        // Redémarrer le marquee après changement de texte
        post {
            isSelected = true
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.d(TAG, "📐 Mesure - Largeur: ${MeasureSpec.getSize(widthMeasureSpec)}, Mode: ${MeasureSpec.getMode(widthMeasureSpec)}")
    }
}
