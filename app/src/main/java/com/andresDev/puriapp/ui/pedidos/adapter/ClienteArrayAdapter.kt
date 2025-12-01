package com.andresDev.puriapp.ui.pedidos.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.andresDev.puriapp.data.model.Cliente

class ClienteArrayAdapter(
    context: Context
) : ArrayAdapter<Cliente>(context, android.R.layout.simple_dropdown_item_1line) {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val cliente = getItem(position)

        if (view is TextView && cliente != null) {
            view.text = cliente.nombreContacto
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
        clear()  // Limpia la lista interna del ArrayAdapter
        addAll(nuevosClientes)  // Agrega a la lista interna del ArrayAdapter
        notifyDataSetChanged()
    }
}