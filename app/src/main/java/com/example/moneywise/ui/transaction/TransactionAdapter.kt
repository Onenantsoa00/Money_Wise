package com.example.moneywise.ui.transaction

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.moneywise.R
import com.example.moneywise.data.entity.Transaction
import com.example.moneywise.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var transactions = listOf<Transaction>()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun submitList(newList: List<Transaction>) {
        transactions = newList
        notifyDataSetChanged()
    }

    // Mettre à jour la liste (pour compatibilité avec le filtrage)
    fun updateList(newList: List<Transaction>) {
        transactions = newList
        notifyDataSetChanged()
    }

    // Obtenir la liste actuelle
    fun getCurrentList(): List<Transaction> = transactions

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount() = transactions.size

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            with(binding) {
                when (transaction.type) {
                    "Dépôt" -> {
                        iconCard.setCardBackgroundColor(root.context.getColor(R.color.deposit_bg))
                        iconImage.setImageResource(R.drawable.ic_depot)
                        iconImage.setColorFilter(root.context.getColor(R.color.deposit))
                        amountText.setTextColor(root.context.getColor(R.color.deposit))
                    }
                    "Retrait" -> {
                        iconCard.setCardBackgroundColor(root.context.getColor(R.color.withdrawal_bg))
                        iconImage.setImageResource(R.drawable.ic_retrait)
                        iconImage.setColorFilter(root.context.getColor(R.color.withdrawal))
                        amountText.setTextColor(root.context.getColor(R.color.withdrawal))
                    }
                    else -> {
                        iconCard.setCardBackgroundColor(root.context.getColor(R.color.transfer_bg))
                        iconImage.setImageResource(R.drawable.ic_transfer)
                        iconImage.setColorFilter(root.context.getColor(R.color.transfer))
                        amountText.setTextColor(root.context.getColor(R.color.transfer))
                    }
                }

                typeText.text = transaction.type
                dateText.text = dateFormat.format(transaction.date)
                amountText.text = when (transaction.type) {
                    "Dépôt" -> "+${transaction.montants} MGA"
                    else -> "-${transaction.montants} MGA"
                }
            }
        }
    }
}
