package com.andresDev.puriapp.ui.clientes.adapter

import androidx.recyclerview.widget.RecyclerView
import com.andresDev.puriapp.data.model.Cliente
import com.andresDev.puriapp.databinding.ItemClienteBinding
import com.andresDev.puriapp.R

class ClienteViewHolder(private val binding: ItemClienteBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(cliente: Cliente) {
        binding.tvBodega.text = cliente.nombreNegocio
        binding.tvCliente.text = cliente.nombreContacto
        binding.tvTelefono.text = cliente.telefono
        binding.tvDireccion.text = cliente.direccion


//        val icono = if (cliente.visitado) R.drawable.ic_check else R.drawable.ic_close
        binding.ivEstadoVisita.setImageResource(R.drawable.ic_check)
    }
}
