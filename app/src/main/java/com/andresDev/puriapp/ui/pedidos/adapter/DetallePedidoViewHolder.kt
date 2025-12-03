package com.andresDev.puriapp.ui.pedidos.adapter

import androidx.recyclerview.widget.RecyclerView
import com.andresDev.puriapp.data.model.DetallePedido
import com.andresDev.puriapp.databinding.ItemDetallePedidoBinding

class DetallePedidoViewHolder(
    private val binding: ItemDetallePedidoBinding,
//    private val onCheckClick: (DetallePedido) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(detallePedido: DetallePedido) {
        binding.apply {
            tvProductoNombre.text = detallePedido.producto.nombre
            tvUnidadMedida.text = detallePedido.producto.unidadMedida
            etCantidad.setText(detallePedido.cantidad.toString())
            etPrecio.setText(detallePedido.precioUnitario.toString())
            tvSubtotal.text = detallePedido.subtotal.toString()

//            btnEliminar
        }
    }
}