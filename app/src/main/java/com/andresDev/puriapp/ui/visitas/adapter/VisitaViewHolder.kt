package com.andresDev.puriapp.ui.visitas.adapter

import androidx.recyclerview.widget.RecyclerView
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.data.model.Visita
import com.andresDev.puriapp.databinding.ItemVisitaBinding

class VisitaViewHolder(
    private val binding: ItemVisitaBinding,
    private val onCheckClick: (Visita) -> Unit,
    private val onInfoClick: (Visita) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(visita : Visita) {
        binding.apply {
            tvNombreCliente.text = visita.cliente.nombreContacto
            tvDireccion.text = visita.cliente.direccion
            tvObservaciones.text = visita.observaciones
            tvEstado.text = visita.estado
            tvFecha.text = visita.fecha

            // Mostrar deuda según boolean
            if (visita.estado === "visitado") {
                tvEstado.text = "visitado"
                tvEstado.setTextColor(
                    itemView.context.getColor(android.R.color.holo_red_dark)
                )
            } else {
                tvEstado.text = "sin visitar"
                tvEstado.setTextColor(
                    itemView.context.getColor(android.R.color.holo_green_dark)
                )
            }

            // Click en botón check
            btnCheck.setOnClickListener {
                onCheckClick(visita)
            }

            // Click en botón info
            btnInfo.setOnClickListener {
                onInfoClick(visita)
            }
        }
    }
}