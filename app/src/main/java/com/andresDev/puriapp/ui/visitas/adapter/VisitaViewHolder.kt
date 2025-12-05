package com.andresDev.puriapp.ui.visitas.adapter

import androidx.recyclerview.widget.RecyclerView
import com.andresDev.puriapp.data.model.Visita
import com.andresDev.puriapp.databinding.ItemVisitaBinding
import com.andresDev.puriapp.utils.DateFormatter

class VisitaViewHolder(
    private val binding: ItemVisitaBinding,
    private val onCheckClick: (Visita) -> Unit,
    private val onInfoClick: (Visita) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(visita: Visita) {
        binding.apply {
            tvNombreCliente.text = visita.cliente.nombreContacto
            tvDireccion.text = visita.cliente.direccion
            tvObservaciones.text = visita.observaciones
            tvEstado.text = visita.estado

            // ========== MODIFICADO: Formatear la fecha correctamente ==========
            tvFecha.text = DateFormatter.formatearFechaHora(visita.fecha)

            // Alternativas de formato (elige la que más te guste):
            // tvFecha.text = DateFormatter.formatearSoloFecha(visita.fecha) // Solo fecha: "18/11/2025"
            // tvFecha.text = DateFormatter.formatearSoloHora(visita.fecha) // Solo hora: "6:59 PM"
            // tvFecha.text = DateFormatter.formatearFechaCompleta(visita.fecha) // Completa: "Martes, 18 de noviembre..."

            // Mostrar estado con colores
            when (visita.estado?.lowercase()) {
                "visitado" -> {
                    tvEstado.text = "Visitado"
                    tvEstado.setTextColor(
                        itemView.context.getColor(android.R.color.holo_green_dark)
                    )
                }
                "pendiente" -> {
                    tvEstado.text = "Pendiente"
                    tvEstado.setTextColor(
                        itemView.context.getColor(android.R.color.holo_orange_dark)
                    )
                }
                else -> {
                    tvEstado.text = visita.estado ?: "Sin estado"
                    tvEstado.setTextColor(
                        itemView.context.getColor(android.R.color.darker_gray)
                    )
                }
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