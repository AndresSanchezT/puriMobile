package com.andresDev.puriapp.ui.pedidos.adapter

import androidx.recyclerview.widget.RecyclerView
import com.andresDev.puriapp.data.model.PedidoDetallesGeneralesResponse
import com.andresDev.puriapp.databinding.ItemDetallePedidoBinding

class DetallePedidoViewHolder(
    private val binding: ItemDetallePedidoBinding,
//    private val onCheckClick: (DetallePedido) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(detallePedido: PedidoDetallesGeneralesResponse.DetallePedidoDTO) {
        binding.apply {
            tvProductoNombre.text = detallePedido.producto.nombre
            tvUnidadMedida.text = detallePedido.producto.unidadMedida
            etCantidad.setText(detallePedido.cantidad.toString())
            etPrecio.setText(detallePedido.precioUnitario.toString())
            etSubtotal.setText(detallePedido.subtotal.toString())

//            btnEliminar
        }
    }
}