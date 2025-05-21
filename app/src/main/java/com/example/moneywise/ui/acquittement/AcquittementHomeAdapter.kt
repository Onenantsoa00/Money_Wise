package com.example.moneywise.ui.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moneywise.R
import com.example.moneywise.data.entity.Acquittement
import java.text.NumberFormat
import java.util.*

class AcquittementHomeAdapter(private var acquittements: List<Acquittement>) :
    RecyclerView.Adapter<AcquittementHomeAdapter.AcquittementViewHolder>() {

    private val format = NumberFormat.getCurrencyInstance().apply {
        maximumFractionDigits = 0
        currency = Currency.getInstance("MGA")
    }

    class AcquittementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textNom: TextView = view.findViewById(R.id.text_nom)
        val textMontant: TextView = view.findViewById(R.id.text_montant)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AcquittementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_acquittement_home, parent, false)
        return AcquittementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AcquittementViewHolder, position: Int) {
        val acquittement = acquittements[position]
        holder.textNom.text = acquittement.personne_acquittement
        holder.textMontant.text = format.format(acquittement.montant)
    }

    override fun getItemCount() = acquittements.size

    fun updateList(newList: List<Acquittement>) {
        acquittements = newList
        notifyDataSetChanged()
    }
}