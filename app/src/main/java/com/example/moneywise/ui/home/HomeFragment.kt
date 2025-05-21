package com.example.moneywise.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.databinding.FragmentHomeBinding
import com.example.moneywise.ui.home.adapters.AcquittementHomeAdapter
import com.example.moneywise.ui.home.adapters.EmpruntHomeAdapter
import com.example.moneywise.ui.home.adapters.ProjetHomeAdapter
import com.example.moneywise.ui.transaction.TransactionHomeAdapter
import java.text.NumberFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var empruntAdapter: EmpruntHomeAdapter
    private lateinit var acquittementAdapter: AcquittementHomeAdapter
    private lateinit var projetAdapter: ProjetHomeAdapter
    private lateinit var transactionAdapter: TransactionHomeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        viewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(database)
        ).get(HomeViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupAdapters()
        setupObservers()
        viewModel.refreshData()
    }

    private fun setupAdapters() {
        // Adapter pour les emprunts
        empruntAdapter = EmpruntHomeAdapter(emptyList())
        binding.recyclerEmprunts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = empruntAdapter
            setHasFixedSize(true)
        }

        // Adapter pour les acquittements
        acquittementAdapter = AcquittementHomeAdapter(emptyList())
        binding.recyclerAcquittements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = acquittementAdapter
            setHasFixedSize(true)
        }

        // Adapter pour les projets
        projetAdapter = ProjetHomeAdapter(emptyList())
        binding.recyclerProjectsHome.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = projetAdapter
            setHasFixedSize(true)
        }

        // Adapter pour les transactions
        transactionAdapter = TransactionHomeAdapter(emptyList())
        binding.recyclerTransactionsHome.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
            setHasFixedSize(true)
        }

        // Masquer l'ancien HorizontalScrollView statique
        binding.horizontalScrollViewProjects.visibility = View.GONE
    }

    private fun setupObservers() {
        // Formatage de la devise
        val format = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 0
        }

        // Observateur pour le solde utilisateur
        viewModel.soldeUtilisateur.observe(viewLifecycleOwner) { solde ->
            solde?.let {
                binding.textSolde.text = "${format.format(it)} MGA"
            }
        }

        // Observateur pour le nom utilisateur
        viewModel.nomUtilisateur.observe(viewLifecycleOwner) { nom ->
            nom?.let {
                binding.textNomUtilisateurText.text = "Bonjour $it"
            }
        }

        // Observateur pour les emprunts récents
        viewModel.empruntsRecents.observe(viewLifecycleOwner) { emprunts ->
            emprunts?.let {
                empruntAdapter.updateList(it)
                binding.recyclerEmprunts.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        // Observateur pour les acquittements récents
        viewModel.acquittementsRecents.observe(viewLifecycleOwner) { acquittements ->
            acquittements?.let {
                acquittementAdapter.updateList(it)
                binding.recyclerAcquittements.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        // Observateur pour les projets récents
        viewModel.projetsRecents.observe(viewLifecycleOwner) { projets ->
            projets?.let {
                projetAdapter.updateList(it)
                binding.recyclerProjectsHome.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        // Observateur pour les transactions récentes
        viewModel.transactionsRecentes.observe(viewLifecycleOwner) { transactions ->
            transactions?.let {
                transactionAdapter.updateList(it)
                binding.recyclerTransactionsHome.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class HomeViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}