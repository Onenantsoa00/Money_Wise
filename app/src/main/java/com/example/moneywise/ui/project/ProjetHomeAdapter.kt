package com.example.moneywise.ui.home.adapters

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moneywise.R
import com.example.moneywise.data.entity.Projet
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.Locale

class ProjetHomeAdapter(
    private var projets: List<Projet>,
    private val onProjetClick: (Projet) -> Unit
) : RecyclerView.Adapter<ProjetHomeAdapter.ProjetViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_projet_home, parent, false)
        return ProjetViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjetViewHolder, position: Int) {
        val projet = projets[position]
        holder.bind(projet)
        holder.itemView.setOnClickListener { onProjetClick(projet) }
    }

    override fun getItemCount(): Int = projets.size

    fun updateList(newList: List<Projet>) {
        this.projets = newList
        notifyDataSetChanged()
    }

    class ProjetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentage)
        private val tvProjectName: TextView = itemView.findViewById(R.id.tvProjectName)
        private val tvCurrentAmount: TextView = itemView.findViewById(R.id.tvCurrentAmount)
        private val tvNeededAmount: TextView = itemView.findViewById(R.id.tvNeededAmount)

        private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 0
        }

        fun bind(projet: Projet) {
            // Réinitialiser la ProgressBar
            progressBar.progress = 0

            // Mettre à jour le nom du projet
            tvProjectName.text = projet.nom

            // Mettre à jour les montants
            tvCurrentAmount.text = numberFormat.format(projet.montant_actuel)
            tvNeededAmount.text = numberFormat.format(projet.montant_necessaire)

            // Changer la couleur en fonction de la progression
            when {
                projet.progression >= 75 -> {
                    progressBar.progressDrawable = itemView.context.getDrawable(R.drawable.circular_progress_bar_green)
                    tvPercentage.setTextColor(itemView.context.getColor(R.color.green))
                }
                projet.progression >= 50 -> {
                    progressBar.progressDrawable = itemView.context.getDrawable(R.drawable.circular_progress_bar)
                    tvPercentage.setTextColor(itemView.context.getColor(R.color.blue))
                }
                projet.progression >= 25 -> {
                    progressBar.progressDrawable = itemView.context.getDrawable(R.drawable.circular_progress_bar_orange)
                    tvPercentage.setTextColor(itemView.context.getColor(R.color.orange))
                }
                else -> {
                    progressBar.progressDrawable = itemView.context.getDrawable(R.drawable.circular_progress_bar_red)
                    tvPercentage.setTextColor(itemView.context.getColor(R.color.red))
                }
            }

            // Animer la progression
            animateProgress(projet.progression)
        }

        private fun animateProgress(targetProgress: Int) {
            // Mettre à jour le texte du pourcentage immédiatement
            tvPercentage.text = "$targetProgress%"

            // Animer la barre de progression
            ObjectAnimator.ofInt(progressBar, "progress", 0, targetProgress).apply {
                duration = 1000 // 1 seconde
                start()
            }
        }
    }
}