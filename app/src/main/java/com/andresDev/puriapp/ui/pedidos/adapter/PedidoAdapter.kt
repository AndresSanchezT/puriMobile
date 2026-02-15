// PedidoAdapter.kt
package com.andresDev.puriapp.ui.pedidos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andresDev.puriapp.data.model.PedidoListaReponse
import com.andresDev.puriapp.databinding.ItemPedidoBinding
import java.util.Collections

class PedidoAdapter(
    private val isAdminMode: Boolean,
    private val onCheckClick: (PedidoListaReponse) -> Unit,
    private val onInfoClick: (Long) -> Unit,
    private val onDeleteClick: (PedidoListaReponse) -> Unit,
    private val onOrderChanged: () -> Unit
) : RecyclerView.Adapter<PedidoViewHolder>() {

    private var pedidosList = mutableListOf<PedidoListaReponse>()
    private var hayChangesPendientes = false

    private var isNumericEditMode = false
    private val orderNumbers = mutableMapOf<Long, Int>()

    fun submitList(list: List<PedidoListaReponse>?) {
        val sortedList = list?.sortedBy { it.orden ?: Int.MAX_VALUE } ?: emptyList()

        if (!hayChangesPendientes) {
            pedidosList.clear()
            pedidosList.addAll(sortedList)
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val binding = ItemPedidoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PedidoViewHolder(
            binding,
            isAdminMode,
            onCheckClick,
            onInfoClick,
            onDeleteClick,
            { pedidoId, numero ->
                orderNumbers[pedidoId] = numero
                hayChangesPendientes = true
                onOrderChanged()
            }
        )
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedido = pedidosList[position]

        // ✅ IMPORTANTE: Pasar el número actual para evitar re-bind
        val currentNumber = pedido.id?.let { orderNumbers[it] }
        holder.bind(pedido, position + 1, isNumericEditMode, currentNumber)
    }

    // ✅ NUEVO: Sobrescribir para evitar rebind completo en payloads
    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // Si hay payloads, solo actualizar lo necesario (no tocar EditText)
            // No hacer nada, mantener el estado del EditText
        }
    }

    override fun getItemCount(): Int = pedidosList.size

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(pedidosList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(pedidosList, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun onDragCompleted() {
        hayChangesPendientes = true
        onOrderChanged()
    }

    fun enableNumericEditMode() {
        isNumericEditMode = true
        orderNumbers.clear()
        notifyDataSetChanged()
    }

    fun disableNumericEditMode() {
        isNumericEditMode = false
        orderNumbers.clear()
        notifyDataSetChanged()
    }

    fun getUpdatedListFromNumbers(): List<PedidoListaReponse> {
        val numberToPedido = mutableMapOf<Int, PedidoListaReponse>()
        val pedidosWithoutNumber = mutableListOf<PedidoListaReponse>()

        for (pedido in pedidosList) {
            val numero = pedido.id?.let { orderNumbers[it] }
            if (numero != null && numero > 0) {
                numberToPedido[numero] = pedido
            } else {
                pedidosWithoutNumber.add(pedido)
            }
        }

        val sortedList = mutableListOf<PedidoListaReponse>()
        val maxNumber = numberToPedido.keys.maxOrNull() ?: 0

        for (i in 1..maxNumber) {
            numberToPedido[i]?.let { sortedList.add(it) }
        }

        sortedList.addAll(pedidosWithoutNumber)

        return sortedList.mapIndexed { index, pedido ->
            pedido.copy(orden = index)
        }
    }

    fun getUpdatedList(): List<PedidoListaReponse> {
        return if (isNumericEditMode && orderNumbers.isNotEmpty()) {
            getUpdatedListFromNumbers()
        } else {
            pedidosList.mapIndexed { index, pedido ->
                pedido.copy(orden = index)
            }
        }
    }

    fun hasPendingChanges(): Boolean = hayChangesPendientes

    fun resetPendingChanges() {
        hayChangesPendientes = false
        orderNumbers.clear()
    }
}