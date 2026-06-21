package com.panelapex.app.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.panelapex.app.R
import com.panelapex.app.services.ApiService
import com.panelapex.app.utils.Prefs
import kotlinx.coroutines.*

class CreateUserActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_user)

        val role = Prefs.getRole(this)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etSubEnd = findViewById<EditText>(R.id.etSubEnd)
        val etNotes = findViewById<EditText>(R.id.etNotes)
        val spinnerRole = findViewById<Spinner>(R.id.spinnerRole)
        val tvError = findViewById<TextView>(R.id.tvError)
        val tvCredits = findViewById<TextView>(R.id.tvCredits)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Roles disponibles según el rol del usuario
        val availableRoles = when (role) {
            "admin" -> arrayOf("client", "demo", "reseller", "super_reseller", "distribuidor")
            "distribuidor" -> arrayOf("client", "demo", "reseller", "super_reseller")
            "super_reseller" -> arrayOf("client", "demo", "reseller")
            else -> arrayOf("client", "demo")
        }

        spinnerRole.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, availableRoles)

        tvCredits.text = "Créditos disponibles: ${Prefs.getCredits(this)}"

        // Cuando cambia el rol mostrar costo
        spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedRole = availableRoles[position]
                val cost = when (selectedRole) {
                    "demo" -> 0
                    "client" -> 1
                    "reseller", "super_reseller" -> 10
                    else -> 0
                }
                tvCredits.text = "Créditos disponibles: ${Prefs.getCredits(this@CreateUserActivity)} | Costo: $cost"
                // Si es demo, auto fecha
                if (selectedRole == "demo") etSubEnd.setText("Demo 1 hora")
                else if (etSubEnd.text.toString() == "Demo 1 hora") etSubEnd.setText("")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }
        findViewById<View>(R.id.btnCreate)?.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val selectedRole = availableRoles[spinnerRole.selectedItemPosition]
            val subEnd = if (selectedRole == "demo") "" else etSubEnd.text.toString().trim()
            val notes = etNotes.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                tvError.text = "Email y contraseña son requeridos"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            tvError.visibility = View.GONE

            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    try { ApiService.createUser(email, password, selectedRole, subEnd, notes) }
                    catch (_: Exception) { org.json.JSONObject() }
                }
                progressBar.visibility = View.GONE
                if (res.optBoolean("success")) {
                    Toast.makeText(this@CreateUserActivity, "✓ Usuario creado", Toast.LENGTH_SHORT).show()
                    // Actualizar créditos
                    val credits = withContext(Dispatchers.IO) { ApiService.getCredits() }
                    Prefs.saveCredits(this@CreateUserActivity, credits.optInt("credits"))
                    finish()
                } else {
                    tvError.text = res.optString("error", "Error al crear")
                    tvError.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
