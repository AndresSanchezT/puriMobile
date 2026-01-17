package com.andresDev.puriapp.ui.pedidos

import android.os.Bundle
import android.text.TextWatcher
import android.util.Log
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
import com.andresDev.puriapp.data.model.PedidoListaReponse
import com.andresDev.puriapp.databinding.FragmentPedidoBinding

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
            onInfoClick = { pedidoId ->
                // Navegar al detalle
                navegarADetalle(pedidoId)
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
        observarPedidoGuardado()
        observarEntregaExitosa()
        observarEstadoCarga()

        binding.btnNuevoPedido.setOnClickListener {
                findNavController().navigate(PedidoFragmentDirections.actionPedidoFragmentToPedidoAddFragment())
        }

    }
    private fun observarEstadoCarga() {
        viewLifecycleOwner.lifecycleScope.launch {
            pedidoViewModel.loading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                if (isLoading) {
                    binding.rvPedidos.visibility = View.GONE
                    binding.emptyState.visibility = View.GONE
                }
            }
        }
    }

    // Solo recarga cuando se guardó algo
    private fun observarPedidoGuardado() {
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>("pedido_guardado")
            ?.observe(viewLifecycleOwner) { guardado ->
                if (guardado == true) {
                    Log.d("PedidoFragment", "🔄 Recargando pedidos porque se guardó uno nuevo")
                    pedidoViewModel.cargarPedidos()

                    // Limpiar flag
                    findNavController().currentBackStackEntry?.savedStateHandle
                        ?.remove<Boolean>("pedido_guardado")
                }
            }
    }

    private fun observarEntregaExitosa() {
        viewLifecycleOwner.lifecycleScope.launch {
            pedidoViewModel.pedidoEntregadoState.collect { state ->
                when (state) {
                    is EntregaState.Success -> {
                        Toast.makeText(
                            requireContext(),
                            "✅ Pedido marcado como entregado",
                            Toast.LENGTH_SHORT
                        ).show()
                        pedidoViewModel.resetearEstadoEntrega()
                    }
                    is EntregaState.Error -> {
                        Toast.makeText(
                            requireContext(),
                            "❌ Error: ${state.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        pedidoViewModel.resetearEstadoEntrega()
                    }
                    is EntregaState.Loading -> {
                        // Opcional: Puedes mostrar un ProgressBar aquí
                        Log.d("PedidoFragment", "Marcando como entregado...")
                    }
                    is EntregaState.Idle -> {
                        // Estado inicial, no hacer nada
                    }
                }
            }
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

                updateEmptyState(pedidos.isEmpty())
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        val isLoading = pedidoViewModel.loading.value

        when {
            isLoading -> {
                // Si está cargando, mostrar solo el ProgressBar
                binding.emptyState.visibility = View.GONE
                binding.rvPedidos.visibility = View.GONE
            }
            isEmpty -> {
                // Si está vacío y NO está cargando, mostrar empty state
                binding.emptyState.visibility = View.VISIBLE
                binding.rvPedidos.visibility = View.GONE
            }
            else -> {
                // Si hay datos, mostrar RecyclerView
                binding.emptyState.visibility = View.GONE
                binding.rvPedidos.visibility = View.VISIBLE
            }
        }
    }
    private fun navegarADetalle(pedidoId: Long){
        val action = PedidoFragmentDirections.actionPedidoFragmentToDetallePedidoFragment(pedidoId)
        findNavController().navigate(action)
    }

    private fun marcarComoEntregado(pedido: PedidoListaReponse) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirmar entrega")
            .setMessage("¿Deseas marcar el pedido de ${pedido.nombreCliente} como ENTREGADO?")
            .setPositiveButton("Sí, entregar") { _, _ ->
                pedido.id?.let { id ->
                    pedidoViewModel.marcarPedidoComoEntregado(id)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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