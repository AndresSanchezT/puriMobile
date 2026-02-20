
package com.andresDev.puriapp.ui.reportes.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.andresDev.puriapp.data.model.ReporteProductoDTO
import com.andresDev.puriapp.databinding.ItemReporteProductoBinding

import java.text.DecimalFormat

class ReporteProductoAdapter :
    ListAdapter<ReporteProductoDTO, ReporteProductoAdapter.ViewHolder>(ReporteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReporteProductoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemReporteProductoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val decimalFormat = DecimalFormat("#,##0.##")

        fun bind(producto: ReporteProductoDTO) {
            with(binding) {
                // Nombre del producto
                tvNombreProducto.text = producto.nombreProducto

                // Unidad de medida
                tvUnidadMedida.text = producto.unidadMedida

                // Total (formateado con separador de miles)
                val totalFormateado = decimalFormat.format(producto.totalProductos)
                tvTotal.text = totalFormateado
            }
        }
    }

    // DiffUtil para actualizaciones eficientes
    private class ReporteDiffCallback : DiffUtil.ItemCallback<ReporteProductoDTO>() {
        override fun areItemsTheSame(
            oldItem: ReporteProductoDTO,
            newItem: ReporteProductoDTO
        ): Boolean {
            return oldItem.nombreProducto == newItem.nombreProducto
        }

        override fun areContentsTheSame(
            oldItem: ReporteProductoDTO,
            newItem: ReporteProductoDTO
        ): Boolean {
            return oldItem == newItem
        }
    }
}