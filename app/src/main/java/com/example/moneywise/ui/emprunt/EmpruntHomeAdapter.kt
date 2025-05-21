package com.example.moneywise.ui.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moneywise.R
import com.example.moneywise.data.entity.Emprunt
import java.text.NumberFormat
import java.util.*

class EmpruntHomeAdapter(private var emprunts: List<Emprunt>) :
    RecyclerView.Adapter<EmpruntHomeAdapter.EmpruntViewHolder>() {

    private val format = NumberFormat.getCurrencyInstance().apply {
        maximumFractionDigits = 0
        currency = Currency.getInstance("MGA")
    }

    class EmpruntViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textNom: TextView = view.findViewById(R.id.text_nom)
        val textMontant: TextView = view.findViewById(R.id.text_montant)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmpruntViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emprunt_home, parent, false)
        return EmpruntViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmpruntViewHolder, position: Int) {
        val emprunt = emprunts[position]
        holder.textNom.text = emprunt.nom_emprunte
        holder.textMontant.text = format.format(emprunt.montant)
    }

    override fun getItemCount() = emprunts.size

    fun updateList(newList: List<Emprunt>) {
        emprunts = newList
        notifyDataSetChanged()
    }
}