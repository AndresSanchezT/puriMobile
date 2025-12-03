package com.andresDev.puriapp.ui.pedidos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.andresDev.puriapp.databinding.ItemDetallePedidoBinding
import com.andresDev.puriapp.data.model.PedidoDetallesGeneralesResponse.DetallePedidoDTO


class DetallePedidoAdapter (
//    private val onCheckClick: (DetallePedido) -> Unit
): ListAdapter<DetallePedidoDTO, DetallePedidoViewHolder>(DetallePedidoDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DetallePedidoViewHolder {
        val binding = ItemDetallePedidoBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return DetallePedidoViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: DetallePedidoViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    class DetallePedidoDiffCallback : DiffUtil.ItemCallback<DetallePedidoDTO>() {
        override fun areItemsTheSame(
            oldItem: DetallePedidoDTO,
            newItem: DetallePedidoDTO
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: DetallePedidoDTO,
            newItem: DetallePedidoDTO
        ): Boolean {
            return oldItem == newItem
        }
    }
}