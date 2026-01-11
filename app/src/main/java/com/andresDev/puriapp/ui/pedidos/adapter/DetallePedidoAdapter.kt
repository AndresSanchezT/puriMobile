package com.andresDev.puriapp.ui.pedidos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.andresDev.puriapp.data.model.DetallePedido
import com.andresDev.puriapp.databinding.ItemDetallePedidoBinding


class DetallePedidoAdapter(
    private val onEliminar: (Long?) -> Unit,
    private val onCantidadChanged: (Long?, Double) -> Unit,
    private val onPrecioChanged: (Long?, Double) -> Unit,
    private val onSubtotalChanged: (Long?, Double) -> Unit
) : ListAdapter<DetallePedido, DetallePedidoViewHolder>(DetallePedidoDiffCallback()) {


    // ========== AGREGADO: Variable para controlar animaciones ==========
    private var lastPosition = -1

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DetallePedidoViewHolder {
        val binding = ItemDetallePedidoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DetallePedidoViewHolder(binding,onEliminar,onCantidadChanged,onPrecioChanged,onSubtotalChanged)
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
            // ✅ Comparar por producto.id en lugar de detalle.id
            // porque los detalles nuevos no tienen id hasta que se guardan
            return oldItem.producto.id == newItem.producto.id
        }

        override fun areContentsTheSame(
            oldItem: DetallePedido,
            newItem: DetallePedido
        ): Boolean {
            // ✅ Comparar SOLO los campos que afectan la UI
            return oldItem.producto.id == newItem.producto.id &&
                    oldItem.producto.nombre == newItem.producto.nombre &&
                    oldItem.cantidad == newItem.cantidad &&
                    oldItem.precioUnitario == newItem.precioUnitario &&
                    oldItem.subtotal == newItem.subtotal &&
                    oldItem.producto.unidadMedida == newItem.producto.unidadMedida
        }
    }
}