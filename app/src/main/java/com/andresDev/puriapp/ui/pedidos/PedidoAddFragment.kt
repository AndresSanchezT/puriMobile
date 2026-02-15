package com.andresDev.puriapp.ui.pedidos

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
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

    private var tipoFechaSeleccionada: String = "MANANA"

    private val pedidoAddViewModel: PedidoAddViewModel by viewModels()

    private lateinit var clienteAdapter: ClienteArrayAdapter
    private lateinit var productoAdapter: ProductoArrayAdapter
    private lateinit var productoPedidoAdapter: ProductoPedidoAdapter

    // ✅ NUEVO: Variable para controlar actualizaciones automáticas
    private var isUpdatingFields = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPedidoAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        animateTitleEntry()
        setupSpinnerFechaEntrega()
        setupClienteAutoComplete()
        setupProductoAutoComplete()
        setupRecyclerView()
        setupListeners()
        observeUiState()
    }

    private fun setupSpinnerFechaEntrega() {
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.opciones_fecha_pedidos,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerFechaEntrega.adapter = adapter
        binding.spinnerFechaEntrega.setSelection(1)

        binding.spinnerFechaEntrega.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                tipoFechaSeleccionada = when (position) {
                    0 -> "HOY"
                    1 -> "MANANA"
                    2 -> "PASADO_MANANA"
                    else -> "MANANA"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

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

        binding.actvCliente.setOnItemClickListener { _, _, position, _ ->
            val clienteSeleccionado = clienteAdapter.getItem(position)

            clienteSeleccionado?.let { cliente ->
                pedidoAddViewModel.seleccionarCliente(cliente)
                binding.actvCliente.setText(cliente.nombreContacto, false)

                // ✅ Cerrar teclado
                ocultarTeclado()

                // ✅ Quitar foco del AutoComplete
                binding.root.clearFocus()

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

            pedidoAddViewModel.filtrarProductos(query)

            if (query.isBlank()) {
                binding.tilProducto.setEndIconDrawable(R.drawable.mtrl_dropdown_arrow)
                binding.tilProducto.setEndIconOnClickListener {
                    binding.actvProducto.showDropDown()
                }
            } else {
                binding.tilProducto.setEndIconDrawable(R.drawable.ic_close)
                binding.tilProducto.setEndIconOnClickListener {
                    binding.actvProducto.setText("", false)
                    ocultarControlesCantidad()
                    pedidoAddViewModel.limpiarProductoSeleccionado()
                }
            }
        }

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
                enfocarBloqueCantidadPrecio()
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
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
                addDuration = 300
                removeDuration = 300
            }
        }
    }

    private fun setupListeners() {
        // ✅ NUEVO: Listener para cantidad - actualiza subtotal
        binding.etCantidad.addTextChangedListener { editable ->
            if (isUpdatingFields) return@addTextChangedListener

            val cantidad = editable?.toString()?.toDoubleOrNull() ?: 0.0
            val precioUnitario = binding.etPrecio.text.toString().toDoubleOrNull() ?: 0.0

            if (cantidad > 0 && precioUnitario > 0) {
                isUpdatingFields = true
                val subtotal = cantidad * precioUnitario
                binding.tvSubtotalProducto.setText(String.format("%.2f", subtotal))
                isUpdatingFields = false
            }
        }

        // ✅ NUEVO: Listener para precio unitario - actualiza subtotal
        binding.etPrecio.addTextChangedListener { editable ->
            if (isUpdatingFields) return@addTextChangedListener

            val precioUnitario = editable?.toString()?.toDoubleOrNull() ?: 0.0
            val cantidad = binding.etCantidad.text.toString().toDoubleOrNull() ?: 0.0

            if (cantidad > 0 && precioUnitario > 0) {
                isUpdatingFields = true
                val subtotal = cantidad * precioUnitario
                binding.tvSubtotalProducto.setText(String.format("%.2f", subtotal))
                isUpdatingFields = false
            }
        }

        // ✅ NUEVO: Listener para subtotal - actualiza precio unitario
        binding.tvSubtotalProducto.addTextChangedListener { editable ->
            if (isUpdatingFields) return@addTextChangedListener

            val subtotal = editable?.toString()?.toDoubleOrNull() ?: 0.0
            val cantidad = binding.etCantidad.text.toString().toDoubleOrNull() ?: 0.0

            if (subtotal > 0 && cantidad > 0) {
                isUpdatingFields = true
                val precioUnitario = subtotal / cantidad
                binding.etPrecio.setText(String.format("%.2f", precioUnitario))
                isUpdatingFields = false
            }
        }

        binding.btnMas.setOnClickListener {
            animateButtonClick(it)
            val cantidadActual = binding.etCantidad.text.toString().toDoubleOrNull() ?: 1.0
            val nuevaCantidad = cantidadActual + 0.5
            binding.etCantidad.setText(String.format("%.1f", nuevaCantidad))
        }

        binding.btnMenos.setOnClickListener {
            animateButtonClick(it)
            val cantidadActual = binding.etCantidad.text.toString().toDoubleOrNull() ?: 1.0
            if (cantidadActual > 0.5) {
                val nuevaCantidad = cantidadActual - 0.5
                binding.etCantidad.setText(String.format("%.1f", nuevaCantidad))
            }
        }

        binding.btnMasPrecio.setOnClickListener {
            animateButtonClick(it)
            val precioActual = binding.etPrecio.text.toString().toDoubleOrNull() ?: 0.0
            val nuevoPrecio = precioActual + 0.50
            binding.etPrecio.setText(String.format("%.2f", nuevoPrecio))
        }

        binding.btnMenosPrecio.setOnClickListener {
            animateButtonClick(it)
            val precioActual = binding.etPrecio.text.toString().toDoubleOrNull() ?: 0.0
            if (precioActual >= 0.50) {
                val nuevoPrecio = precioActual - 0.50
                binding.etPrecio.setText(String.format("%.2f", nuevoPrecio))
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
            pedidoAddViewModel.guardarPedido(observacion, tipoFechaSeleccionada)
        }

        binding.etCantidad.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validarYFormatearCantidad()
            }
        }
    }

    private fun validarYFormatearCantidad() {
        val texto = binding.etCantidad.text.toString()
        val cantidad = texto.toDoubleOrNull() ?: 1.0

        val cantidadValida = when {
            cantidad < 0.5 -> 0.5
            else -> {
                (kotlin.math.round(cantidad * 2) / 2).coerceAtLeast(0.5)
            }
        }

        binding.etCantidad.setText(String.format("%.1f", cantidadValida))
    }

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

                    if (state.mostrarDialogoConfirmacion) {
                        mostrarDialogoConfirmacion()
                    }
                }
            }
        }
    }

    private fun updateUI(state: PedidoAddState) {
        updateClientesList(state.clientesFiltrados)
        updateProductosList(state.productosFiltrados)
        updateProductosEnPedido(state.productosEnPedido)

        if (state.isLoading) {
            showLoading()
        } else {
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

    private fun mostrarDialogoConfirmacion() {
        val clienteNombre = pedidoAddViewModel.uiState.value.clienteSeleccionado?.nombreContacto ?: ""

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ Pedido Existente")
            .setMessage("Ya existe un pedido registrado para $clienteNombre en esta fecha.\n\n¿Desea continuar de todas formas?")
            .setPositiveButton("Continuar") { dialog, _ ->
                dialog.dismiss()
                pedidoAddViewModel.ocultarDialogoConfirmacion()
                val observacion = binding.etObservacion.text.toString()
                pedidoAddViewModel.guardarPedido(observacion, tipoFechaSeleccionada, forzar = true)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                pedidoAddViewModel.ocultarDialogoConfirmacion()
            }
             // Si tienes un drawable personalizado
            .setCancelable(false)
            .show()
    }
    private fun updateClientesList(clientes: List<Cliente>) {
        clienteAdapter.actualizarClientes(clientes)
    }

    private fun updateProductosList(productos: List<Producto>) {
        productoAdapter.actualizarProductos(productos)
    }

    private fun updateProductosEnPedido(productos: List<DetallePedido>) {
        productoPedidoAdapter.submitList(productos.toList()) {
            if (productos.isNotEmpty()) {
                binding.rvProductosAgregados.smoothScrollToPosition(0)
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

    private fun animateTotalesUpdate(state: PedidoAddState) {
        animateTextChange(binding.tvSubtotal, formatearPrecio(state.subtotal))
        animateTextChange(binding.tvIgv, formatearPrecio(state.igv))
        animateTextChange(binding.tvTotal, formatearPrecio(state.total))
    }

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

        // ✅ MODIFICADO: Inicializar valores y calcular subtotal
        isUpdatingFields = true
        binding.etCantidad.setText("1")
        binding.etPrecio.setText(String.format("%.2f", producto.precio))

        val subtotalInicial = 1 * producto.precio
        binding.tvSubtotalProducto.setText(String.format("%.2f", subtotalInicial))
        isUpdatingFields = false
    }

    private fun enfocarBloqueCantidadPrecio() {
        binding.scrollRoot.post {
            // Scroll hasta el layout de cantidad/precio
            binding.scrollRoot.smoothScrollTo(
                0,
                binding.layoutCantidadPrecio.top
            )

            // Quitar foco de cualquier EditText
            binding.root.clearFocus()

            // (Opcional) hacer el bloque "focusable" visualmente
            binding.layoutCantidadPrecio.isFocusable = true
            binding.layoutCantidadPrecio.isFocusableInTouchMode = true
            binding.layoutCantidadPrecio.requestFocus()

            // Pequeña animación para guiar la vista
            binding.layoutCantidadPrecio.animate()
                .alpha(0.95f)
                .setDuration(120)
                .withEndAction {
                    binding.layoutCantidadPrecio.animate()
                        .alpha(1f)
                        .setDuration(120)
                        .start()
                }
                .start()
        }
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

    private fun agregarProductoAlPedido() {
        val producto = pedidoAddViewModel.uiState.value.productoSeleccionado
        val cantidad = binding.etCantidad.text.toString().toDoubleOrNull() ?: 0.0
        val precioUnitario = binding.etPrecio.text.toString().toDoubleOrNull() ?: 0.0
        val subtotal = binding.tvSubtotalProducto.text.toString().toDoubleOrNull() ?: 0.0

        // ✅ Validaciones
        if (cantidad < 0.5) {
            showError("La cantidad mínima es 0.5")
            return
        }

        if (precioUnitario <= 0) {
            showError("El precio unitario debe ser mayor a 0")
            return
        }

        if (subtotal <= 0) {
            showError("El subtotal debe ser mayor a 0")
            return
        }

        if (producto != null) {
            pedidoAddViewModel.agregarProducto(
                producto = producto,
                cantidad = cantidad,
                precioTotal = subtotal,  // ✅ Usar el subtotal calculado
                precioUnitario = precioUnitario
            )

            binding.actvProducto.setText("")
            ocultarControlesCantidad()
            showSuccessSnackbar("Producto agregado")
        }
    }

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

    private fun showSuccessAnimation() {
        showSuccessSnackbar("¡Pedido registrado exitosamente!")
        findNavController().previousBackStackEntry?.savedStateHandle?.set("pedido_guardado", true)
        view?.postDelayed({
            findNavController().navigateUp()
        }, 1500)
    }

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