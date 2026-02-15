package com.andresDev.puriapp.ui.home

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.andresDev.puriapp.R
import com.andresDev.puriapp.data.manager.TokenManager
import com.andresDev.puriapp.data.repository.PedidoRepository
import com.andresDev.puriapp.databinding.ActivityMainBinding
import com.andresDev.puriapp.ui.login.Login
import com.andresDev.puriapp.ui.pedidos.PedidoViewModel
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var pedidoRepository: PedidoRepository

    private val pedidoViewModel: PedidoViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 VERIFICAR SI HAY SESIÓN VÁLIDA
        if (!tokenManager.isLoggedIn()) {
            redirectToLogin()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }

    private fun initUI() {
        initNavigation()
        setupBottomNavigationMenu()
        setupUserIcon()
    }

    private fun setupBottomNavigationMenu() {
        val userRole = tokenManager.getUserRole()

        val menuRes = if (userRole?.equals("administrador", ignoreCase = true) == true) {
            R.menu.bottom_nav_menu_admin
        } else {
            R.menu.bottom_menu
        }

        binding.bottonNavView.menu.clear()
        binding.bottonNavView.inflateMenu(menuRes)
        binding.bottonNavView.setupWithNavController(navController)

        Log.d("MainActivity", "Menú cargado para rol: $userRole")
    }

    private fun initNavigation() {
        val navHost = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHost.navController
    }

    private fun setupUserIcon() {
        binding.ivUserIcon.setOnClickListener {
            showUserMenuDialog()
        }
    }

    private fun showUserMenuDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_user_menu)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val userName = tokenManager.getUserName() ?: "Usuario"
        val userRole = tokenManager.getUserRole() ?: "Sin rol"

        val tvUserName = dialog.findViewById<TextView>(R.id.tvUserName)
        val tvUserRole = dialog.findViewById<TextView>(R.id.tvUserRole)
        val tvCantidadEfectivo = dialog.findViewById<TextView>(R.id.tvCantidadEfectivo)
        val btnCerrarSesion = dialog.findViewById<MaterialButton>(R.id.btnCerrarSesion)

        tvUserName.text = userName
        tvUserRole.text = userRole

        lifecycleScope.launch {
            pedidoViewModel.efectivoDelDia.collect { efectivo ->
                tvCantidadEfectivo.text = "S/. %.2f".format(efectivo)
            }
        }

        pedidoViewModel.actualizarEfectivoDelDia()

        btnCerrarSesion.setOnClickListener {
            dialog.dismiss()
            cerrarSesion()
        }

        dialog.show()
    }

    private fun cerrarSesion() {
        // Limpiar caché de pedidos antes de cerrar sesión
        pedidoRepository.limpiarCache()
        Log.d("MainActivity", "🗑️ Caché de pedidos limpiado")

        // Limpiar sesión
        tokenManager.clearSession()

        // Redirigir a login
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}