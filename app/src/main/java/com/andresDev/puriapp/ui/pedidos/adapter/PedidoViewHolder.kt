package com.andresDev.puriapp.ui.pedidos.adapter

import androidx.recyclerview.widget.RecyclerView
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.data.model.PedidoListaReponse
import com.andresDev.puriapp.databinding.ItemPedidoBinding

class PedidoViewHolder(
    private val binding: ItemPedidoBinding,
    private val onCheckClick: (PedidoListaReponse) -> Unit,
    private val onInfoClick: (Long) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(pedidoListaReponse: PedidoListaReponse) {
        binding.apply {
            // Nombre del cliente
            tvNombreCliente.text = pedidoListaReponse.nombreCliente

            // Dirección
            tvDireccion.text = pedidoListaReponse.direccion

            // Mostrar deuda según boolean
            if (pedidoListaReponse.tieneCredito == true) {
                tvDeuda.text = "1250.30"
                tvDeuda.setTextColor(
                    itemView.context.getColor(android.R.color.holo_red_dark)
                )
            } else {
                tvDeuda.text = "0.0"
                tvDeuda.setTextColor(
                    itemView.context.getColor(android.R.color.holo_green_dark)
                )
            }

            // Click en botón check
            btnCheck.setOnClickListener {
                onCheckClick(pedidoListaReponse)
            }

            // Botón Info - Pasa solo el ID
            btnInfo.setOnClickListener {
                pedidoListaReponse.id?.let { id ->
                    onInfoClick(id)  // ← Aquí se envía el ID al Fragment
                }
            }
        }
    }

}