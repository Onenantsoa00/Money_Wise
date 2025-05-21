package com.example.moneywise.ui.historique

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moneywise.R
import com.example.moneywise.data.entity.Historique
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class HistoriqueAdapter(private var items: List<Historique>) :
    RecyclerView.Adapter<HistoriqueAdapter.HistoriqueViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    class HistoriqueViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardType: MaterialCardView = view.findViewById(R.id.card_type)
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

        when (item.typeTransaction) {
            "ACQUITTEMENT" -> {
                holder.cardType.setCardBackgroundColor(holder.itemView.context.getColor(R.color.green_light))
                holder.textType.text = "Acquittement"
                holder.textMontant.text = "-${item.montant}"
                holder.textMontant.setTextColor(holder.itemView.context.getColor(R.color.red))
            }
            "EMPRUNT" -> {
                holder.cardType.setCardBackgroundColor(holder.itemView.context.getColor(R.color.blue_light))
                holder.textType.text = "Emprunt"
                holder.textMontant.text = "+${item.montant}"
                holder.textMontant.setTextColor(holder.itemView.context.getColor(R.color.green))
            }
            "REMBOURSEMENT_ACQUITTEMENT" -> {
                holder.cardType.setCardBackgroundColor(holder.itemView.context.getColor(R.color.green_light))
                holder.textType.text = "Remb. Acquitt."
                holder.textMontant.text = "+${item.montant}"
                holder.textMontant.setTextColor(holder.itemView.context.getColor(R.color.green))
            }
            "REMBOURSEMENT_EMPRUNT" -> {
                holder.cardType.setCardBackgroundColor(holder.itemView.context.getColor(R.color.blue_light))
                holder.textType.text = "Remb. Emprunt"
                holder.textMontant.text = "-${item.montant}"
                holder.textMontant.setTextColor(holder.itemView.context.getColor(R.color.red))
            }
        }

        holder.textDate.text = dateFormat.format(item.dateHeure)
        holder.textMotif.text = item.motif
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<Historique>) {
        items = newList
        notifyDataSetChanged()
    }
}