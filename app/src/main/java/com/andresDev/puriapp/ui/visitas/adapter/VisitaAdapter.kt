package com.andresDev.puriapp.ui.visitas.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.data.model.Visita
import com.andresDev.puriapp.databinding.ItemVisitaBinding

class VisitaAdapter (
    private val onCheckClick: (Visita) -> Unit,
    private val onInfoClick: (Visita) -> Unit
): ListAdapter<Visita, VisitaViewHolder>(VisitaDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VisitaViewHolder {
        val binding = ItemVisitaBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return VisitaViewHolder(binding,onCheckClick,onInfoClick)
    }

    override fun onBindViewHolder(
        holder: VisitaViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }


    class VisitaDiffCallback : DiffUtil.ItemCallback<Visita>() {
        override fun areItemsTheSame(oldItem: Visita, newItem: Visita): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Visita, newItem: Visita): Boolean {
            return oldItem == newItem
        }
    }

}