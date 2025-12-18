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
import com.andresDev.puriapp.data.model.Pedido
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

        binding.btnNuevoPedido.setOnClickListener {
                findNavController().navigate(PedidoFragmentDirections.actionPedidoFragmentToPedidoAddFragment())
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

    private fun navegarADetalle(pedidoId: Long){
        val action = PedidoFragmentDirections.actionPedidoFragmentToDetallePedidoFragment(pedidoId)
        findNavController().navigate(action)
    }

    private fun marcarComoEntregado(pedidoListaReponse: PedidoListaReponse) {
        Toast.makeText(
            requireContext(),
            "Cliente ${pedidoListaReponse.nombreCliente} marcado como entregado",
            Toast.LENGTH_SHORT
        ).show()

        // Aquí puedes agregar lógica para:
        // - Crear una visita
        // - Actualizar el estado del cliente
        // - Navegar a un formulario de visita
    }

    private fun verDetallePedido(pedidoId: Long) {


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