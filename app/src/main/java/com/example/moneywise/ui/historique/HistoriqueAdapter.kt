package com.example.moneywise.ui.historique

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.moneywise.R
import com.example.moneywise.data.entity.Historique
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class HistoriqueAdapter(private var items: List<Historique>) :
    RecyclerView.Adapter<HistoriqueAdapter.HistoriqueViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val numberFormat = NumberFormat.getInstance(Locale.getDefault())

    class HistoriqueViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardType: CardView = view.findViewById(R.id.card_type)
        val textType: TextView = view.findViewById(R.id.text_type)
        val textMontant: TextView = view.findViewById(R.id.text_montant)
        val textDate: TextView = view.findViewById(R.id.text_date)
        val textMotif: TextView = view.findViewById(R.id.text_motif)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoriqueViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historique, parent, false)
        return HistoriqueViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoriqueViewHolder, position: Int) {
        val item = items[position]

        // 🔥 Formatage du montant avec MGA
        val montantValue = item.montant.toString().replace(",", "").replace(" ", "").replace("MGA", "").trim().toDoubleOrNull() ?: 0.0
        val formattedMontant = numberFormat.format(montantValue)

        when (item.typeTransaction) {
            "ACQUITTEMENT" -> {
                holder.cardType.setCardBackgroundColor(Color.parseColor("#E8F5E8"))
                holder.textType.text = "Acquittement"
                holder.textMontant.text = "-$formattedMontant MGA"
                holder.textMontant.setTextColor(Color.parseColor("#D32F2F"))
            }
            "EMPRUNT" -> {
                holder.cardType.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                holder.textType.text = "Emprunt"
                holder.textMontant.text = "+$formattedMontant MGA"
                holder.textMontant.setTextColor(Color.parseColor("#388E3C"))
            }
            "REMBOURSEMENT_ACQUITTEMENT" -> {
                holder.cardType.setCardBackgroundColor(Color.parseColor("#E8F5E8"))
                holder.textType.text = "Remb. Acquitt."
                holder.textMontant.text = "+$formattedMontant MGA"
                holder.textMontant.setTextColor(Color.parseColor("#388E3C"))
            }
            "REMBOURSEMENT_EMPRUNT" -> {
                holder.cardType.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                holder.textType.text = "Remb. Emprunt"
                holder.textMontant.text = "-$formattedMontant MGA"
                holder.textMontant.setTextColor(Color.parseColor("#D32F2F"))
            }
            // 🔥 Support pour les transactions normales
            "Dépôt" -> {
                holder.cardType.setCardBackgroundColor(Color.parseColor("#E8F5E8"))
                holder.textType.text = "Dépôt"
                holder.textMontant.text = "+$formattedMontant MGA"
                holder.textMontant.setTextColor(Color.parseColor("#388E3C"))
            }
            "Retrait" -> {
                holder.cardType.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                holder.textType.text = "Retrait"
                holder.textMontant.text = "-$formattedMontant MGA"
                holder.textMontant.setTextColor(Color.parseColor("#D32F2F"))
            }
            "Transfert" -> {
                holder.cardType.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                holder.textType.text = "Transfert"
                holder.textMontant.text = "$formattedMontant MGA"
                holder.textMontant.setTextColor(Color.parseColor("#1976D2"))
            }
            else -> {
                holder.cardType.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
                holder.textType.text = item.typeTransaction
                holder.textMontant.text = "$formattedMontant MGA"
                holder.textMontant.setTextColor(Color.parseColor("#757575"))
            }
        }

        holder.textDate.text = dateFormat.format(item.dateHeure)
        holder.textMotif.text = if (item.motif.isNullOrEmpty()) "Aucun motif" else item.motif
    }

    override fun getItemCount() = items.size

    // 🔥 Méthode de mise à jour optimisée
    fun updateList(newList: List<Historique>) {
        items = newList
        notifyDataSetChanged()
    }

    // 🔥 Obtenir la liste actuelle
    fun getCurrentList(): List<Historique> = items
}
