package com.andresDev.puriapp.ui.pedidos.adapter

import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlin.math.abs

class DragDropCallback(
    private val adapter: PedidoAdapter,
    private val rootView: View
) : ItemTouchHelper.Callback() {

    private var currentSnackbar: Snackbar? = null

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.bindingAdapterPosition
        val toPosition = target.bindingAdapterPosition

        adapter.onItemMove(fromPosition, toPosition)

        updatePositionSnackbar(toPosition)

        // Actualizar badge con la nueva posición
        if (viewHolder is PedidoViewHolder) {
            viewHolder.showPositionBadge(toPosition)
        }

        return true
    }

    private fun updatePositionSnackbar(position: Int) {
        currentSnackbar?.setText("📍 Posición: ${position + 1}")
    }

    private fun showPositionSnackbar(position: Int) {
        currentSnackbar = Snackbar.make(
            rootView,
            "📍 Posición: ${position + 1}",
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setBackgroundTint(0xFF10B981.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            show()
        }
    }
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // No implementamos swipe
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    // ✅ CLAVE 1: Reducir el umbral de intercambio
    override fun getMoveThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.2f  // Solo necesita 20% de superposición (default es 0.5 = 50%)
    }

    // ✅ CLAVE 2: Ajustar el umbral de interpolación para movimiento más suave
    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.2f
    }

    // ✅ CLAVE 3: Permitir selección de target mientras arrastra
    override fun chooseDropTarget(
        selected: RecyclerView.ViewHolder,
        dropTargets: MutableList<RecyclerView.ViewHolder>,
        curX: Int,
        curY: Int
    ): RecyclerView.ViewHolder? {
        val selectedBottom = selected.itemView.bottom
        val selectedTop = selected.itemView.top
        val selectedHeight = selectedBottom - selectedTop

        var bestTarget: RecyclerView.ViewHolder? = null
        var bestDistance = Int.MAX_VALUE

        for (target in dropTargets) {
            if (target === selected) continue

            val targetTop = target.itemView.top
            val targetBottom = target.itemView.bottom
            val targetHeight = targetBottom - targetTop

            // Calcular la distancia vertical
            val distance = when {
                curY < targetTop -> targetTop - curY
                curY > targetBottom -> curY - targetBottom
                else -> 0 // Está dentro del target
            }

            // ✅ Verificar si hay suficiente superposición (20%)
            val overlap = when {
                selectedBottom > targetTop && selectedTop < targetBottom -> {
                    // Hay superposición
                    val overlapHeight = minOf(selectedBottom, targetBottom) - maxOf(selectedTop, targetTop)
                    val overlapPercentage = overlapHeight.toFloat() / selectedHeight
                    overlapPercentage >= 0.2f
                }
                else -> false
            }

            if (overlap && distance < bestDistance) {
                bestDistance = distance
                bestTarget = target
            }
        }

        return bestTarget ?: super.chooseDropTarget(selected, dropTargets, curX, curY)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            viewHolder?.itemView?.alpha = 0.5f
            viewHolder?.itemView?.elevation = 8f

            // Mostrar badge inicial
            if (viewHolder is PedidoViewHolder) {
                val position = viewHolder.bindingAdapterPosition
                viewHolder.showPositionBadge(position)
            }

            val position = viewHolder?.bindingAdapterPosition ?: 0
            showPositionSnackbar(position)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        viewHolder.itemView.alpha = 1.0f
        viewHolder.itemView.elevation = 0f

        // Ocultar badge al soltar
        if (viewHolder is PedidoViewHolder) {
            viewHolder.hidePositionBadge()
        }

        currentSnackbar?.dismiss()
        currentSnackbar = null

        adapter.onDragCompleted()
    }

    // ✅ CLAVE 4: Habilitar scroll automático mientras arrastra
    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    // ✅ CLAVE 5: Velocidad de auto-scroll más rápida
    override fun getAnimationDuration(
        recyclerView: RecyclerView,
        animationType: Int,
        animateDx: Float,
        animateDy: Float
    ): Long {
        return 150L  // Animación más rápida (default es 250ms)
    }
}

