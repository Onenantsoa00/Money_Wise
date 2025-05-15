package com.example.moneywise.ui.project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.moneywise.R
import com.example.moneywise.data.entity.Projet
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ProjectAdapter(private val onProjectClick: (Projet) -> Unit) :
    ListAdapter<Projet, ProjectAdapter.ProjectViewHolder>(ProjectDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_projet, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val projet = getItem(position)
        holder.bind(projet)
        holder.itemView.setOnClickListener { onProjectClick(projet) }
    }

    class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardProject: MaterialCardView = itemView.findViewById(R.id.card_project)
        private val cardIconBackground: MaterialCardView = itemView.findViewById(R.id.card_icon_background)
        private val imgProjectIcon: ImageView = itemView.findViewById(R.id.img_project_icon)
        private val tvProjectName: TextView = itemView.findViewById(R.id.tv_project_name)
        private val cardStatusBackground: MaterialCardView = itemView.findViewById(R.id.card_status_background)
        private val tvProjectStatus: TextView = itemView.findViewById(R.id.tv_project_status)
        private val tvBudgetTotal: TextView = itemView.findViewById(R.id.tv_budget_total)
        private val tvBudgetCurrent: TextView = itemView.findViewById(R.id.tv_budget_current)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
        private val tvProgressPercent: TextView = itemView.findViewById(R.id.tv_progress_percent)
        private val tvDeadline: TextView = itemView.findViewById(R.id.tv_deadline)

        private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 0
        }

        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun bind(projet: Projet) {
            tvProjectName.text = projet.nom

            // Formater les montants
            val montantNecessaireFormatted = "${numberFormat.format(projet.montant_necessaire)} MGA"
            val montantActuelFormatted = "${numberFormat.format(projet.montant_actuel)} MGA"
            tvBudgetTotal.text = montantNecessaireFormatted
            tvBudgetCurrent.text = montantActuelFormatted

            // Progression
            progressBar.progress = projet.progression
            tvProgressPercent.text = "${projet.progression}%"

            // Date limite
            tvDeadline.text = "Échéance: ${dateFormat.format(projet.date_limite)}"

            // Statut du projet
            when {
                projet.progression == 100 -> {
                    tvProjectStatus.text = "Complété"
                    cardStatusBackground.setCardBackgroundColor(itemView.context.getColor(R.color.completed_bg))
                    tvProjectStatus.setTextColor(itemView.context.getColor(R.color.completed_text))
                    cardIconBackground.setCardBackgroundColor(itemView.context.getColor(R.color.completed_light_bg))
                    imgProjectIcon.setColorFilter(itemView.context.getColor(R.color.completed_text))
                    // Choisir l'icône appropriée
                    imgProjectIcon.setImageResource(R.drawable.ic_check)
                }
                projet.progression >= 50 -> {
                    tvProjectStatus.text = "En cours"
                    cardStatusBackground.setCardBackgroundColor(itemView.context.getColor(R.color.active_bg))
                    tvProjectStatus.setTextColor(itemView.context.getColor(R.color.active_text))
                    cardIconBackground.setCardBackgroundColor(itemView.context.getColor(R.color.active_light_bg))
                    imgProjectIcon.setColorFilter(itemView.context.getColor(R.color.active_text))
                    // Choisir l'icône appropriée en fonction du nom du projet
                    if (projet.nom.contains("maison", ignoreCase = true)) {
                        imgProjectIcon.setImageResource(R.drawable.ic_home)
                    } else if (projet.nom.contains("voiture", ignoreCase = true)) {
                        imgProjectIcon.setImageResource(R.drawable.ic_car)
                    } else {
                        imgProjectIcon.setImageResource(R.drawable.ic_project)
                    }
                }
                else -> {
                    tvProjectStatus.text = "En cours"
                    cardStatusBackground.setCardBackgroundColor(itemView.context.getColor(R.color.ongoing_bg))
                    tvProjectStatus.setTextColor(itemView.context.getColor(R.color.ongoing_text))
                    cardIconBackground.setCardBackgroundColor(itemView.context.getColor(R.color.ongoing_light_bg))
                    imgProjectIcon.setColorFilter(itemView.context.getColor(R.color.ongoing_text))
                    // Choisir l'icône appropriée en fonction du nom du projet
                    if (projet.nom.contains("maison", ignoreCase = true)) {
                        imgProjectIcon.setImageResource(R.drawable.ic_home)
                    } else if (projet.nom.contains("voiture", ignoreCase = true)) {
                        imgProjectIcon.setImageResource(R.drawable.ic_car)
                    } else {
                        imgProjectIcon.setImageResource(R.drawable.ic_project)
                    }
                }
            }
        }
    }

    class ProjectDiffCallback : DiffUtil.ItemCallback<Projet>() {
        override fun areItemsTheSame(oldItem: Projet, newItem: Projet): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Projet, newItem: Projet): Boolean {
            return oldItem == newItem
        }
    }
}