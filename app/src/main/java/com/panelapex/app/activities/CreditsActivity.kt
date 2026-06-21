package com.panelapex.app.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.panelapex.app.R
import com.panelapex.app.services.ApiService
import com.panelapex.app.utils.Prefs
import kotlinx.coroutines.*

class CreditsActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credits)

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }

        loadCredits()

        findViewById<View>(R.id.btnAssign)?.setOnClickListener {
            val email = findViewById<EditText>(R.id.etEmail)?.text.toString().trim()
            val amount = findViewById<EditText>(R.id.etAmount)?.text.toString().trim().toIntOrNull() ?: 0

            if (email.isEmpty() || amount <= 0) {
                Toast.makeText(this, "Email y cantidad requeridos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            scope.launch {
                // Buscar el userId por email
                val users = withContext(Dispatchers.IO) { ApiService.getUsers() }
                val target = users.firstOrNull { it.optString("email") == email }
                if (target == null) {
                    Toast.makeText(this@CreditsActivity, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val res = withContext(Dispatchers.IO) {
                    ApiService.assignCredits(target.optString("_id"), amount)
                }
                if (res.optBoolean("success")) {
                    Toast.makeText(this@CreditsActivity, "✓ Créditos asignados", Toast.LENGTH_SHORT).show()
                    loadCredits()
                } else {
                    Toast.makeText(this@CreditsActivity, res.optString("error", "Error"), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadCredits() {
        scope.launch {
            val data = withContext(Dispatchers.IO) { ApiService.getCredits() }
            Prefs.saveCredits(this@CreditsActivity, data.optInt("credits"))
            findViewById<TextView>(R.id.tvMyCredits)?.text = "💰 ${data.optInt("credits")} créditos disponibles"
            findViewById<TextView>(R.id.tvMyRole)?.text = "Rol: ${data.optString("role").uppercase().replace("_", " ")}"
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
