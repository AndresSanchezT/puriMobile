// PedidoViewHolder.kt
package com.andresDev.puriapp.ui.pedidos.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.andresDev.puriapp.R
import com.andresDev.puriapp.data.model.PedidoListaReponse
import com.andresDev.puriapp.databinding.ItemPedidoBinding

class PedidoViewHolder(
    private val binding: ItemPedidoBinding,
    private val isAdminMode: Boolean,
    private val onCheckClick: (PedidoListaReponse) -> Unit,
    private val onInfoClick: (Long) -> Unit,
    private val onDeleteClick: (PedidoListaReponse) -> Unit,
    private val onOrderNumberChanged: (Long, Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private var currentPedido: PedidoListaReponse? = null
    private var textWatcher: TextWatcher? = null
    private var isSettingText = false

    fun bind(
        pedidoListaReponse: PedidoListaReponse,
        orderNumber: Int,
        isNumericEditMode: Boolean,
        currentNumber: Int? = null
    ) {
        currentPedido = pedidoListaReponse

        binding.apply {
            badgePosicion.visibility = View.GONE

            if (isNumericEditMode) {
                cardNumeroOrden.visibility = View.VISIBLE

                // ✅ SIEMPRE remover el watcher anterior y crear uno nuevo para el pedido actual
                textWatcher?.let { etNumeroOrden.removeTextChangedListener(it) }
                textWatcher = null

                // ✅ Primero setear el texto ANTES de agregar el watcher
                isSettingText = true
                if (currentNumber != null && currentNumber > 0) {
                    etNumeroOrden.setText(currentNumber.toString())
                    etNumeroOrden.setSelection(etNumeroOrden.text?.length ?: 0)
                } else {
                    etNumeroOrden.text?.clear()
                }
                isSettingText = false

                // ✅ Crear watcher atado al pedido actual DESPUÉS de setear el texto
                val watcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if (isSettingText) return
                        val numero = s?.toString()?.toIntOrNull() ?: 0
                        pedidoListaReponse.id?.let { id ->
                            onOrderNumberChanged(id, numero)
                        }
                    }
                }
                textWatcher = watcher
                etNumeroOrden.addTextChangedListener(watcher)

            } else {
                // Salir del modo edición: limpiar todo
                textWatcher?.let { etNumeroOrden.removeTextChangedListener(it) }
                textWatcher = null
                isSettingText = true
                etNumeroOrden.text?.clear()
                isSettingText = false
                cardNumeroOrden.visibility = View.GONE
            }

            tvNombreCliente.text = "#$orderNumber - ${pedidoListaReponse.nombreCliente}"
            tvDireccion.text = pedidoListaReponse.direccion
            tvDeuda.text = pedidoListaReponse.total.toString()
            tvDeuda.setTextColor(itemView.context.getColor(android.R.color.holo_green_light))

            val estado = pedidoListaReponse.estado?.lowercase() ?: "registrado"
            when (estado) {
                "registrado" -> colorIndicator.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.primaryDark)
                )
                "entregado" -> colorIndicator.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                )
                "anulado", "cancelado" -> colorIndicator.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
                )
                else -> colorIndicator.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.primaryDark)
                )
            }

            if (isAdminMode) {
                btnCheckCard.visibility = View.GONE
                btnDeleteCard.visibility = View.VISIBLE
                btnDelete.setOnClickListener { onDeleteClick(pedidoListaReponse) }
            } else {
                if (estado == "registrado") {
                    btnCheckCard.visibility = View.VISIBLE
                    btnCheck.setOnClickListener { onCheckClick(pedidoListaReponse) }
                } else {
                    btnCheckCard.visibility = View.GONE
                }
                btnDeleteCard.visibility = View.GONE
            }

            btnInfo.setOnClickListener {
                pedidoListaReponse.id?.let { id -> onInfoClick(id) }
            }
        }
    }

    fun showPositionBadge(position: Int) {
        binding.badgePosicion.visibility = View.VISIBLE
        binding.tvPosicionBadge.text = "Pos. ${position + 1}"
        binding.badgePosicion.apply {
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .scaleX(1.1f).scaleY(1.1f).setDuration(100)
                .withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()
        }
    }

    fun hidePositionBadge() {
        binding.badgePosicion.animate()
            .alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(150)
            .withEndAction {
                binding.badgePosicion.visibility = View.GONE
                binding.badgePosicion.alpha = 1f
            }.start()
    }
}