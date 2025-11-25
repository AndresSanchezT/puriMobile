package com.andresDev.puriapp.ui.clientes
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController

import androidx.recyclerview.widget.LinearLayoutManager

import com.andresDev.puriapp.databinding.FragmentClientesBinding
import com.andresDev.puriapp.ui.clientes.adapter.ClienteAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class ClientesFragment : Fragment() {

    private val clienteViewModel by viewModels<ClienteViewModel>()
    private var _binding: FragmentClientesBinding? = null
    private val binding get() = _binding!!

    private lateinit var clienteAdapter: ClienteAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = FragmentClientesBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initList()
        observeUI()
        initSearch()
        binding.btnNuevoCliente.setOnClickListener {
            findNavController().navigate(
                ClientesFragmentDirections.actionClientesFragmentToClienteAddFragment()
            )
        }
    }

    private fun initList() {
        clienteAdapter = ClienteAdapter()
        binding.rvClientes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = clienteAdapter
        }
    }

    private fun observeUI() {
        viewLifecycleOwner.lifecycleScope.launch {
            clienteViewModel.clientes.collect { lista ->
                clienteAdapter.submitList(lista)
            }
        }

//        viewLifecycleOwner.lifecycleScope.launch {
//            clienteViewModel.loading.collect { isLoading ->
//                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
//            }
//        }
    }

    private fun initSearch() {
        binding.etBuscarCliente.addTextChangedListener { text ->
            clienteViewModel.filtrarClientes(text.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}