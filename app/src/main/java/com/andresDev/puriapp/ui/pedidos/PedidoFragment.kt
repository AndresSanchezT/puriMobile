package com.andresDev.puriapp.ui.pedidos

import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.databinding.FragmentPedidoBinding
import com.andresDev.puriapp.ui.clientes.ClientesFragmentDirections
import com.andresDev.puriapp.ui.pedidos.adapter.PedidoAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PedidoFragment : Fragment() {

    private var _binding: FragmentPedidoBinding? = null
    private val binding get() = _binding!!

    private val pedidoViewModel: PedidoViewModel by viewModels()

    private var textWatcher: TextWatcher? = null

    private val pedidoAdapter by lazy {
        PedidoAdapter(
            onCheckClick = { pedido ->
                // Acción al hacer click en el check verde
                marcarComoEntregado(pedido)
            },
            onInfoClick = { pedido ->
                // Acción al hacer click en info
                verDetallePedido(pedido)
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPedidoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initSearch()
        setupRecyclerView()
        observePedidos()

        binding.btnNuevoPedido.setOnClickListener {
                findNavController().navigate(PedidoFragmentDirections.actionPedidoFragmentToPedidoAddFragment())
        }
    }

    private fun setupRecyclerView() {
        binding.rvPedidos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pedidoAdapter
        }
    }

    private fun observePedidos() {
        viewLifecycleOwner.lifecycleScope.launch {
            pedidoViewModel.pedidos.collect { pedidos ->
                pedidoAdapter.submitList(pedidos)
            }
        }
    }

    private fun marcarComoEntregado(pedido: Pedido) {
        Toast.makeText(
            requireContext(),
            "Cliente ${pedido.cliente.nombreContacto} marcado como entregado",
            Toast.LENGTH_SHORT
        ).show()

        // Aquí puedes agregar lógica para:
        // - Crear una visita
        // - Actualizar el estado del cliente
        // - Navegar a un formulario de visita
    }

    private fun verDetallePedido(pedido: Pedido) {
        Toast.makeText(
            requireContext(),
            "Ver detalles de ${pedido.cliente.nombreContacto}",
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
        textWatcher = binding.etBuscarPedido.addTextChangedListener { text ->
            pedidoViewModel.filtrarPedidos(text.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remover el listener antes de liberar el binding
        textWatcher?.let { binding.etBuscarPedido.removeTextChangedListener(it) }
        textWatcher = null
        _binding = null
    }
}