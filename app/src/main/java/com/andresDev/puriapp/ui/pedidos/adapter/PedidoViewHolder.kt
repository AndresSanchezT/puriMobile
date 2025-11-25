package com.andresDev.puriapp.ui.pedidos.adapter

import androidx.recyclerview.widget.RecyclerView
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.databinding.ItemPedidoBinding

class PedidoViewHolder(
    private val binding: ItemPedidoBinding,
    private val onCheckClick: (Pedido) -> Unit,
    private val onInfoClick: (Pedido) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(pedido: Pedido) {
        binding.apply {
            // Nombre del cliente
            tvNombreCliente.text = pedido.cliente.nombreContacto

            // Dirección
            tvDireccion.text = pedido.cliente.direccion

            // Deuda formateada
            val deudaFormateada = String.format("Deuda: S/ %.2f", pedido.cliente.tieneCredito ?: 0.0)
            tvDeuda.text = deudaFormateada

            // Cambiar color según si tiene deuda
            if (pedido.cliente.tieneCredito == true) {
                tvDeuda.setTextColor(
                    itemView.context.getColor(android.R.color.holo_red_dark)
                )
            } else {
                tvDeuda.text = "Sin deuda"
                tvDeuda.setTextColor(
                    itemView.context.getColor(android.R.color.holo_green_dark)
                )
            }

            // Click en botón check
            btnCheck.setOnClickListener {
                onCheckClick(pedido)
            }

            // Click en botón info
            btnInfo.setOnClickListener {
                onInfoClick(pedido)
            }
        }
    }
}