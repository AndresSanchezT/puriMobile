package com.andresDev.puriapp.ui.visitas

import android.os.Bundle
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.andresDev.puriapp.data.model.Visita
import com.andresDev.puriapp.databinding.FragmentVisitasBinding
import com.andresDev.puriapp.ui.visitas.adapter.VisitaAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VisitasFragment : Fragment() {

    private var _binding: FragmentVisitasBinding? = null
    private val binding get() = _binding!!

    private val visitaViewModel: VisitaViewModel by viewModels()

    private var textWatcher: TextWatcher? = null

    private val visitaAdapter by lazy {
        VisitaAdapter(
            onCheckClick = { pedido ->
                // Acción al hacer click en el check verde
                marcarComoVisitado(pedido)
            },
            onInfoClick = { pedido ->
                // Acción al hacer click en info
                verDetalleVisita(pedido)
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentVisitasBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSearch()
        setupRecyclerView()
        observePedidos()
    }

    private fun setupRecyclerView() {
        binding.rvVisitas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = visitaAdapter
        }
    }

    private fun observePedidos() {
        viewLifecycleOwner.lifecycleScope.launch {
            visitaViewModel.visitas.collect { visitas ->
                visitaAdapter.submitList(visitas)
            }
        }
    }

    private fun marcarComoVisitado(visita: Visita) {
        Toast.makeText(
            requireContext(),
            "Cliente ${visita.cliente.nombreContacto} marcado como visitado",
            Toast.LENGTH_SHORT
        ).show()

        // Aquí puedes agregar lógica para:
        // - Crear una visita
        // - Actualizar el estado del cliente
        // - Navegar a un formulario de visita
    }

    private fun verDetalleVisita(visita: Visita) {
        Toast.makeText(
            requireContext(),
            "Ver detalles de ${visita.cliente.nombreContacto}",
            Toast.LENGTH_SHORT
        ).show()

        // Aquí puedes:
        // - Navegar a ClienteDetailFragment
        // - Mostrar un diálogo con más información
        // - Navegar a una pantalla de edición

        // Ejemplo con Navigation:
        // val action = ClienteListFragmentDirections
        //     .actionClienteListToClienteDetail(cliente.id)
        // findNavController().navigate(action)
    }

    private fun initSearch() {
        textWatcher = binding.etBuscarVisita.addTextChangedListener { text ->
            visitaViewModel.filtrarVisita(text.toString())
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        // Remover el listener antes de liberar el binding
        textWatcher?.let { binding.etBuscarVisita.removeTextChangedListener(it) }
        textWatcher = null
        _binding = null
    }

}