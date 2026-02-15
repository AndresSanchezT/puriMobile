package com.andresDev.puriapp.ui.pedidos

import android.R.attr.backgroundTint
import android.os.Bundle
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.andresDev.puriapp.R
import com.andresDev.puriapp.data.model.PedidoListaReponse
import com.andresDev.puriapp.databinding.FragmentPedidoBinding
import com.andresDev.puriapp.ui.pedidos.adapter.DragDropCallback
import com.andresDev.puriapp.ui.pedidos.adapter.PedidoAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PedidoFragment : Fragment() {

    private var _binding: FragmentPedidoBinding? = null
    private val binding get() = _binding!!

    private val pedidoViewModel: PedidoViewModel by viewModels()

    private var textWatcher: TextWatcher? = null
    private lateinit var itemTouchHelper: ItemTouchHelper

    // ✅ NUEVO: Estado del modo de edición
    private var isEditingWithNumbers = false

    private val pedidoAdapter by lazy {
        PedidoAdapter(
            isAdminMode = pedidoViewModel.isAdmin,
            onCheckClick = { pedido -> marcarComoEntregado(pedido) },
            onInfoClick = { pedidoId -> navegarADetalle(pedidoId) },
            onDeleteClick = { pedido -> confirmarEliminacion(pedido) },
            onOrderChanged = {
                mostrarBotonGuardarOrden()
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

        Log.d("PedidoFragment", "🔑 Modo Admin: ${pedidoViewModel.isAdmin}")

        setupSpinner()
        initSearch()
        setupRecyclerView()
        setupDragAndDrop()
        setupBotonGuardarOrden()
        setupBotonEditarOrden()
        observarPedidoGuardado()
        observarEntregaExitosa()
        observarEstadoCarga()
        observarEliminacionExitosa()
        observarGuardadoOrden()

        binding.btnNuevoPedido.setOnClickListener {
            findNavController().navigate(
                PedidoFragmentDirections.actionPedidoFragmentToPedidoAddFragment()
            )
        }

        // ✅ Mostrar botón de editar si es admin
        if (pedidoViewModel.isAdmin) {
            binding.layoutBotonesAccion.visibility = View.VISIBLE
        }
    }

    // ✅ NUEVO: Configurar botón de editar orden
    private fun setupBotonEditarOrden() {
        binding.btnEditarOrden.setOnClickListener {
            if (isEditingWithNumbers) {
                // Cancelar modo edición
                cancelarEdicionNumerica()
            } else {
                // Activar modo edición
                activarEdicionNumerica()
            }
        }
    }

    private fun activarEdicionNumerica() {
        isEditingWithNumbers = true
        pedidoAdapter.enableNumericEditMode()

        // Deshabilitar drag & drop
        itemTouchHelper.attachToRecyclerView(null)

        // Cambiar texto del botón
        binding.btnEditarOrden.apply {
            text = "❌ Cancelar"
            setBackgroundColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            )
        }

        // Mostrar instrucciones
        Toast.makeText(
            requireContext(),
            "Asigna números a los pedidos para reordenar\nEjemplo: #1, #2, #3...",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun cancelarEdicionNumerica() {
        isEditingWithNumbers = false
        pedidoAdapter.disableNumericEditMode()
        pedidoAdapter.resetPendingChanges()

        // Re-habilitar drag & drop
        itemTouchHelper.attachToRecyclerView(binding.rvPedidos)

        // Restaurar botón
        binding.btnEditarOrden.apply {
            text = "✏️ Editar Orden"
            setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.primaryDark)
            )
        }

        // Ocultar botón guardar
        ocultarBotonGuardarOrden()
    }

    // ✅ NUEVO: Configurar botón de guardar orden
    private fun setupBotonGuardarOrden() {
        binding.btnGuardarOrden.setOnClickListener {
            confirmarGuardarOrden()
        }
    }


    // ✅ NUEVO: Mostrar el botón cuando hay cambios
    private fun mostrarBotonGuardarOrden() {
        binding.btnGuardarOrden.visibility = View.VISIBLE

        // Animación de entrada
        binding.btnGuardarOrden.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .start()
        }
    }

    // ✅ NUEVO: Ocultar el botón
    private fun ocultarBotonGuardarOrden() {
        binding.btnGuardarOrden.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(200)
            .withEndAction {
                binding.btnGuardarOrden.visibility = View.GONE
            }
            .start()
    }

    // ✅ NUEVO: Confirmar antes de guardar
    private fun confirmarGuardarOrden() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("💾 Guardar Orden")
            .setMessage("¿Deseas guardar el nuevo orden de los pedidos?\n\nEste cambio será visible para todos los usuarios.")
            .setPositiveButton("Sí, guardar") { _, _ ->
                guardarNuevoOrden()
            }
            .setNegativeButton("Cancelar", null)
            .setIcon(android.R.drawable.ic_menu_save)
            .show()
    }

    // ✅ NUEVO: Guardar el orden en el servidor
    private fun guardarNuevoOrden() {
        val updatedList = pedidoAdapter.getUpdatedList()
        pedidoViewModel.actualizarOrdenPedidos(updatedList)

        // Si estaba en modo edición numérica, salir
        if (isEditingWithNumbers) {
            cancelarEdicionNumerica()
        }
    }

    // ✅ NUEVO: Observar cuando se guarda el orden
    private fun observarGuardadoOrden() {
        viewLifecycleOwner.lifecycleScope.launch {
            pedidoViewModel.ordenGuardadoState.collect { state ->
                when (state) {
                    is OrdenGuardadoState.Success -> {
                        Log.d("PedidoFragment", "✅ Orden guardado exitosamente")
                        Toast.makeText(
                            requireContext(),
                            "✅ Orden guardado exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()


                        pedidoAdapter.resetPendingChanges()
                        ocultarBotonGuardarOrden()
                        pedidoViewModel.resetearEstadoOrden()

                        // ✅ OPCIONAL: Recargar para sincronizar con el servidor
                        // val posicionActual = binding.spinnerFechaPedidos.selectedItemPosition
                        // when (posicionActual) {
                        //     0 -> pedidoViewModel.cargarPedidosHoy()
                        //     1 -> pedidoViewModel.cargarPedidosManana()
                        //     2 -> pedidoViewModel.cargarPedidosPasadoManana()
                        // }
                    }
                    is OrdenGuardadoState.Error -> {
                        Log.e("PedidoFragment", "❌ Error: ${state.message}")
                        Toast.makeText(
                            requireContext(),
                            "❌ Error al guardar: ${state.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        pedidoViewModel.resetearEstadoOrden()
                    }
                    is OrdenGuardadoState.Loading -> {
                        Log.d("PedidoFragment", "⏳ Guardando orden...")
                        // Opcional: Deshabilitar botón mientras guarda
                        binding.btnGuardarOrden.isEnabled = false
                        binding.btnGuardarOrden.text = "Guardando..."
                    }
                    is OrdenGuardadoState.Idle -> {
                        binding.btnGuardarOrden.isEnabled = true
                        binding.btnGuardarOrden.text = "💾 Guardar Nuevo Orden"
                    }
                }
            }
        }
    }

    private fun setupDragAndDrop() {
        val callback = DragDropCallback(
            pedidoAdapter,
            binding.root
        )
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.rvPedidos)
    }
    private fun confirmarEliminacion(pedido: PedidoListaReponse) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Confirmar eliminación")
            .setMessage("¿Estás seguro de eliminar el pedido de ${pedido.nombreCliente}?\n\nEsta acción NO se puede deshacer.")
            .setPositiveButton("Sí, eliminar") { _, _ ->
                pedido.id?.let { id ->
                    pedidoViewModel.eliminarPedido(id)
                }
            }
            .setNegativeButton("Cancelar", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    // ✅ NUEVO: Observar estado de eliminación
    private fun observarEliminacionExitosa() {
        viewLifecycleOwner.lifecycleScope.launch {
            pedidoViewModel.pedidoEliminadoState.collect { state ->
                when (state) {
                    is EliminacionState.Success -> {
                        Log.d("PedidoFragment", "✅ Pedido eliminado - UI actualizada")
                        Toast.makeText(
                            requireContext(),
                            "✅ Pedido eliminado exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        pedidoViewModel.resetearEstadoEliminacion()
                    }
                    is EliminacionState.Error -> {
                        Log.e("PedidoFragment", "❌ Error: ${state.message}")
                        Toast.makeText(
                            requireContext(),
                            "❌ Error: ${state.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        pedidoViewModel.resetearEstadoEliminacion()
                    }
                    is EliminacionState.Loading -> {
                        Log.d("PedidoFragment", "⏳ Eliminando pedido...")
                        // Opcional: Mostrar un loading indicator
                    }
                    is EliminacionState.Idle -> {}
                }
            }
        }
    }
    private fun setupSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.opciones_fecha_pedidos,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerFechaPedidos.adapter = adapter
        binding.spinnerFechaPedidos.setSelection(0)

        binding.spinnerFechaPedidos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                ocultarBotonGuardarOrden()
                val estadoTexto = when (position) {
                    0 -> {
                        pedidoViewModel.cargarPedidosHoy()
                        "Hoy"
                    }
                    1 -> {
                        pedidoViewModel.cargarPedidosManana()
                        "Mañana"
                    }
                    2 -> {
                        pedidoViewModel.cargarPedidosPasadoManana()
                        "Pasado Mañana"
                    }
                    else -> {
                        pedidoViewModel.cargarPedidosHoy()
                        "Hoy"
                    }
                }
                binding.tvEstadoPedidos.text = estadoTexto
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("PedidoFragment", "🔄 onResume: Forzando recarga de pedidos")

        // ✅ Solo recargar si NO hay cambios pendientes
        if (!pedidoAdapter.hasPendingChanges()) {
            val posicionActual = binding.spinnerFechaPedidos.selectedItemPosition
            when (posicionActual) {
                0 -> pedidoViewModel.cargarPedidosHoy()
                1 -> pedidoViewModel.cargarPedidosManana()
                2 -> pedidoViewModel.cargarPedidosPasadoManana()
            }
        } else {
            Log.d("PedidoFragment", "⚠️ Hay cambios pendientes, no recargo")
        }
    }

    private fun observarEstadoCarga() {
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(
                pedidoViewModel.loading,
                pedidoViewModel.pedidos
            ) { isLoading, pedidos ->
                Pair(isLoading, pedidos)
            }.collect { (isLoading, pedidos) ->

                Log.d("PedidoFragment", "🔄 Estado -> Loading: $isLoading, Pedidos: ${pedidos.size}")

                // ✅ Actualizar cantidad de pedidos
                actualizarCantidadPedidos(pedidos.size)

                when {
                    isLoading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.rvPedidos.visibility = View.GONE
                        binding.emptyState.visibility = View.GONE
                    }
                    pedidos.isEmpty() -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvPedidos.visibility = View.GONE
                        binding.emptyState.visibility = View.VISIBLE
                    }
                    else -> {
                        binding.progressBar.visibility = View.GONE
                        binding.emptyState.visibility = View.GONE
                        binding.rvPedidos.visibility = View.VISIBLE

                        // ✅ CORRECCIÓN: Solo actualizar si NO hay cambios pendientes
                        if (!pedidoAdapter.hasPendingChanges()) {
                            pedidoAdapter.submitList(pedidos.toList())
                        } else {
                            Log.d("PedidoFragment", "⚠️ Hay cambios pendientes, NO actualizo la lista")
                        }
                    }
                }
            }
        }
    }

    // ✅ NUEVO: Función para actualizar la cantidad con animación
    private fun actualizarCantidadPedidos(cantidad: Int) {
        binding.tvCantidadPedidos.apply {
            // Animación de escala
            animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(150)
                .withEndAction {
                    text = cantidad.toString()
                    animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start()
                }
                .start()
        }
    }

    private fun observarPedidoGuardado() {
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>("pedido_guardado")
            ?.observe(viewLifecycleOwner) { guardado ->
                if (guardado == true) {
                    Log.d("PedidoFragment", "🔄 Recargando pedidos porque se guardó uno nuevo")

                    val posicionActual = binding.spinnerFechaPedidos.selectedItemPosition
                    when (posicionActual) {
                        0 -> pedidoViewModel.cargarPedidosHoy()
                        1 -> pedidoViewModel.cargarPedidosManana()
                        2 -> pedidoViewModel.cargarPedidosPasadoManana()
                    }

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
                        Log.d("PedidoFragment", "Marcando como entregado...")
                    }
                    is EntregaState.Idle -> {}
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvPedidos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pedidoAdapter

            // ✅ NUEVO: Evitar que el RecyclerView se desplace al enfocar EditText
            descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
            isFocusableInTouchMode = true

            addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(
                context,
                androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
            ))
        }
    }

    private fun navegarADetalle(pedidoId: Long) {
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
        textWatcher?.let { binding.etBuscarPedido.removeTextChangedListener(it) }
        textWatcher = null
        _binding = null
    }
}