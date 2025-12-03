package com.andresDev.puriapp.ui.pedidos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.andresDev.puriapp.data.model.PedidoDetallesGeneralesResponse
import com.andresDev.puriapp.databinding.FragmentDetallePedidoBinding
import com.andresDev.puriapp.ui.pedidos.adapter.DetallePedidoAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DetallePedidoFragment : Fragment() {

    private var _binding: FragmentDetallePedidoBinding? = null
    private val binding get() = _binding!!

    private val detallePedidoViewModel: DetallePedidoViewModel by viewModels()
    private val args: DetallePedidoFragmentArgs by navArgs()

    private val detallePedidoAdapter by lazy {
        DetallePedidoAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        detallePedidoViewModel.cargarPedido(args.pedidoId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetallePedidoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvProductos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = detallePedidoAdapter
        }
    }

    private fun setupListeners() {
        binding.btnCerrar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnGuardar.setOnClickListener {
            // Guardar cambios
        }

        binding.btnAgregarProducto.setOnClickListener {
            // Agregar producto
        }

        // Botón reintentar en caso de error
        binding.btnReintentar.setOnClickListener {
            detallePedidoViewModel.cargarPedido(args.pedidoId)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                detallePedidoViewModel.uiState.collect { state ->
                    when (state) {
                        is DetallePedidoUiState.Loading -> {
                            mostrarCargando()
                        }
                        is DetallePedidoUiState.Success -> {
                            mostrarContenido()
                            mostrarDatosPedido(state.pedido)
                        }
                        is DetallePedidoUiState.Error -> {
                            mostrarError(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun mostrarCargando() {
        binding.apply {
            loadingLayout.visibility = View.VISIBLE
            contentLayout.visibility = View.GONE
            errorLayout.visibility = View.GONE
        }
    }

    private fun mostrarContenido() {
        binding.apply {
            loadingLayout.visibility = View.GONE
            contentLayout.visibility = View.VISIBLE
            errorLayout.visibility = View.GONE
        }
    }

    private fun mostrarError(mensaje: String) {
        binding.apply {
            loadingLayout.visibility = View.GONE
            contentLayout.visibility = View.GONE
            errorLayout.visibility = View.VISIBLE
            tvErrorMessage.text = mensaje
        }
    }

    private fun mostrarDatosPedido(pedido: PedidoDetallesGeneralesResponse) {
        binding.apply {
            // Información del cliente
            tvNombreCliente.text = pedido.cliente?.nombre ?: "Sin nombre"
            tvDireccion.text = pedido.cliente?.direccion ?: "Sin dirección"
            tvTelefono.text = pedido.cliente?.telefono ?: "Sin teléfono"

            // Información del pedido
            tvVendedor.text = pedido.vendedor?.nombreVendedor ?: "Sin vendedor"
            tvEstado.text = pedido.estado ?: "Sin estado"
            tvObservaciones.text = pedido.observaciones ?: "Sin observaciones"

            // Totales
            tvSubtotal.text = String.format("S/ %.2f", pedido.subtotal ?: 0.0)
            tvIgv.text = String.format("S/ %.2f", pedido.igv ?: 0.0)
            tvTotal.text = String.format("S/ %.2f", pedido.total ?: 0.0)

            // Lista de productos
            detallePedidoAdapter.submitList(pedido.detallePedidos)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PEDIDO_ID = "pedido_id"

        fun newInstance(pedidoId: Long): DetallePedidoFragment {
            return DetallePedidoFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PEDIDO_ID, pedidoId)
                }
            }
        }
    }
}