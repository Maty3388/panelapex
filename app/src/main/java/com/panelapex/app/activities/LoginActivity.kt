package com.panelapex.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.panelapex.app.R
import com.panelapex.app.services.ApiService
import com.panelapex.app.utils.Prefs
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvError = findViewById<TextView>(R.id.tvError)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                tvError.text = "Completá todos los campos"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false
            tvError.visibility = View.GONE
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try { ApiService.login(email, password) } catch (_: Exception) { false }
                }
                progressBar.visibility = View.GONE
                btnLogin.isEnabled = true
                if (ok) {
                    Prefs.saveToken(this@LoginActivity, ApiService.token)
                    Prefs.saveEmail(this@LoginActivity, ApiService.userEmail)
                    Prefs.saveRole(this@LoginActivity, ApiService.userRole)
                    Prefs.saveCredits(this@LoginActivity, ApiService.userCredits)
                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                    finish()
                } else {
                    tvError.text = when {
                        ApiService.loginError.contains("vencida", ignoreCase = true) -> "Suscripción vencida"
                        ApiService.loginError.contains("bloqueado", ignoreCase = true) -> "Cuenta bloqueada"
                        ApiService.loginError.isNotEmpty() -> ApiService.loginError
                        else -> "Sin permisos de acceso"
                    }
                    tvError.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
