package com.example.moneywise.ui.acquittement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.moneywise.databinding.FragmentAcquittementBinding

class AcquittementFragment : Fragment() {

    private var _binding: FragmentAcquittementBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AcquittementViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAcquittementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(AcquittementViewModel::class.java)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.fabAddAcquittement.setOnClickListener {
            // Action pour ajouter un nouvel acquittement
            // (À implémenter selon vos besoins)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}