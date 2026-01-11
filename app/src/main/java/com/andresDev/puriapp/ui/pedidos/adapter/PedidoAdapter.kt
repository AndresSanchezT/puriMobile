package com.andresDev.puriapp.ui.pedidos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.andresDev.puriapp.data.model.PedidoListaReponse
import com.andresDev.puriapp.databinding.ItemPedidoBinding

class PedidoAdapter(
    private val onCheckClick: (PedidoListaReponse) -> Unit,
    private val onInfoClick: (Long) -> Unit
) : ListAdapter<PedidoListaReponse, PedidoViewHolder>(PedidoDiffCallback()) {

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

    class PedidoDiffCallback : DiffUtil.ItemCallback<PedidoListaReponse>() {
        override fun areItemsTheSame(oldItem: PedidoListaReponse, newItem: PedidoListaReponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PedidoListaReponse, newItem: PedidoListaReponse): Boolean {
            return oldItem == newItem
        }
    }
}