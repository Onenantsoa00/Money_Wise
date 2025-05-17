package com.example.moneywise.ui.emprunt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moneywise.R
import com.example.moneywise.data.entity.Emprunt
import java.text.SimpleDateFormat
import java.util.Locale

class EmpruntAdapter(
    private val emprunts: List<Emprunt>,
    private val onRembourserClick: (Emprunt) -> Unit
) : RecyclerView.Adapter<EmpruntAdapter.EmpruntViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    class EmpruntViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textNom: TextView = itemView.findViewById(R.id.text_nom)
        val textContact: TextView = itemView.findViewById(R.id.text_contact)
        val textMontant: TextView = itemView.findViewById(R.id.text_montant)
        val textDateEmprunt: TextView = itemView.findViewById(R.id.text_date_emprunt)
        val textDateRemboursement: TextView = itemView.findViewById(R.id.text_date_remboursement)
        val initials: TextView = itemView.findViewById(R.id.initials)
        val btnRembourser: Button = itemView.findViewById(R.id.btn_rembourser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmpruntViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emprunt, parent, false)
        return EmpruntViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmpruntViewHolder, position: Int) {
        val emprunt = emprunts[position]

        // Extraire les initiales
        val initials = try {
            emprunt.nom_emprunte.split(" ").take(2)
                .filter { it.isNotEmpty() }
                .joinToString("") { it.first().toString().uppercase() }
        } catch (e: Exception) {
            "?"
        }

        holder.initials.text = initials
        holder.textNom.text = emprunt.nom_emprunte
        holder.textContact.text = emprunt.contacte
        holder.textMontant.text = "${emprunt.montant} MGA"
        holder.textDateEmprunt.text = dateFormat.format(emprunt.date_emprunt)
        holder.textDateRemboursement.text = dateFormat.format(emprunt.date_remboursement)

        // Configurer le bouton Rembourser
        holder.btnRembourser.visibility = if (emprunt.estRembourse) View.GONE else View.VISIBLE
        holder.btnRembourser.setOnClickListener {
            onRembourserClick(emprunt)
        }
    }

    override fun getItemCount() = emprunts.size
}