package com.example.moneywise.ui.historique

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.moneywise.databinding.FragmentHistoriqueBinding

class HistoriqueFragment : Fragment() {

    private var _binding: FragmentHistoriqueBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HistoriqueViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoriqueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(HistoriqueViewModel::class.java)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.fabScrollTop.setOnClickListener {
            // Faire défiler vers le haut
            binding.nestedScrollView.smoothScrollTo(0, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}