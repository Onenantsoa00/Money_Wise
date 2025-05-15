package com.example.moneywise.ui.acquittement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moneywise.R
import com.example.moneywise.data.entity.Acquittement
import java.text.SimpleDateFormat
import java.util.Locale

class AcquittementAdapter(private val acquittements: List<Acquittement>) :
    RecyclerView.Adapter<AcquittementAdapter.AcquittementViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    class AcquittementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textNom: TextView = itemView.findViewById(R.id.text_nom)
        val textContact: TextView = itemView.findViewById(R.id.text_contact)
        val textMontant: TextView = itemView.findViewById(R.id.text_montant)
        val textDateCredit: TextView = itemView.findViewById(R.id.text_date_credit)
        val textDateRemise: TextView = itemView.findViewById(R.id.text_date_remise)
        val initials: TextView = itemView.findViewById(R.id.initials)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AcquittementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_acquittement, parent, false)
        return AcquittementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AcquittementViewHolder, position: Int) {
        val acquittement = acquittements[position]

        // Extraire les initiales avec gestion des cas vides
        val initials = try {
            acquittement.personne_acquittement.split(" ").take(2)
                .filter { it.isNotEmpty() }
                .joinToString("") { it.first().toString().uppercase() }
        } catch (e: Exception) {
            "?"
        }

        holder.initials.text = initials
        holder.textNom.text = acquittement.personne_acquittement
        holder.textContact.text = acquittement.contacte
        holder.textMontant.text = "${acquittement.montant} MGA"
        holder.textDateCredit.text = dateFormat.format(acquittement.date_crédit)
        holder.textDateRemise.text = dateFormat.format(acquittement.date_remise_crédit)
    }

    override fun getItemCount() = acquittements.size
}