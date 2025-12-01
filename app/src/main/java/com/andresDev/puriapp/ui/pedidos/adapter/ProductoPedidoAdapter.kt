package com.andresDev.puriapp.ui.pedidos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.andresDev.puriapp.R
import com.andresDev.puriapp.data.model.DetallePedido
import com.andresDev.puriapp.databinding.ItemProductoPedidoBinding
import java.text.NumberFormat
import java.util.Locale

class ProductoPedidoAdapter(
    private val onCantidadChanged: (Long?, Int) -> Unit,
    private val onEliminar: (Long?) -> Unit,
    private val onIncrementar: (Long?) -> Unit,
    private val onDecrementar: (Long?) -> Unit
) : ListAdapter<DetallePedido, ProductoPedidoAdapter.ProductoPedidoViewHolder>(ProductoPedidoDiffCallback()) {

    // ========== AGREGADO: Variable para controlar animaciones ==========
    private var lastPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoPedidoViewHolder {
        val binding = ItemProductoPedidoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductoPedidoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductoPedidoViewHolder, position: Int) {
        holder.bind(getItem(position))

        // ========== AGREGADO: Llamada a animación de entrada ==========
        setAnimation(holder.itemView, position)
    }

    // ========== AGREGADO: Función de animación de entrada ==========
    private fun setAnimation(view: android.view.View, position: Int) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(view.context, R.anim.item_slide_in)
            view.startAnimation(animation)
            lastPosition = position
        }
    }

    // ========== AGREGADO: Limpiar animaciones cuando se desadjunta ==========
    override fun onViewDetachedFromWindow(holder: ProductoPedidoViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }

    inner class ProductoPedidoViewHolder(
        private val binding: ItemProductoPedidoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(productoPedido: DetallePedido) {
            with(binding) {
                tvNombreProducto.text = productoPedido.producto.nombre
                tvCantidad.text = "x${productoPedido.cantidad}" // MODIFICADO: Formato más compacto
                tvPrecioUnitario.text = "${formatearPrecio(productoPedido.producto.precio)} c/u" // AGREGADO: Precio unitario
                tvPrecioItem.text = formatearPrecio(productoPedido.precioTotal)

                // ========== MODIFICADO: Botón eliminar con animación ==========
                btnEliminar.setOnClickListener {
                    animateRemove(productoPedido)
                }
            }
        }

        // ========== AGREGADO: Función de animación de eliminación ==========
        private fun animateRemove(productoPedido: DetallePedido) {
            binding.root.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(200)
                .withEndAction {
                    onEliminar(productoPedido.producto.id)
                    // Resetear valores para cuando se reutilice el ViewHolder
                    binding.root.alpha = 1f
                    binding.root.scaleX = 1f
                    binding.root.scaleY = 1f
                }
                .start()
        }

        private fun formatearPrecio(precio: Double): String {
            val formato = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
            return formato.format(precio)
        }
    }

    class ProductoPedidoDiffCallback : DiffUtil.ItemCallback<DetallePedido>() {
        override fun areItemsTheSame(oldItem: DetallePedido, newItem: DetallePedido): Boolean {
            return oldItem.producto.id == newItem.producto.id
        }

        override fun areContentsTheSame(oldItem: DetallePedido, newItem: DetallePedido): Boolean {
            return oldItem == newItem
        }
    }
}