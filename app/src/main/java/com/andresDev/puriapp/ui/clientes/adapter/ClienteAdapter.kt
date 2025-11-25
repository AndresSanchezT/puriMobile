package com.andresDev.puriapp.ui.clientes.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.andresDev.puriapp.data.model.Cliente
import com.andresDev.puriapp.databinding.ItemClienteBinding

class ClienteAdapter : ListAdapter<Cliente, ClienteViewHolder>(ClienteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
        val binding = ItemClienteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ClienteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ClienteDiffCallback : DiffUtil.ItemCallback<Cliente>() {
    override fun areItemsTheSame(oldItem: Cliente, newItem: Cliente) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Cliente, newItem: Cliente) = oldItem == newItem
}
