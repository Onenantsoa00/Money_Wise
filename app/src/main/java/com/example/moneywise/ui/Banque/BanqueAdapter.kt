package com.example.moneywise.ui.Banque

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.moneywise.data.entity.Banque
import com.example.moneywise.databinding.ItemBanqueBinding

class BanqueAdapter(
    private val onDeleteClick: (Banque) -> Unit,
    private val onUpdateClick: (Banque) -> Unit
) : ListAdapter<Banque, BanqueAdapter.BanqueViewHolder>(DiffCallback()) {

    class BanqueViewHolder(
        private val binding: ItemBanqueBinding,
        private val onDeleteClick: (Banque) -> Unit,
        private val onUpdateClick: (Banque) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(banque: Banque) {
            binding.banqueName.text = banque.nom

            binding.deleteButton.setOnClickListener {
                onDeleteClick(banque)
            }

            binding.updateButton.setOnClickListener {
                onUpdateClick(banque)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Banque>() {
        override fun areItemsTheSame(oldItem: Banque, newItem: Banque): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Banque, newItem: Banque): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BanqueViewHolder {
        val binding = ItemBanqueBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BanqueViewHolder(binding, onDeleteClick, onUpdateClick)
    }

    override fun onBindViewHolder(holder: BanqueViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}