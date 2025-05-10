package com.example.moneywise.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.moneywise.databinding.FragmentTransactionBinding

class TransactionFragment : Fragment() {

    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TransactionViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(TransactionViewModel::class.java)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.fabAddTransaction.setOnClickListener {
            // Action pour ajouter une nouvelle transaction
            // (À implémenter selon vos besoins)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}