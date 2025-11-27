package com.andresDev.puriapp.ui.visitas

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andresDev.puriapp.data.model.Visita
import com.andresDev.puriapp.data.repository.VisitaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VisitaViewModel @Inject constructor(private val visitaRepository: VisitaRepository) : ViewModel(){

    private val _visitas = MutableStateFlow<List<Visita>>(emptyList())
    val visitas = _visitas.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    init {
        cargarVisitas()
    }

    fun cargarVisitas() {
        viewModelScope.launch {
            Log.d("DEBUG", "Iniciando carga de visitas...")
            _loading.value = true

            try {
                val lista = visitaRepository.obtenerVisitas()

                Log.d("DEBUG", "Datos recibidos: $lista")
                Log.d("DEBUG", "Cantidad: ${lista.size}")

                _visitas.value = lista

            } catch (e: Exception) {
                Log.e("DEBUG", "Error cargando visitas: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }


//    fun agregarVisita(visita: Visita) {
//        viewModelScope.launch {
//            try {
//                val nuevaVisita = visitaRepository.crearVisita(visita)
//                cargarVisitas() // Recargar para actualizar UI
//            } catch (e: Exception) {
//                Log.e("VisitaViewModel", "Error al agregar: ${e.message}")
//            }
//        }
//    }
//
//    fun actualizarVisita(visita: Visita) {
//        val index = _allVisitas.indexOfFirst { it.id == visita.id }
//        if (index != -1) _allVisitas[index] = visita
//        _visitas.value = _allVisitas.toList()
//    }


    //AÑADIR FILTRO SI ESTA VISITADO O NO
    fun filtrarVisita(query: String) {
        viewModelScope.launch {
            val filtrados = visitaRepository.buscarEnCache(query)
            _visitas.value = filtrados
        }
    }

}