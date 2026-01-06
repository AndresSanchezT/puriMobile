package com.andresDev.puriapp.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.andresDev.puriapp.R
import com.andresDev.puriapp.data.manager.TokenManager
import com.andresDev.puriapp.databinding.ActivityMainBinding
import com.andresDev.puriapp.ui.login.Login
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var tokenManager: TokenManager
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController : NavController


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

    private fun initUI(){
        initNavigation()
    }
    private fun initNavigation(){
        val navHost = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment

        navController = navHost.navController
        binding.bottonNavView.setupWithNavController(navController)
    }
}