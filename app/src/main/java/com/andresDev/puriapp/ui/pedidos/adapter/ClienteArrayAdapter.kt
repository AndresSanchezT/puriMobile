package com.andresDev.puriapp.ui.pedidos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import com.andresDev.puriapp.data.model.Cliente
import com.andresDev.puriapp.databinding.ItemClienteDropdownBinding

class ClienteArrayAdapter(
    context: Context
) : ArrayAdapter<Cliente>(context, 0) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: ItemClienteDropdownBinding
        val view: View

        if (convertView == null) {
            binding = ItemClienteDropdownBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            view = binding.root
            view.tag = binding
        } else {
            view = convertView
            binding = view.tag as ItemClienteDropdownBinding
        }

        val cliente = getItem(position)

        if (cliente != null) {
            binding.tvNombreContacto.text = cliente.nombreContacto
            binding.tvDireccion.text = cliente.direccion
        }

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                return FilterResults()
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                // No hacer nada - el filtrado lo maneja el ViewModel
            }
        }
    }

    fun actualizarClientes(nuevosClientes: List<Cliente>) {
        clear()
        addAll(nuevosClientes)
        notifyDataSetChanged()
    }
}