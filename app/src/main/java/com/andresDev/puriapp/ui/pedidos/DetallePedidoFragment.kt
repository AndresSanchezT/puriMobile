package com.andresDev.puriapp.ui.pedidos

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.andresDev.puriapp.R
import com.andresDev.puriapp.data.model.DetallePedido
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.data.model.PedidoAddState
import com.andresDev.puriapp.data.model.Producto
import com.andresDev.puriapp.databinding.FragmentDetallePedidoBinding
import com.andresDev.puriapp.ui.pedidos.adapter.DetallePedidoAdapter
import com.andresDev.puriapp.ui.pedidos.adapter.ProductoArrayAdapter
import com.andresDev.puriapp.ui.pedidos.adapter.ProductoPedidoAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class DetallePedidoFragment : Fragment() {

    private var _binding: FragmentDetallePedidoBinding? = null
    private val binding get() = _binding!!

    private val detallePedidoViewModel: DetallePedidoViewModel by viewModels()

    private lateinit var productoAdapter: ProductoArrayAdapter

    private lateinit var detallePedidoAdapter: DetallePedidoAdapter
    private val args: DetallePedidoFragmentArgs by navArgs()

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
        observeUiState()
        setupProductoAutoComplete()

    }

    private fun setupRecyclerView() {
        detallePedidoAdapter = DetallePedidoAdapter(

            onEliminar = { productoId ->
                detallePedidoViewModel.eliminarProducto(productoId)
                // ========== AGREGADO: Snackbar al eliminar ==========
                showSuccessSnackbar("Producto eliminado")
            },
            onCantidadChanged = { productoId, cantidad ->
                detallePedidoViewModel.actualizarCantidad(productoId, cantidad)
            },
            onPrecioChanged = { productoId, precio ->
                detallePedidoViewModel.actualizarPrecioUnitario(productoId, precio)
            },
            onSubtotalChanged = { productoId, subtotal ->
                detallePedidoViewModel.actualizarSubtotal(productoId, subtotal)
            }
        )

        binding.rvProductos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = detallePedidoAdapter
            isNestedScrollingEnabled = true
            // ✅ Desactivar animaciones del ItemAnimator para evitar conflictos
            itemAnimator = null
        }
    }
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                detallePedidoViewModel.uiState.collectLatest { state ->

                    // ✅ Mostrar loadingOverlay cuando está actualizando
                    binding.loadingOverlay.visibility = if (state.isUpdating) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    // ✅ PASO 1: Manejar estados de carga inicial/error
                    when {
                        state.isLoading -> {
                            mostrarCargando() // Muestra loadingLayout
                            return@collectLatest
                        }
                        state.error != null && state.error != "success" -> {
                            mostrarError(state.error)
                            return@collectLatest
                        }
                        state.error == "success" -> {
                            hideLoading()
                            showSuccessAnimation()
                            return@collectLatest
                        }
                    }

                    // ✅ PASO 2: Si hay pedido cargado, mostrar contenido
                    state.pedidoCargado?.let { pedido ->
                        mostrarContenido()
                        mostrarDatosPedido(pedido)
                    }

                    // ✅ PASO 3: Actualizar lista SOLO si el contenido está visible
                    if (binding.contentLayout.visibility == View.VISIBLE) {
                        updateProductosEnPedido(state.productosEnPedido)
                        updateLoadingState(state.isLoadingProductos)
                        updateProductosList(state.productosFiltrados)
                        animateRecyclerViewVisibility(state.productosEnPedido.isNotEmpty())
                    }

                    animateTotalesUpdate(state)
                }
            }
        }
    }
    // Variable de clase para guardar el precio unitario original
    private var precioUnitarioOriginal: Double = 0.0
    private fun setupListeners() {
        binding.btnCerrar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnGuardar.setOnClickListener {
            detallePedidoViewModel.actualizarPedido()
        }

        // ✅ Mostrar card para agregar producto
        binding.btnAgregarProducto.setOnClickListener {
            mostrarCardAgregarProducto()
        }

        // ✅ Cerrar card de agregar producto
        binding.btnCerrarCardProducto.setOnClickListener {
            ocultarCardAgregarProducto()
        }

        // ✅ Botones de cantidad
        binding.btnMas.setOnClickListener {
            animateButtonClick(it)

            val cantidadActual = binding.etCantidad.text.toString().toDoubleOrNull() ?: 1.0
            val nuevaCantidad = cantidadActual + 0.5

            binding.etCantidad.setText(String.format("%.1f", nuevaCantidad))
            // Ya no necesitas llamar actualizarPrecioProductoSeleccionado() aquí
            // porque el TextWatcher lo hará automáticamente
        }

        binding.btnMenos.setOnClickListener {
            animateButtonClick(it)

            val cantidadActual = binding.etCantidad.text.toString().toDoubleOrNull() ?: 1.0
            if (cantidadActual > 0.5) {
                val nuevaCantidad = cantidadActual - 0.5
                binding.etCantidad.setText(String.format("%.1f", nuevaCantidad))
                // Ya no necesitas llamar actualizarPrecioProductoSeleccionado() aquí
            }
        }

        // ✅ Botones de precio
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

        // ✅ Confirmar agregar producto
        binding.btnAgregarProductoAlPedido.setOnClickListener {
            animateButtonClick(it)
            agregarProductoAlPedido()
        }

        binding.btnReintentar.setOnClickListener {
            detallePedidoViewModel.cargarPedido(args.pedidoId)
        }
    }

    private fun agregarProductoAlPedido() {
        val producto = detallePedidoViewModel.uiState.value.productoSeleccionado
        val cantidadTexto = binding.etCantidad.text.toString()
        val cantidad = cantidadTexto.toDoubleOrNull() ?: 1.0
        val precioUnitario = binding.etPrecio.text.toString()
        val precioTotal = (precioUnitario.toDoubleOrNull() ?: 0.0) * cantidad

        // Validar cantidad mínima
        if (cantidad < 0.5) {
            showError("La cantidad mínima es 0.5")
            return
        }

        if (producto != null && cantidad > 0) {
            detallePedidoViewModel.agregarProducto(producto, cantidad, precioTotal)

            binding.actvProducto.setText("")
            binding.etCantidad.setText("1")
            ocultarControlesCantidad()

            showSuccessSnackbar("Producto agregado")
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

    private fun mostrarDatosPedido(pedido: Pedido) {
        binding.apply {
            // Información del cliente
            tvNombreCliente.text = pedido.cliente?.nombreContacto ?: "Sin nombre"
            tvDireccion.text = pedido.cliente?.direccion ?: "Sin dirección"
            tvTelefono.text = pedido.cliente?.telefono ?: "Sin teléfono"

            // Información del pedido
            tvVendedor.text = pedido.vendedor?.nombre ?: "Sin vendedor"
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

    private fun mostrarCardAgregarProducto() {
        binding.cardAgregarProducto.visibility = View.VISIBLE

        // Animación de entrada suave
        binding.cardAgregarProducto.alpha = 0f
        binding.cardAgregarProducto.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        // Limpiar campos
        binding.actvProducto.text.clear()
        binding.etCantidad.setText("1")
        binding.etPrecio.setText("0.00")
    }

    private fun ocultarCardAgregarProducto() {
        binding.cardAgregarProducto.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.cardAgregarProducto.visibility = View.GONE
            }
            .start()
    }

    private fun setupProductoAutoComplete() {
        productoAdapter = ProductoArrayAdapter(requireContext())

        binding.actvProducto.setAdapter(productoAdapter)
        binding.actvProducto.threshold = 1

        binding.actvProducto.addTextChangedListener { text ->
            val query = text?.toString().orEmpty()

            // Filtrado
            detallePedidoViewModel.filtrarProductos(query)

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
                    detallePedidoViewModel.limpiarProductoSeleccionado()
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
                detallePedidoViewModel.seleccionarProducto(producto)
                binding.actvProducto.setText(producto.nombre, false)
                ocultarTeclado()
                mostrarControlesCantidad(producto)
            }
        }
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

    private fun updateProductosList(productos: List<Producto>) {
        productoAdapter.actualizarProductos(productos)
    }

    private fun updateProductosEnPedido(productos: List<DetallePedido>) {
        // Debug logs (eliminar en producción)
        android.util.Log.d("DetallePedido", "📦 Actualizando: ${productos.size} items")

        // Crear nueva lista para forzar detección de cambios
        val nuevaLista = productos.map { it.copy() }

        detallePedidoAdapter.submitList(nuevaLista) {
            // Callback después de actualización exitosa
            android.util.Log.d("DetallePedido", "✅ Lista actualizada en adapter")

            if (nuevaLista.isNotEmpty()) {
                binding.rvProductos.post {
                    binding.rvProductos.smoothScrollToPosition(nuevaLista.size - 1)
                }
            }
        }
    }
    // ========== Función para mostrar loading ==========

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
//        binding.loadingOverlay.visibility = View.VISIBLE
        binding.btnGuardar.isEnabled = false
    }
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
//        binding.loadingOverlay.visibility = View.GONE
        binding.btnGuardar.isEnabled = true
    }


    private fun showError(errorMsg: String) {
        Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG)
            .setAction("Reintentar") {
                detallePedidoViewModel.cargarProductos()
                detallePedidoViewModel.limpiarError()
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
    private fun ocultarTeclado() {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
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

        binding.btnAgregarProductoAlPedido.apply {
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

    }

    private fun confirmarAgregarProducto() {
        // TODO: Lógica para agregar el producto al pedido
        // Actualizar RecyclerView y totales

        ocultarCardAgregarProducto()

        // Mostrar mensaje de éxito
        Toast.makeText(requireContext(), "Producto agregado", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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


    // ========== Función para animar visibilidad del RecyclerView ==========
    private fun animateRecyclerViewVisibility(show: Boolean) {
        if (show && binding.rvProductos.visibility != View.VISIBLE) {
            binding.rvProductos.apply {
                visibility = View.VISIBLE
                alpha = 0f
                translationY = 30f
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .start()
            }
        } else if (!show && binding.rvProductos.visibility == View.VISIBLE) {
            binding.rvProductos.animate()
                .alpha(0f)
                .translationY(30f)
                .setDuration(300)
                .withEndAction {
                    binding.rvProductos.visibility = View.GONE
                }
                .start()
        }
    }

    private fun updateLoadingState(isLoadingProductos: Boolean) {
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
    private fun formatearPrecio(precio: Double): String {
        val formato = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
        return formato.format(precio)
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