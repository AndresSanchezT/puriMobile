package com.andresDev.puriapp.ui.pedidos.adapter

import androidx.recyclerview.widget.RecyclerView
import com.andresDev.puriapp.data.model.DetallePedido
import com.andresDev.puriapp.databinding.ItemDetallePedidoBinding

class DetallePedidoViewHolder(
    private val binding: ItemDetallePedidoBinding,
    private val onEliminar: (Long?) -> Unit,
    private val onCantidadChanged: (Long?, Double) -> Unit,
    private val onPrecioChanged: (Long?, Double) -> Unit,
    private val onSubtotalChanged: (Long?, Double) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(detallePedido: DetallePedido) {
        binding.apply {
            tvProductoNombre.text = detallePedido.producto.nombre
            tvUnidadMedida.text = detallePedido.producto.unidadMedida
            etCantidad.setText(detallePedido.cantidad.toString())

            etPrecio.setText(detallePedido.precioUnitario.toString())
            etSubtotal.setText(detallePedido.subtotal.toString())

            btnEliminar.setOnClickListener {
                animateRemove(detallePedido)
            }

            // ⭐ Listener para cambios en cantidad
            etCantidad.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val nuevaCantidad = etCantidad.text.toString().toDoubleOrNull()
                    if (nuevaCantidad != null && nuevaCantidad != detallePedido.cantidad) {
                        onCantidadChanged(detallePedido.producto.id, nuevaCantidad)
                    }
                }
            }

            // ⭐ Listener para cambios en precio
            etPrecio.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val nuevoPrecio = etPrecio.text.toString().toDoubleOrNull()
                    if (nuevoPrecio != null && nuevoPrecio != detallePedido.precioUnitario) {
                        onPrecioChanged(detallePedido.producto.id, nuevoPrecio)
                    }
                }
            }

            // ⭐ NUEVO: Listener para cambios en subtotal
            etSubtotal.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val nuevoSubtotal = etSubtotal.text.toString()
                        .replace("S/", "")
                        .replace(",", "")
                        .trim()
                        .toDoubleOrNull()

                    if (nuevoSubtotal != null && nuevoSubtotal != detallePedido.subtotal) {
                        onSubtotalChanged(detallePedido.producto.id, nuevoSubtotal)
                    }
                }
            }
        }
    }

    private fun animateRemove(productoPedido: DetallePedido) {
        binding.root.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(200)
            .withEndAction {
                onEliminar(productoPedido.producto.id)
                // Resetear valores para cuando se reutilice el ViewHolder
                binding.root.alpha = 1f
                binding.root.scaleX = 1f
                binding.root.scaleY = 1f
            }
            .start()
    }
}