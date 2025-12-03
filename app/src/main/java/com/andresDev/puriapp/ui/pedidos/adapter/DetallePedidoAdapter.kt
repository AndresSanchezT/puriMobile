package com.andresDev.puriapp.ui.pedidos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.andresDev.puriapp.data.model.DetallePedido
import com.andresDev.puriapp.databinding.ItemDetallePedidoBinding


class DetallePedidoAdapter (
//    private val onCheckClick: (DetallePedido) -> Unit
): ListAdapter<DetallePedido, DetallePedidoViewHolder>(DetallePedidoDiffCallback()) {

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

    class DetallePedidoDiffCallback : DiffUtil.ItemCallback<DetallePedido>() {
        override fun areItemsTheSame(
            oldItem: DetallePedido,
            newItem: DetallePedido
        ): Boolean {
            return oldItem.producto.id == newItem.producto.id
        }

        override fun areContentsTheSame(
            oldItem: DetallePedido,
            newItem: DetallePedido
        ): Boolean {
            return oldItem == newItem
        }
    }
}