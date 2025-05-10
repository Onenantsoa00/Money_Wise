package com.example.moneywise.ui.emprunt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.moneywise.databinding.FragmentEmpruntBinding

class EmpruntFragment : Fragment() {

    private var _binding: FragmentEmpruntBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: EmpruntViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmpruntBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(EmpruntViewModel::class.java)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.fabAddEmprunt.setOnClickListener {
            // Action pour ajouter un nouvel emprunt
            // (À implémenter selon vos besoins)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}