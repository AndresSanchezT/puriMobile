// ui/login/LoginViewModel.kt
package com.andresDev.puriapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andresDev.puriapp.data.api.AuthApi
import com.andresDev.puriapp.data.manager.TokenManager
import com.andresDev.puriapp.data.model.LoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(usuario: String, password: String) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading

                val response = authApi.login(LoginRequest(usuario, password))

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    // Guardar token y datos del usuario
                    tokenManager.saveToken(loginResponse.token)
                    tokenManager.saveUserData(
                        loginResponse.id,
                        loginResponse.username,
                        loginResponse.role,
                        loginResponse.nombre
                    )

                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.Error("Credenciales incorrectas")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}