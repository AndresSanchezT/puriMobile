package com.andresDev.puriapp.ui.pedidos

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.andresDev.puriapp.data.model.Cliente
import com.andresDev.puriapp.data.model.PedidoAddState
import com.andresDev.puriapp.data.model.Producto
import com.andresDev.puriapp.databinding.FragmentPedidoAddBinding
import com.andresDev.puriapp.ui.pedidos.adapter.ClienteArrayAdapter
import com.andresDev.puriapp.ui.pedidos.adapter.ProductoArrayAdapter
import com.andresDev.puriapp.ui.pedidos.adapter.ProductoPedidoAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class PedidoAddFragment : Fragment() {

    private var _binding: FragmentPedidoAddBinding? = null
    private val binding get() = _binding!!

    private val pedidoAddViewModel: PedidoAddViewModel by viewModels()

    private lateinit var clienteAdapter: ClienteArrayAdapter
    private lateinit var productoAdapter: ProductoArrayAdapter
    private lateinit var productoPedidoAdapter: ProductoPedidoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPedidoAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ========== AGREGADO: Animación de entrada del título ==========
        animateTitleEntry()

        setupClienteAutoComplete()
        setupProductoAutoComplete()
        setupRecyclerView()
        setupListeners()
        observeUiState()
    }

    // ========== AGREGADO: Función de animación de título ==========
    private fun animateTitleEntry() {
        binding.tvTitulo.apply {
            alpha = 0f
            translationY = -50f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(OvershootInterpolator())
                .start()
        }
    }

    private fun setupClienteAutoComplete() {
        clienteAdapter = ClienteArrayAdapter(requireContext())

        binding.actvCliente.setAdapter(clienteAdapter)
        binding.actvCliente.threshold = 1

        binding.actvCliente.addTextChangedListener { text ->
            pedidoAddViewModel.filtrarClientes(text.toString())
        }

        binding.actvCliente.setOnItemClickListener { parent, _, position, _ ->
            val clienteSeleccionado = clienteAdapter.getItem(position)

            clienteSeleccionado?.let { cliente ->
                pedidoAddViewModel.seleccionarCliente(cliente)
                binding.actvCliente.setText(cliente.nombreContacto, false)

                // ========== MODIFICADO: Snackbar con animación ==========
                showSuccessSnackbar("Cliente: ${cliente.nombreContacto}")
            }
        }
    }

    private fun setupProductoAutoComplete() {
        productoAdapter = ProductoArrayAdapter(requireContext())

        binding.actvProducto.setAdapter(productoAdapter)
        binding.actvProducto.threshold = 1

        binding.actvProducto.addTextChangedListener { text ->
            pedidoAddViewModel.filtrarProductos(text.toString())
        }

        binding.actvProducto.setOnItemClickListener { parent, _, position, _ ->
            val productoSeleccionado = productoAdapter.getItem(position)

            productoSeleccionado?.let { producto ->
                pedidoAddViewModel.seleccionarProducto(producto)
                binding.actvProducto.setText("", false)
                ocultarTeclado()
                mostrarControlesCantidad(producto)
            }
        }
    }

    private fun ocultarTeclado() {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private fun setupRecyclerView() {
        productoPedidoAdapter = ProductoPedidoAdapter(
            onCantidadChanged = { productoId, cantidad ->
                pedidoAddViewModel.actualizarCantidad(productoId, cantidad)
            },
            onEliminar = { productoId ->
                pedidoAddViewModel.eliminarProducto(productoId)
                // ========== AGREGADO: Snackbar al eliminar ==========
                showSuccessSnackbar("Producto eliminado")
            },
            onIncrementar = { productoId ->
                pedidoAddViewModel.incrementarCantidad(productoId)
            },
            onDecrementar = { productoId ->
                pedidoAddViewModel.decrementarCantidad(productoId)
            }
        )

        binding.rvProductosAgregados.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productoPedidoAdapter

            // ========== AGREGADO: Configurar animaciones del RecyclerView ==========
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
                addDuration = 300
                removeDuration = 300
            }
        }
    }

    private fun setupListeners() {
        // ========== MODIFICADO: Botones con animación de click ==========
        binding.btnMas.setOnClickListener {
            animateButtonClick(it)
            val cantidadActual = binding.etCantidad.text.toString().toIntOrNull() ?: 1
            binding.etCantidad.setText((cantidadActual + 1).toString())
            actualizarPrecioProductoSeleccionado()
        }

        binding.btnMenos.setOnClickListener {
            animateButtonClick(it)
            val cantidadActual = binding.etCantidad.text.toString().toIntOrNull() ?: 1
            if (cantidadActual > 1) {
                binding.etCantidad.setText((cantidadActual - 1).toString())
                actualizarPrecioProductoSeleccionado()
            }
        }

        binding.etCantidad.addTextChangedListener {
            actualizarPrecioProductoSeleccionado()
        }

        binding.btnAgregarProducto.setOnClickListener {
            animateButtonClick(it)
            agregarProductoAlPedido()
        }

        binding.btnGuardarPedido.setOnClickListener {
            animateButtonClick(it)
            pedidoAddViewModel.guardarPedido()
        }
    }

    // ========== AGREGADO: Función de animación de botones ==========
    private fun animateButtonClick(view: View) {
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pedidoAddViewModel.uiState.collectLatest { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: PedidoAddState) {
        updateClientesList(state.clientesFiltrados)
        updateProductosList(state.productosFiltrados)
        updateProductosEnPedido(state.productosEnPedido.size)
        updateLoadingState(state.isLoading, state.isLoadingProductos)

        // ========== MODIFICADO: Actualizar totales con animación ==========
        animateTotalesUpdate(state)

        state.error?.let { error ->
            // ========== MODIFICADO: Manejo de errores con animaciones ==========
            when (error) {
                "success" -> {
                    hideLoading()
                    showSuccessAnimation()
                }
                else -> {
                    hideLoading()
                    showError(error)
                }
            }
        }

        // ========== MODIFICADO: Visibilidad del RecyclerView con animación ==========
        animateRecyclerViewVisibility(state.productosEnPedido.isNotEmpty())
    }

    private fun updateClientesList(clientes: List<Cliente>) {
        clienteAdapter.actualizarClientes(clientes)
    }

    private fun updateProductosList(productos: List<Producto>) {
        productoAdapter.actualizarProductos(productos)
    }

    private fun updateProductosEnPedido(cantidadProductos: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            pedidoAddViewModel.uiState.collectLatest { state ->
                productoPedidoAdapter.submitList(state.productosEnPedido)
            }
        }
    }

    private fun updateLoadingState(isLoadingClientes: Boolean, isLoadingProductos: Boolean) {
        binding.tilCliente.isEnabled = !isLoadingClientes
        binding.actvCliente.hint = if (isLoadingClientes) {
            "Cargando clientes..."
        } else {
            "Seleccionar o buscar cliente"
        }

        binding.tilProducto.isEnabled = !isLoadingProductos
        binding.actvProducto.hint = if (isLoadingProductos) {
            "Cargando productos..."
        } else {
            "Buscar producto"
        }
    }

    // ========== MODIFICADO: Actualizar totales con animación ==========
    private fun animateTotalesUpdate(state: PedidoAddState) {
        animateTextChange(binding.tvSubtotal, formatearPrecio(state.subtotal))
        animateTextChange(binding.tvIgv, formatearPrecio(state.igv))
        animateTextChange(binding.tvTotal, formatearPrecio(state.total))
    }

    // ========== AGREGADO: Función para animar cambios de texto ==========
    private fun animateTextChange(textView: android.widget.TextView, newText: String) {
        if (textView.text != newText) {
            textView.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(150)
                .withEndAction {
                    textView.text = newText
                    textView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start()
                }
                .start()
        }
    }

    // ========== AGREGADO: Función para animar visibilidad del RecyclerView ==========
    private fun animateRecyclerViewVisibility(show: Boolean) {
        if (show && binding.rvProductosAgregados.visibility != View.VISIBLE) {
            binding.rvProductosAgregados.apply {
                visibility = View.VISIBLE
                alpha = 0f
                translationY = 30f
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .start()
            }
        } else if (!show && binding.rvProductosAgregados.visibility == View.VISIBLE) {
            binding.rvProductosAgregados.animate()
                .alpha(0f)
                .translationY(30f)
                .setDuration(300)
                .withEndAction {
                    binding.rvProductosAgregados.visibility = View.GONE
                }
                .start()
        }
    }

    private fun mostrarControlesCantidad(producto: Producto) {
        // ========== MODIFICADO: Mostrar controles con animación ==========
        binding.layoutCantidadPrecio.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleY(1f)
                .setDuration(200)
                .start()
        }

        binding.btnAgregarProducto.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleY(1f)
                .setDuration(200)
                .start()
        }

        binding.etCantidad.setText("1")
        binding.tvPrecio.text = formatearPrecio(producto.precio)
    }

    private fun ocultarControlesCantidad() {
        binding.layoutCantidadPrecio.animate()
            .alpha(0f)
            .scaleY(0.8f)
            .setDuration(200)
            .withEndAction {
                binding.layoutCantidadPrecio.visibility = View.GONE
            }
            .start()

        binding.btnAgregarProducto.animate()
            .alpha(0f)
            .scaleY(0.8f)
            .setDuration(200)
            .withEndAction {
                binding.btnAgregarProducto.visibility = View.GONE
            }
            .start()
    }

    private fun actualizarPrecioProductoSeleccionado() {
        val cantidad = binding.etCantidad.text.toString().toIntOrNull() ?: 1
        val producto = pedidoAddViewModel.uiState.value.productoSeleccionado

        producto?.let {
            val precioTotal = it.precio * cantidad
            // ========== MODIFICADO: Actualizar precio con animación ==========
            animateTextChange(binding.tvPrecio, formatearPrecio(precioTotal))
        }
    }

    private fun agregarProductoAlPedido() {
        val producto = pedidoAddViewModel.uiState.value.productoSeleccionado
        val cantidad = binding.etCantidad.text.toString().toIntOrNull() ?: 1

        if (producto != null && cantidad > 0) {
            pedidoAddViewModel.agregarProducto(producto, cantidad)

            binding.actvProducto.setText("")
            binding.etCantidad.setText("")
            ocultarControlesCantidad()

            // ========== MODIFICADO: Snackbar animado ==========
            showSuccessSnackbar("Producto agregado")

            // ========== AGREGADO: Scroll suave al último item ==========
            binding.rvProductosAgregados.smoothScrollToPosition(
                productoPedidoAdapter.itemCount
            )
        }
    }

    // ========== AGREGADO: Función para mostrar loading ==========
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.loadingOverlay.visibility = View.GONE
        binding.btnGuardarPedido.isEnabled = true
    }

    // ========== AGREGADO: Función para animación de éxito ==========
    private fun showSuccessAnimation() {
        showSuccessSnackbar("¡Pedido registrado exitosamente!")

        view?.postDelayed({
            findNavController().navigateUp()
        }, 1500)
    }

    // ========== AGREGADO: Snackbar con estilo personalizado ==========
    private fun showSuccessSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }

    private fun showError(errorMsg: String) {
        Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG)
            .setAction("Reintentar") {
                pedidoAddViewModel.cargarClientes()
                pedidoAddViewModel.cargarProductos()
                pedidoAddViewModel.limpiarError()
            }
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()

        // ========== AGREGADO: Efecto shake en error ==========
        binding.root.animate()
            .translationX(-10f)
            .setDuration(50)
            .withEndAction {
                binding.root.animate()
                    .translationX(10f)
                    .setDuration(50)
                    .withEndAction {
                        binding.root.animate()
                            .translationX(0f)
                            .setDuration(50)
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun formatearPrecio(precio: Double): String {
        val formato = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
        return formato.format(precio)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}