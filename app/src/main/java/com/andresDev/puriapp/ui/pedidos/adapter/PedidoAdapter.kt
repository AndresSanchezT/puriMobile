package com.andresDev.puriapp.ui.clientes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.andresDev.puriapp.data.model.Cliente
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.databinding.ItemPedidoBinding
import com.andresDev.puriapp.ui.pedidos.adapter.PedidoViewHolder

class PedidoAdapter(
    private val onCheckClick: (Pedido) -> Unit,
    private val onInfoClick: (Pedido) -> Unit
) : ListAdapter<Pedido, PedidoViewHolder>(PedidoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val binding = ItemPedidoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PedidoViewHolder(binding, onCheckClick, onInfoClick)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PedidoDiffCallback : DiffUtil.ItemCallback<Pedido>() {
        override fun areItemsTheSame(oldItem: Pedido, newItem: Pedido): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Pedido, newItem: Pedido): Boolean {
            return oldItem == newItem
        }
    }
}