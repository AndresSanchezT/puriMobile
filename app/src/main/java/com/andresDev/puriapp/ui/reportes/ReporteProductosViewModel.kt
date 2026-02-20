package com.andresDev.puriapp.ui.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andresDev.puriapp.data.model.ReporteProductoDTO
import com.andresDev.puriapp.data.repository.ReportesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ====================================================================
// UI STATES
// ====================================================================

sealed class ReporteUiState {
    object Loading : ReporteUiState()
    data class Success(val productos: List<ReporteProductoDTO>) : ReporteUiState()
    data class Error(val message: String) : ReporteUiState()
    object Empty : ReporteUiState()
}

// ====================================================================
// VIEWMODEL
// ====================================================================

@HiltViewModel
class ReporteProductosViewModel @Inject constructor(
    private val repository: ReportesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReporteUiState>(ReporteUiState.Loading)
    val uiState: StateFlow<ReporteUiState> = _uiState.asStateFlow()

    /**
     * Carga el reporte de productos según el día seleccionado
     * @param dia "hoy" o "manana"
     */
    fun cargarReporte(dia: String) {
        viewModelScope.launch {
            _uiState.value = ReporteUiState.Loading

            // ✅ CORRECCIÓN: Manejar el Result
            val resultado = when (dia) {
                "hoy" -> repository.obtenerReporteHoy()
                "manana" -> repository.obtenerReporteManana()
                else -> Result.success(emptyList()) // ✅ Especificar el tipo
            }

            // ✅ Manejar success y failure
            resultado.onSuccess { productos ->
                if (productos.isEmpty()) {
                    _uiState.value = ReporteUiState.Empty
                } else {
                    _uiState.value = ReporteUiState.Success(productos)
                }
            }

            resultado.onFailure { error ->
                _uiState.value = ReporteUiState.Error(
                    error.message ?: "Error al cargar el reporte"
                )
            }
        }
    }
}