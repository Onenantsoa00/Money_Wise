package com.example.moneywise.ui.historique

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.databinding.FragmentHistoriqueBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

class HistoriqueFragment : Fragment() {

    private var _binding: FragmentHistoriqueBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HistoriqueViewModel
    private lateinit var adapter: HistoriqueAdapter

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

        val database = AppDatabase.getDatabase(requireContext())
        viewModel = ViewModelProvider(this, HistoriqueViewModelFactory(database))
            .get(HistoriqueViewModel::class.java)

        setupRecyclerView()
        setupObservers()
        loadResumeData()

        // Bouton pour remonter en haut
        binding.fabScrollTop.setOnClickListener {
            binding.nestedScrollView.smoothScrollTo(0, 0)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerHistorique.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = HistoriqueAdapter(emptyList()).also {
                this@HistoriqueFragment.adapter = it
            }
        }
    }

    private fun setupObservers() {
        viewModel.allHistorique.observe(viewLifecycleOwner) { historiques ->
            adapter.updateList(historiques)
        }
    }

    private fun loadResumeData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (credits, debits, solde) = viewModel.getResumeHistorique()
                withContext(Dispatchers.Main) {
                    val formatter = NumberFormat.getInstance(Locale.getDefault())
                    binding.textCredit.text = "+${formatter.format(credits)}"
                    binding.textDebit.text = "-${formatter.format(debits)}"
                    binding.textSolde.text = if (solde >= 0) "+${formatter.format(solde)}" else formatter.format(solde)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.textCredit.text = "+0"
                    binding.textDebit.text = "-0"
                    binding.textSolde.text = "0"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}