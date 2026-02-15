package com.andresDev.puriapp.ui.pedidos

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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

    // ✅ NUEVO: Variable para controlar actualizaciones automáticas
    private var isUpdatingFields = false

    private fun EditText.getDouble(): Double {
        return text.toString().toDoubleOrNull() ?: 0.0
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
        observeUiState()
        setupProductoAutoComplete()
    }

    private fun setupRecyclerView() {
        detallePedidoAdapter = DetallePedidoAdapter(
            onEliminar = { productoId ->
                detallePedidoViewModel.eliminarProducto(productoId)
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
            itemAnimator = null
        }
    }

    private var totalPedido: Double = 0.0
    private var totalAnterior: Double = 0.0

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                detallePedidoViewModel.uiState.collectLatest { state ->

                    binding.loadingOverlay.visibility = if (state.isUpdating) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    when {
                        state.isLoading -> {
                            mostrarCargando()
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

                    state.pedidoCargado?.let { pedido ->
                        mostrarContenido()
                        mostrarDatosPedido(pedido)
                        inicializarPagos(pedido.total ?: 0.0)
                    }

                    if (binding.contentLayout.visibility == View.VISIBLE) {
                        updateProductosEnPedido(state.productosEnPedido)
                        updateLoadingState(state.isLoadingProductos)
                        updateProductosList(state.productosFiltrados)
                        animateRecyclerViewVisibility(state.productosEnPedido.isNotEmpty())
                    }

                    animateTotalesUpdate(state)
                    actualizarCreditoPorCambioTotal(state.total)
                }
            }
        }
    }

    private fun actualizarCreditoPorCambioTotal(nuevoTotal: Double) {
        if (nuevoTotal != totalAnterior && totalAnterior != 0.0) {
            totalPedido = nuevoTotal
            totalAnterior = nuevoTotal
            recalcularCredito()
        } else if (totalAnterior == 0.0) {
            totalAnterior = nuevoTotal
        }
    }

    private fun inicializarPagos(total: Double) {
        totalPedido = total
        totalAnterior = total

        val pedido = detallePedidoViewModel.uiState.value.pedidoCargado

        if (pedido?.yape != null && pedido.yape > 0) {
            binding.etYape.setText(String.format(Locale.US, "%.2f", pedido.yape))
        } else {
            binding.etYape.setText("")
        }

        if (pedido?.plin != null && pedido.plin > 0) {
            binding.etPlin.setText(String.format(Locale.US, "%.2f", pedido.plin))
        } else {
            binding.etPlin.setText("")
        }

        if (pedido?.efectivo != null && pedido.efectivo > 0) {
            binding.etEfectivo.setText(String.format(Locale.US, "%.2f", pedido.efectivo))
        } else {
            binding.etEfectivo.setText("")
        }

        val creditoInicial = if (pedido?.credito != null && pedido.credito > -1) {
            pedido.credito
        } else {
            totalPedido
        }

        binding.etCredito.setText(
            String.format(Locale.US, "%.2f", creditoInicial)
        )

        configurarListenersPago()
    }

    private fun validarPagos(): Boolean {
        val yape = binding.etYape.getDouble()
        val plin = binding.etPlin.getDouble()
        val efectivo = binding.etEfectivo.getDouble()

        val totalPagado = yape + plin + efectivo

        if (totalPagado > totalPedido) {
            mostrarErrorPago(
                "El total pagado (S/ %.2f) supera el total del pedido (S/ %.2f)"
                    .format(totalPagado, totalPedido)
            )
            return false
        }

        return true
    }

    private fun mostrarErrorPago(mensaje: String) {
        Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG)
            .setBackgroundTint(requireContext().getColor(R.color.red))
            .setTextColor(requireContext().getColor(android.R.color.white))
            .show()

        binding.cardMetodosPagos.animate()
            .translationX(-10f)
            .setDuration(50)
            .withEndAction {
                binding.cardMetodosPagos.animate()
                    .translationX(10f)
                    .setDuration(50)
                    .withEndAction {
                        binding.cardMetodosPagos.animate()
                            .translationX(0f)
                            .setDuration(50)
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun configurarListenersPago() {
        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                recalcularCredito()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }

        binding.etYape.addTextChangedListener(watcher)
        binding.etPlin.addTextChangedListener(watcher)
        binding.etEfectivo.addTextChangedListener(watcher)
    }

    private fun recalcularCredito() {
        val yape = binding.etYape.getDouble()
        val plin = binding.etPlin.getDouble()
        val efectivo = binding.etEfectivo.getDouble()

        val totalPagado = yape + plin + efectivo
        val credito = totalPedido - totalPagado

        binding.etCredito.setText(
            String.format(Locale.US, "%.2f", credito.coerceAtLeast(0.0))
        )

        val color = if (totalPagado > totalPedido) {
            R.color.red
        } else {
            R.color.red
        }

        binding.tilCredito.setBoxStrokeColor(
            requireContext().getColor(color)
        )
    }

    private fun setupListeners() {
        binding.btnCerrar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnGuardar.setOnClickListener {
            if (!validarPagos()) {
                return@setOnClickListener
            }

            binding.root.clearFocus()
            ocultarTeclado()

            binding.root.postDelayed({
                detallePedidoViewModel.actualizarPagos(
                    yape = binding.etYape.getDouble(),
                    plin = binding.etPlin.getDouble(),
                    efectivo = binding.etEfectivo.getDouble(),
                    credito = binding.etCredito.getDouble()
                )

                detallePedidoViewModel.actualizarPedido()
            }, 100)
        }

        binding.btnAgregarProducto.setOnClickListener {
            mostrarCardAgregarProducto()
        }

        binding.btnCerrarCardProducto.setOnClickListener {
            ocultarCardAgregarProducto()
        }

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
            detallePedidoViewModel.agregarProducto(
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
            tvNombreCliente.text = pedido.cliente?.nombreContacto ?: "Sin nombre"
            tvDireccion.text = pedido.cliente?.direccion ?: "Sin dirección"
            tvTelefono.text = pedido.cliente?.telefono ?: "Sin teléfono"
            tvVendedor.text = pedido.vendedor?.nombre ?: "Sin vendedor"
            tvEstado.text = pedido.estado ?: "Sin estado"
            tvObservaciones.text = pedido.observaciones ?: "Sin observaciones"
            tvSubtotal.text = String.format("S/ %.2f", pedido.subtotal ?: 0.0)
            tvIgv.text = String.format("S/ %.2f", pedido.igv ?: 0.0)
            tvTotal.text = String.format("S/ %.2f", pedido.total ?: 0.0)
            detallePedidoAdapter.submitList(pedido.detallePedidos)
        }
    }

    private fun mostrarCardAgregarProducto() {
        binding.cardAgregarProducto.visibility = View.VISIBLE

        binding.cardAgregarProducto.alpha = 0f
        binding.cardAgregarProducto.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

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

            detallePedidoViewModel.filtrarProductos(query)

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
                    detallePedidoViewModel.limpiarProductoSeleccionado()
                }
            }
        }

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

    private fun updateProductosList(productos: List<Producto>) {
        productoAdapter.actualizarProductos(productos)
    }

    private fun updateProductosEnPedido(productos: List<DetallePedido>) {
        android.util.Log.d("DetallePedido", "📦 Actualizando: ${productos.size} items")

        val nuevaLista = productos.map { it.copy() }

        detallePedidoAdapter.submitList(nuevaLista) {
            android.util.Log.d("DetallePedido", "✅ Lista actualizada en adapter")

            if (nuevaLista.isNotEmpty()) {
                binding.rvProductos.post {
                    binding.rvProductos.smoothScrollToPosition(0)
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnGuardar.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
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

        // ✅ MODIFICADO: Inicializar valores y calcular subtotal
        isUpdatingFields = true
        binding.etCantidad.setText("0.5")
        binding.etPrecio.setText(String.format("%.2f", producto.precio))

        val subtotalInicial = 0.5 * producto.precio
        binding.tvSubtotalProducto.setText(String.format("%.2f", subtotalInicial))
        isUpdatingFields = false
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    private fun formatearPrecio(precio: Double): String {
        val formato = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
        return formato.format(precio)
    }

    private fun showSuccessAnimation() {
        showSuccessSnackbar("¡Pedido actualizado exitosamente!")

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