package com.andresDev.puriapp.ui.clientes

import androidx.lifecycle.ViewModel
import com.andresDev.puriapp.data.repository.ClienteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ClienteAddViewModel @Inject constructor(private val repository: ClienteRepository) : ViewModel() {


}