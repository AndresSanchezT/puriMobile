package com.andresDev.puriapp.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.andresDev.puriapp.R
import com.andresDev.puriapp.databinding.ActivityLoginBinding
import com.andresDev.puriapp.ui.home.MainActivity

class Login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btnLogin.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Opcional: para que no puedan volver al Login presionando atrás
            finish()
        }
    }
}