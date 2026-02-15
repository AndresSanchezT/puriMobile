package com.andresDev.puriapp.ui.login

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.andresDev.puriapp.data.manager.TokenManager
import com.andresDev.puriapp.data.repository.PedidoRepository
import com.andresDev.puriapp.databinding.ActivityLoginBinding
import com.andresDev.puriapp.ui.home.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var tokenManager: TokenManager

    //  Inyectar PedidoRepository para limpiar caché
//    @Inject
//    lateinit var pedidoRepository: PedidoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔥 Limpiar token expirado y caché
        limpiarTokenExpirado()
        observeLoginState()
        setupListeners()
    }

    private fun limpiarTokenExpirado() {
        val token = tokenManager.getToken()
        if (token != null) {
            Log.d(TAG, "Token encontrado, verificando validez...")

//            // 🔥  Limpiar caché de pedidos
//            pedidoRepository.limpiarCache()
//            Log.d(TAG, "🗑️ Caché de pedidos limpiado")

            // Limpiar sesión para forzar nuevo login
            tokenManager.clearSession()
            Log.d(TAG, "Sesión limpiada por seguridad")

            Toast.makeText(
                this,
                "Sesión expirada. Por favor inicia sesión nuevamente.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val usuario = binding.usuarioEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            when {
                usuario.isEmpty() -> {
                    binding.ilUsuario.error = "Ingrese su usuario"
                }
                password.isEmpty() -> {
                    binding.passwordInputLayout.error = "Ingrese su contraseña"
                }
                else -> {
                    binding.ilUsuario.error = null
                    binding.passwordInputLayout.error = null
                    viewModel.login(usuario, password)
                }
            }
        }
    }

    private fun observeLoginState() {
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is LoginState.Idle -> {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = "Iniciar Sesión"
                    }
                    is LoginState.Loading -> {
                        binding.btnLogin.isEnabled = false
                        binding.btnLogin.text = "Cargando..."
                    }
                    is LoginState.Success -> {
                        Toast.makeText(this@Login, "Login exitoso", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@Login, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    is LoginState.Error -> {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = "Iniciar Sesión"
                        Toast.makeText(this@Login, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}