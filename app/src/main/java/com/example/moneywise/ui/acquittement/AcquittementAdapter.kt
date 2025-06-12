package com.example.moneywise.ui.acquittement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moneywise.R
import com.example.moneywise.data.entity.Acquittement
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AcquittementAdapter(
    private var acquittements: List<Acquittement>,
    private val onRembourserClick: (Acquittement) -> Unit
) : RecyclerView.Adapter<AcquittementAdapter.AcquittementViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val numberFormat = NumberFormat.getInstance(Locale.getDefault())

    class AcquittementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textNom: TextView = view.findViewById(R.id.text_nom)
        val textContact: TextView = view.findViewById(R.id.text_contact)
        val textMontant: TextView = view.findViewById(R.id.text_montant)
        val textDateCredit: TextView = view.findViewById(R.id.text_date_credit)
        val textDateRemise: TextView = view.findViewById(R.id.text_date_remise)
        val initials: TextView = view.findViewById(R.id.initials)
        val btnRembourser: MaterialButton = view.findViewById(R.id.btn_rembourser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AcquittementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_acquittement, parent, false)
        return AcquittementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AcquittementViewHolder, position: Int) {
        val acquittement = acquittements[position]

        holder.textNom.text = acquittement.personne_acquittement
        holder.textContact.text = acquittement.contacte.ifEmpty { "Aucun contact" }
        holder.textMontant.text = "${numberFormat.format(acquittement.montant)} MGA"
        holder.textDateCredit.text = dateFormat.format(acquittement.date_crédit)
        holder.textDateRemise.text = dateFormat.format(acquittement.date_remise_crédit)

        // Extraire les initiales
        val initials = try {
            acquittement.personne_acquittement.split(" ").take(2)
                .filter { it.isNotEmpty() }
                .joinToString("") { it.first().toString().uppercase() }
        } catch (e: Exception) {
            "?"
        }
        holder.initials.text = initials

        holder.btnRembourser.setOnClickListener {
            onRembourserClick(acquittement)
        }
    }

    override fun getItemCount() = acquittements.size

    // Mettre à jour la liste
    fun updateList(newList: List<Acquittement>) {
        acquittements = newList
        notifyDataSetChanged()
    }

    // Obtenir la liste actuelle
    fun getCurrentList(): List<Acquittement> = acquittements
}
