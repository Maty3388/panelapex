package com.panelapex.app.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.panelapex.app.R
import com.panelapex.app.services.ApiService
import kotlinx.coroutines.*
import org.json.JSONObject

class UsersActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var allUsers = listOf<JSONObject>()
    private lateinit var adapter: UsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }
        findViewById<View>(R.id.btnCreate)?.setOnClickListener {
            startActivity(Intent(this, CreateUserActivity::class.java))
        }

        val rv = findViewById<RecyclerView>(R.id.rvUsers)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = UsersAdapter(mutableListOf(),
            onEdit = { showEditDialog(it) },
            onDelete = { confirmDelete(it) },
            onRenew = { showRenewDialog(it) }
        )
        rv.adapter = adapter

        findViewById<EditText>(R.id.etSearch)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { filterUsers(s.toString()) }
        })

        loadUsers()
    }

    override fun onResume() { super.onResume(); loadUsers() }

    private fun loadUsers() {
        scope.launch {
            try {
                val users = withContext(Dispatchers.IO) { ApiService.getUsers() }
                allUsers = users
                filterUsers(findViewById<EditText>(R.id.etSearch)?.text.toString())
                findViewById<TextView>(R.id.tvCount)?.text = "${users.size} usuarios"
            } catch (_: Exception) {}
        }
    }

    private fun filterUsers(query: String) {
        val filtered = if (query.length < 2) allUsers
        else allUsers.filter { it.optString("email").contains(query, ignoreCase = true) }
        adapter.updateList(filtered)
    }

    private fun showEditDialog(user: JSONObject) {
        val dp = resources.displayMetrics.density
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16*dp).toInt(), (16*dp).toInt(), (16*dp).toInt(), (8*dp).toInt())
        }
        val etSubEnd = EditText(this).apply {
            hint = "Vencimiento (YYYY-MM-DD)"
            setText(user.optString("subscription_end").take(10))
            setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF555566.toInt())
        }
        val etNotes = EditText(this).apply {
            hint = "Notas"
            setText(user.optString("notes"))
            setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF555566.toInt())
        }
        val etPass = EditText(this).apply {
            hint = "Nueva contraseña (opcional)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF555566.toInt())
        }
        val switchBlocked = Switch(this).apply {
            text = "Bloqueado"
            isChecked = user.optBoolean("blocked")
            setTextColor(0xFFE8E8E8.toInt())
        }
        layout.addView(TextView(this).apply { text = user.optString("email"); setTextColor(0xFFC9A84C.toInt()); textSize = 14f })
        layout.addView(etSubEnd); layout.addView(etNotes); layout.addView(etPass); layout.addView(switchBlocked)

        android.app.AlertDialog.Builder(this)
            .setTitle("✏️ Editar usuario")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                scope.launch {
                    try {
                        val body = JSONObject()
                        body.put("subscription_end", etSubEnd.text.toString().trim())
                        body.put("notes", etNotes.text.toString().trim())
                        body.put("blocked", switchBlocked.isChecked)
                        if (etPass.text.isNotEmpty()) body.put("password", etPass.text.toString())
                        val res = withContext(Dispatchers.IO) { ApiService.editUser(user.optString("_id"), body) }
                        if (res.optBoolean("success")) {
                            Toast.makeText(this@UsersActivity, "Guardado ✓", Toast.LENGTH_SHORT).show()
                            loadUsers()
                        }
                    } catch (_: Exception) {}
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun showRenewDialog(user: JSONObject) {
        val options = arrayOf("30 días (-1 crédito)", "60 días (-1 crédito)", "90 días (-1 crédito)", "180 días (-1 crédito)", "365 días (-1 crédito)")
        val days = arrayOf(30, 60, 90, 180, 365)
        android.app.AlertDialog.Builder(this)
            .setTitle("🔄 Renovar: ${user.optString("email")}")
            .setItems(options) { _, which ->
                scope.launch {
                    val res = withContext(Dispatchers.IO) { ApiService.renewUser(user.optString("_id"), days[which]) }
                    if (res.optBoolean("success")) {
                        Toast.makeText(this@UsersActivity, "Renovado ✓", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    } else {
                        Toast.makeText(this@UsersActivity, res.optString("error", "Error"), Toast.LENGTH_SHORT).show()
                    }
                }
            }.show()
    }

    private fun confirmDelete(user: JSONObject) {
        android.app.AlertDialog.Builder(this)
            .setTitle("🗑️ Eliminar")
            .setMessage("¿Eliminar ${user.optString("email")}?")
            .setPositiveButton("Eliminar") { _, _ ->
                scope.launch {
                    val res = withContext(Dispatchers.IO) { ApiService.deleteUser(user.optString("_id")) }
                    if (res.optBoolean("success")) {
                        Toast.makeText(this@UsersActivity, "Eliminado ✓", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

class UsersAdapter(
    private val users: MutableList<JSONObject>,
    private val onEdit: (JSONObject) -> Unit,
    private val onDelete: (JSONObject) -> Unit,
    private val onRenew: (JSONObject) -> Unit
) : RecyclerView.Adapter<UsersAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmail: TextView = view.findViewById(R.id.tvEmail)
        val tvRole: TextView = view.findViewById(R.id.tvRole)
        val tvSubEnd: TextView = view.findViewById(R.id.tvSubEnd)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val btnEdit: View = view.findViewById(R.id.btnEdit)
        val btnRenew: View = view.findViewById(R.id.btnRenew)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }

    fun updateList(newList: List<JSONObject>) {
        users.clear(); users.addAll(newList); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user_panel, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = users[position]
        holder.tvEmail.text = user.optString("email")
        holder.tvRole.text = user.optString("role").uppercase().replace("_", " ")
        holder.tvSubEnd.text = user.optString("subscription_end").take(10).ifEmpty { "Sin fecha" }
        val blocked = user.optBoolean("blocked")
        holder.tvStatus.text = if (blocked) "🔴" else "🟢"
        holder.btnEdit.setOnClickListener { onEdit(user) }
        holder.btnRenew.setOnClickListener { onRenew(user) }
        holder.btnDelete.setOnClickListener { onDelete(user) }
    }

    override fun getItemCount() = users.size
}
