package com.andresDev.puriapp.ui.reportes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope

import com.andresDev.puriapp.data.model.ReporteProductoDTO
import com.andresDev.puriapp.databinding.FragmentReporteProductosBinding
import com.andresDev.puriapp.ui.reportes.adapter.ReporteProductoAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReporteProductosFragment : Fragment() {

    private var _binding: FragmentReporteProductosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReporteProductosViewModel by viewModels()

    private val adapter = ReporteProductoAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReporteProductosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupChipGroup()
        observeViewModel()

        // Cargar datos de HOY por defecto
        viewModel.cargarReporte("hoy")
    }

    private fun setupRecyclerView() {
        binding.recyclerViewReporte.adapter = adapter
    }

    private fun setupChipGroup() {
        binding.chipGroupDia.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                binding.chipHoy.id -> {
                    binding.tvSubtitulo.text = "Productos de Hoy"
                    viewModel.cargarReporte("hoy")
                }
                binding.chipManana.id -> {
                    binding.tvSubtitulo.text = "Productos de Mañana"
                    viewModel.cargarReporte("manana")
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is ReporteUiState.Loading -> showLoading()
                    is ReporteUiState.Success -> showData(state.productos)
                    is ReporteUiState.Error -> showError(state.message)
                    is ReporteUiState.Empty -> showEmpty()
                }
            }
        }
    }

    private fun showLoading() {
        with(binding) {
            progressBar.isVisible = true
            recyclerViewReporte.isVisible = false
            layoutEmpty.isVisible = false
        }
    }

    private fun showData(productos: List<ReporteProductoDTO>) {
        with(binding) {
            progressBar.isVisible = false
            recyclerViewReporte.isVisible = true
            layoutEmpty.isVisible = false
        }

        adapter.submitList(productos)
    }

    private fun showError(message: String) {
        with(binding) {
            progressBar.isVisible = false
            recyclerViewReporte.isVisible = false
            layoutEmpty.isVisible = false
        }

        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Reintentar") {
                val dia = if (binding.chipHoy.isChecked) "hoy" else "manana"
                viewModel.cargarReporte(dia)
            }
            .show()
    }

    private fun showEmpty() {
        with(binding) {
            progressBar.isVisible = false
            recyclerViewReporte.isVisible = false
            layoutEmpty.isVisible = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}