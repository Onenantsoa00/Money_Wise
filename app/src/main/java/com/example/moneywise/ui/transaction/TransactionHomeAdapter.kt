package com.example.moneywise.ui.transaction

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moneywise.R
import com.example.moneywise.data.entity.Transaction
import java.text.SimpleDateFormat
import java.util.*

class TransactionHomeAdapter(private var transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionHomeAdapter.TransactionViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeText: TextView = itemView.findViewById(R.id.textTransactionType)
        val amountText: TextView = itemView.findViewById(R.id.textTransactionAmount)
        val dateText: TextView = itemView.findViewById(R.id.textTransactionDate)
        val motifText: TextView = itemView.findViewById(R.id.textTransactionMotif)
        val descriptionText: TextView = itemView.findViewById(R.id.textTransactionDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_home, parent, false)
        return TransactionViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        holder.typeText.text = transaction.type
        holder.amountText.text = transaction.montants
        holder.dateText.text = dateFormat.format(transaction.date)
        holder.motifText.text = "-" // Vous pouvez ajouter un champ motif dans votre entité Transaction si nécessaire
        holder.descriptionText.text = "-" // Idem pour la description

        // Changer la couleur du montant selon le type
        val colorRes = when (transaction.type) {
            "Dépôt" -> R.color.green
            else -> R.color.red
        }
        holder.amountText.setTextColor(holder.itemView.context.getColor(colorRes))
    }

    override fun getItemCount() = transactions.size

    fun updateList(newList: List<Transaction>) {
        transactions = newList
        notifyDataSetChanged()
    }
}