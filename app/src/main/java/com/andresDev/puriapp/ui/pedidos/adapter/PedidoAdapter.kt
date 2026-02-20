package com.andresDev.puriapp.ui.pedidos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.andresDev.puriapp.data.model.PedidoListaReponse
import com.andresDev.puriapp.databinding.ItemPedidoBinding
import java.util.Collections

class PedidoAdapter(
    private val isAdminMode: Boolean,
    private val onCheckClick: (PedidoListaReponse) -> Unit,
    private val onInfoClick: (Long) -> Unit,
    private val onDeleteClick: (PedidoListaReponse) -> Unit,
    private val onOrderChanged: () -> Unit
) : ListAdapter<PedidoListaReponse, PedidoViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PedidoListaReponse>() {
            override fun areItemsTheSame(old: PedidoListaReponse, new: PedidoListaReponse) =
                old.id == new.id
            override fun areContentsTheSame(old: PedidoListaReponse, new: PedidoListaReponse) =
                old == new
        }
    }

    // Lista mutable interna para drag & drop y filtrado en modo edición
    private val pedidosList = mutableListOf<PedidoListaReponse>()
    private var cachedPedidosSnapshot = listOf<PedidoListaReponse>()

    private var hayChangesPendientes = false
    private var isNumericEditMode = false
    val orderNumbers = mutableMapOf<Long, Int>()

    override fun submitList(list: List<PedidoListaReponse>?) {
        if (hayChangesPendientes) return // No pisar cambios pendientes

        val sorted = list?.sortedBy { it.orden ?: Int.MAX_VALUE } ?: emptyList()
        pedidosList.clear()
        pedidosList.addAll(sorted)
        super.submitList(sorted)
    }

    fun cacheCurrentPedidos() {
        cachedPedidosSnapshot = pedidosList.toList()
    }

    fun filterByName(query: String) {
        if (!isNumericEditMode) return

        val filtered = if (query.isBlank()) {
            cachedPedidosSnapshot.toList()
        } else {
            cachedPedidosSnapshot.filter { pedido ->
                pedido.nombreCliente?.contains(query, ignoreCase = true) == true ||
                        pedido.direccion?.contains(query, ignoreCase = true) == true
            }
        }

        pedidosList.clear()
        pedidosList.addAll(filtered)

        // submitList de ListAdapter usa DiffUtil internamente → no destruye ViewHolders innecesariamente
        super.submitList(filtered.toList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val binding = ItemPedidoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PedidoViewHolder(
            binding, isAdminMode,
            onCheckClick, onInfoClick, onDeleteClick
        ) { pedidoId, numero ->
            orderNumbers[pedidoId] = numero
            hayChangesPendientes = true
            onOrderChanged()
        }
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedido = getItem(position)
        val currentNumber = pedido.id?.let { orderNumbers[it] }
        holder.bind(pedido, position + 1, isNumericEditMode, currentNumber)
    }

    override fun onBindViewHolder(
        holder: PedidoViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            // Evitar rebind si hay payloads (preserva foco del EditText)
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition)
                Collections.swap(pedidosList, i, i + 1)
        } else {
            for (i in fromPosition downTo toPosition + 1)
                Collections.swap(pedidosList, i, i - 1)
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
        cacheCurrentPedidos()
        notifyDataSetChanged()
    }

    fun disableNumericEditMode() {
        isNumericEditMode = false
        orderNumbers.clear()
        cachedPedidosSnapshot = emptyList()
        // Restaurar lista original
        super.submitList(pedidosList.toList())
    }

    fun getUpdatedList(): List<PedidoListaReponse> {
        val allPedidos = cachedPedidosSnapshot.ifEmpty { pedidosList.toList() }
        val totalPedidos = allPedidos.size
        if (totalPedidos == 0) return emptyList()

        val idsConNumero = mutableSetOf<Long>()
        val asignados = mutableMapOf<Int, PedidoListaReponse>()

        val asignacionesOrdenadas = orderNumbers.entries
            .filter { it.value > 0 }
            .sortedBy { it.value }

        for ((pedidoId, numero) in asignacionesOrdenadas) {
            val pedido = allPedidos.firstOrNull { it.id == pedidoId } ?: continue
            var idx = (numero - 1).coerceIn(0, totalPedidos - 1)
            while (idx < totalPedidos && asignados.containsKey(idx)) idx++
            if (idx < totalPedidos) {
                asignados[idx] = pedido
                pedido.id?.let { idsConNumero.add(it) }
            }
        }

        val sinNumero = allPedidos.filter { it.id !in idsConNumero }.toMutableList()
        val resultado = arrayOfNulls<PedidoListaReponse>(totalPedidos)

        for ((idx, pedido) in asignados) resultado[idx] = pedido

        val iter = sinNumero.iterator()
        for (i in resultado.indices) {
            if (resultado[i] == null && iter.hasNext()) resultado[i] = iter.next()
        }

        return resultado.filterNotNull().mapIndexed { index, pedido ->
            pedido.copy(orden = index)
        }
    }

    fun hasPendingChanges() = hayChangesPendientes

    fun resetPendingChanges() {
        hayChangesPendientes = false
        orderNumbers.clear()
        cachedPedidosSnapshot = emptyList()
    }
}