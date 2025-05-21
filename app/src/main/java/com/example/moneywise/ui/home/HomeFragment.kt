package com.example.moneywise.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.databinding.FragmentHomeBinding
import com.example.moneywise.ui.home.adapters.AcquittementHomeAdapter
import com.example.moneywise.ui.home.adapters.EmpruntHomeAdapter
import java.text.NumberFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var empruntAdapter: EmpruntHomeAdapter
    private lateinit var acquittementAdapter: AcquittementHomeAdapter

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

        // Initialisation de la base de données et du ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        viewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(database)
        ).get(HomeViewModel::class.java)

        // Liaison du ViewModel avec le binding
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // Initialisation de l'UI
        initUI()
        setupAdapters()
        setupObservers()

        // Rafraîchir les données
        viewModel.refreshData()
    }

    private fun initUI() {
        // Formatage de la devise
        val format = NumberFormat.getCurrencyInstance()
        format.maximumFractionDigits = 0
        format.currency = Currency.getInstance("MGA")

        // Observer le solde de l'utilisateur
        viewModel.soldeUtilisateur.observe(viewLifecycleOwner) { solde ->
            solde?.let {
                binding.textSolde.text = format.format(it)
            }
        }

        // Observer le nom de l'utilisateur
        viewModel.getNomUtilisateur().observe(viewLifecycleOwner) { nom ->
            nom?.let {
                binding.textNomUtilisateur.findViewById<TextView>(R.id.textNomUtilisateurText).text = "Bonjour $nom"
            }
        }
    }

    private fun setupAdapters() {
        // Adapter pour les emprunts
        empruntAdapter = EmpruntHomeAdapter(emptyList())
        binding.recyclerEmprunts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = empruntAdapter
        }

        // Adapter pour les acquittements
        acquittementAdapter = AcquittementHomeAdapter(emptyList())
        binding.recyclerAcquittements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = acquittementAdapter
        }
    }

    private fun setupObservers() {
        // Observer les emprunts non remboursés
        viewModel.empruntsNonRembourses.observe(viewLifecycleOwner) { emprunts ->
            emprunts?.let {
                empruntAdapter.updateList(it)
            }
        }

        // Observer les acquittements récents
        viewModel.acquittementsRecents.observe(viewLifecycleOwner) { acquittements ->
            acquittements?.let {
                acquittementAdapter.updateList(it)
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