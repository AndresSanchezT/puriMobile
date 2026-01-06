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
import com.andresDev.puriapp.R
import com.andresDev.puriapp.data.model.Cliente
import com.andresDev.puriapp.data.model.DetallePedido
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
            val query = text?.toString().orEmpty()

            // Filtrado
            pedidoAddViewModel.filtrarProductos(query)

            if (query.isBlank()) {
                // 🔽 Modo dropdown
                binding.tilProducto.setEndIconDrawable(
                    R.drawable.mtrl_dropdown_arrow
                )
                binding.tilProducto.setEndIconOnClickListener {
                    binding.actvProducto.showDropDown()
                }
            } else {
                // ❌ Modo limpiar
                binding.tilProducto.setEndIconDrawable(R.drawable.ic_close)
                binding.tilProducto.setEndIconOnClickListener {
                    binding.actvProducto.setText("", false)
                    ocultarControlesCantidad()
                    pedidoAddViewModel.limpiarProductoSeleccionado()
                }
            }
        }

        // Estado inicial (MUY importante)
        binding.actvProducto.text?.let {
            binding.actvProducto.setText(it, false)
        }

        binding.actvProducto.setOnItemClickListener { _, _, position, _ ->
            val productoSeleccionado = productoAdapter.getItem(position)

            productoSeleccionado?.let { producto ->
                pedidoAddViewModel.seleccionarProducto(producto)
                binding.actvProducto.setText(producto.nombre, false)
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
            isNestedScrollingEnabled = true
            // ========== AGREGADO: Configurar animaciones del RecyclerView ==========
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
                addDuration = 300
                removeDuration = 300
            }
        }
    }

    // Variable de clase para guardar el precio unitario original
    private var precioUnitarioOriginal: Double = 0.0

    private fun setupListeners() {
        // 🔥 NUEVO: Listener para detectar cuando el usuario escribe en etCantidad
        binding.etCantidad.addTextChangedListener { editable ->
            val texto = editable?.toString() ?: ""

            // Solo actualizar si hay un número válido
            if (texto.isNotBlank()) {
                actualizarPrecioProductoSeleccionado()
            }
        }

        // Botón MÁS: Incrementar en 0.5
        binding.btnMas.setOnClickListener {
            animateButtonClick(it)

            val cantidadActual = binding.etCantidad.text.toString().toDoubleOrNull() ?: 1.0
            val nuevaCantidad = cantidadActual + 0.5

            binding.etCantidad.setText(String.format("%.1f", nuevaCantidad))
            // Ya no necesitas llamar actualizarPrecioProductoSeleccionado() aquí
            // porque el TextWatcher lo hará automáticamente
        }

        // Botón MENOS: Decrementar en 0.5 (mínimo 0.5)
        binding.btnMenos.setOnClickListener {
            animateButtonClick(it)

            val cantidadActual = binding.etCantidad.text.toString().toDoubleOrNull() ?: 1.0
            if (cantidadActual > 0.5) {
                val nuevaCantidad = cantidadActual - 0.5
                binding.etCantidad.setText(String.format("%.1f", nuevaCantidad))
                // Ya no necesitas llamar actualizarPrecioProductoSeleccionado() aquí
            }
        }

        // Ajustar precio de 0.50 en 0.50
        binding.btnMasPrecio.setOnClickListener {
            animateButtonClick(it)
            val precioActual = obtenerPrecioDelEditText()
            val nuevoPrecio = precioActual + 0.50

            precioUnitarioOriginal = nuevoPrecio
            binding.etPrecio.setText(String.format("%.2f", nuevoPrecio))

            // Resetear cantidad a 1 cuando cambias el precio manualmente
            binding.etCantidad.setText("1")
        }

        binding.btnMenosPrecio.setOnClickListener {
            animateButtonClick(it)
            val precioActual = obtenerPrecioDelEditText()
            if (precioActual >= 0.50) {
                val nuevoPrecio = precioActual - 0.50

                precioUnitarioOriginal = nuevoPrecio
                binding.etPrecio.setText(String.format("%.2f", nuevoPrecio))

                // Resetear cantidad a 1 cuando cambias el precio manualmente
                binding.etCantidad.setText("1")
            }
        }

        binding.btnAgregarProducto.setOnClickListener {
            animateButtonClick(it)
            agregarProductoAlPedido()
        }

        binding.btnGuardarPedido.setOnClickListener {
            animateButtonClick(it)
            showLoading()
            val observacion = binding.etObservacion.text.toString()
            pedidoAddViewModel.guardarPedido(observacion)
        }

        binding.etPrecio.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val precioEditado = obtenerPrecioDelEditText()
                if (precioEditado > 0) {
                    precioUnitarioOriginal = precioEditado
                    binding.etCantidad.setText("0.5")
                }
            }
        }

        // Validar entrada manual de cantidad
        binding.etCantidad.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validarYFormatearCantidad()
            }
        }
    }

    // ========== 3. NUEVA FUNCIÓN: Validar entrada manual ==========
    private fun validarYFormatearCantidad() {
        val texto = binding.etCantidad.text.toString()
        val cantidad = texto.toDoubleOrNull() ?: 1.0

        // Asegurar que sea mínimo 0.5 y múltiplo de 0.5
        val cantidadValida = when {
            cantidad < 0.5 -> 0.5
            else -> {
                // Redondear al múltiplo de 0.5 más cercano
                (kotlin.math.round(cantidad * 2) / 2).coerceAtLeast(0.5)
            }
        }

        binding.etCantidad.setText(String.format("%.1f", cantidadValida))
        actualizarPrecioProductoSeleccionado()
    }

    // ========== NUEVA FUNCIÓN: Obtener precio limpio del EditText ==========
    private fun obtenerPrecioDelEditText(): Double {
        val texto = binding.etPrecio.text.toString()
        // Si está vacío, retornar 0
        if (texto.isBlank()) return 0.0

        // Intentar convertir directamente (si ya es un número sin formato)
        texto.toDoubleOrNull()?.let { return it }

        // Si tiene formato de moneda, limpiarlo
        val limpio = texto
            .replace("S/", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()

        return limpio.toDoubleOrNull() ?: 0.0
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

        // ========== Actualizar RecyclerView directamente ==========
        updateProductosEnPedido(state.productosEnPedido)

        //Manejar el estado de loading principal
        if (state.isLoading) {
            showLoading()
        } else {
            // Solo ocultar si no hay loading
            hideLoading()
        }

        updateLoadingState(state.isLoading, state.isLoadingProductos)
        animateTotalesUpdate(state)

        state.error?.let { error ->
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

        animateRecyclerViewVisibility(state.productosEnPedido.isNotEmpty())
    }


    private fun updateClientesList(clientes: List<Cliente>) {
        clienteAdapter.actualizarClientes(clientes)
    }

    private fun updateProductosList(productos: List<Producto>) {
        productoAdapter.actualizarProductos(productos)
    }

    private fun updateProductosEnPedido(productos: List<DetallePedido>) {
        // Crear una NUEVA lista para forzar que DiffUtil detecte cambios
        productoPedidoAdapter.submitList(productos.toList()) {
            // Callback opcional: scroll al último item cuando se agrega uno nuevo
            if (productos.isNotEmpty()) {
                binding.rvProductosAgregados.smoothScrollToPosition(productos.size - 1)
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

    // ========== Actualizar totales con animación ==========
    private fun animateTotalesUpdate(state: PedidoAddState) {
        animateTextChange(binding.tvSubtotal, formatearPrecio(state.subtotal))
        animateTextChange(binding.tvIgv, formatearPrecio(state.igv))
        animateTextChange(binding.tvTotal, formatearPrecio(state.total))
    }

    // ========== Función para animar cambios de texto ==========
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

    // ========== Función para animar visibilidad del RecyclerView ==========
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

    // ========== 4. ACTUALIZAR mostrarControlesCantidad() ==========
    private fun mostrarControlesCantidad(producto: Producto) {
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

        // 🔥 CAMBIO: Iniciar en 0.5 en lugar de 1
        binding.etCantidad.setText("0.5")
        binding.etPrecio.setText(String.format("%.2f", producto.precio))

        precioUnitarioOriginal = producto.precio
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


    // Función simplificada que usa el precio unitario guardado
    private fun actualizarPrecioProductoSeleccionado() {
        val cantidad = binding.etCantidad.text.toString().toDoubleOrNull() ?: 0.5

        // Multiplicar el precio unitario original por la cantidad
        val precioTotal = precioUnitarioOriginal * cantidad
        binding.etPrecio.setText(String.format("%.2f", precioTotal))
    }

    private fun agregarProductoAlPedido() {
        val producto = pedidoAddViewModel.uiState.value.productoSeleccionado
        val cantidadTexto = binding.etCantidad.text.toString()
        val cantidad = cantidadTexto.toDoubleOrNull() ?: 1.0
        val precioTotal = binding.etPrecio.text.toString().toDoubleOrNull() ?: 0.0

        // Validar cantidad mínima
        if (cantidad < 0.5) {
            showError("La cantidad mínima es 0.5")
            return
        }

        if (producto != null && cantidad > 0) {
            pedidoAddViewModel.agregarProducto(producto, cantidad, precioTotal)

            binding.actvProducto.setText("")
            binding.etCantidad.setText("1")
            ocultarControlesCantidad()

            showSuccessSnackbar("Producto agregado")
        }
    }

    // ========== Función para mostrar loading ==========

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.btnGuardarPedido.isEnabled = false
    }
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.loadingOverlay.visibility = View.GONE
        binding.btnGuardarPedido.isEnabled = true
    }

    // ==========  Función para animación de éxito ==========
    private fun showSuccessAnimation() {
        showSuccessSnackbar("¡Pedido registrado exitosamente!")

        // Marcar que se guardó un pedido
        findNavController().previousBackStackEntry?.savedStateHandle?.set("pedido_guardado", true)

        view?.postDelayed({
            findNavController().navigateUp()
        }, 1500)
    }

    // ==========  Snackbar con estilo personalizado ==========
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

        // ==========  Efecto shake en error ==========
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