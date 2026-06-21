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

class ChannelsListActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var allChannels = listOf<JSONObject>()
    private lateinit var adapter: ChannelsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channels_list)

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }
        findViewById<View>(R.id.btnAddChannel)?.setOnClickListener {
            startActivity(Intent(this, ChannelsActivity::class.java))
        }

        val rv = findViewById<RecyclerView>(R.id.rvChannels)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ChannelsAdapter(mutableListOf(),
            onEdit = { showEditDialog(it) },
            onDelete = { confirmDelete(it) }
        )
        rv.adapter = adapter

        findViewById<EditText>(R.id.etSearch)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { filterChannels(s.toString()) }
        })

        // Spinner de categorias
        val spinnerCat = findViewById<Spinner>(R.id.spinnerCategory)
        spinnerCat?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val cat = parent?.getItemAtPosition(position).toString()
                filterChannels(findViewById<EditText>(R.id.etSearch)?.text.toString(), cat)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadChannels()
    }

    override fun onResume() { super.onResume(); loadChannels() }

    private fun loadChannels() {
        scope.launch {
            try {
                val channels = withContext(Dispatchers.IO) { ApiService.getChannels() }
                allChannels = channels
                findViewById<TextView>(R.id.tvCount)?.text = "${channels.size} canales"

                // Cargar categorias en spinner
                val cats = listOf("Todas") + channels.map { it.optString("category") }.distinct().sorted()
                val spinnerCat = findViewById<Spinner>(R.id.spinnerCategory)
                spinnerCat?.adapter = ArrayAdapter(this@ChannelsListActivity,
                    android.R.layout.simple_spinner_dropdown_item, cats)

                filterChannels("")
            } catch (_: Exception) {}
        }
    }

    private fun filterChannels(query: String, category: String = "Todas") {
        var filtered = allChannels
        if (query.length >= 2) filtered = filtered.filter { it.optString("name").contains(query, ignoreCase = true) }
        if (category != "Todas") filtered = filtered.filter { it.optString("category") == category }
        adapter.updateList(filtered)
    }

    private fun showEditDialog(ch: JSONObject) {
        val dp = resources.displayMetrics.density
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16*dp).toInt(), (16*dp).toInt(), (16*dp).toInt(), (8*dp).toInt())
        }
        val etName = EditText(this).apply { hint = "Nombre"; setText(ch.optString("name")); setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF555566.toInt()) }
        val etCat = EditText(this).apply { hint = "Categoría"; setText(ch.optString("category")); setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF555566.toInt()) }
        val etUrl = EditText(this).apply { hint = "URL"; setText(ch.optString("stream_url")); setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF555566.toInt()) }
        val etLogo = EditText(this).apply { hint = "Logo URL"; setText(ch.optString("logo")); setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF555566.toInt()) }
        val etNum = EditText(this).apply { hint = "Número"; setText(ch.optInt("number").toString()); inputType = android.text.InputType.TYPE_CLASS_NUMBER; setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF555566.toInt()) }
        val etDrm = EditText(this).apply { hint = "DRM Keys"; setText(ch.optString("drm_keys")); setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF555566.toInt()) }
        val switchActive = Switch(this).apply { text = "Activo"; isChecked = ch.optBoolean("active", true); setTextColor(0xFFE8E8E8.toInt()) }

        layout.addView(etName); layout.addView(etCat); layout.addView(etUrl)
        layout.addView(etLogo); layout.addView(etNum); layout.addView(etDrm)
        layout.addView(switchActive)

        android.app.AlertDialog.Builder(this)
            .setTitle("✏️ Editar canal")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                scope.launch {
                    try {
                        val body = JSONObject()
                        body.put("name", etName.text.toString().trim())
                        body.put("category", etCat.text.toString().trim())
                        body.put("stream_url", etUrl.text.toString().trim())
                        body.put("logo", etLogo.text.toString().trim())
                        body.put("number", etNum.text.toString().trim().toIntOrNull() ?: 999)
                        body.put("drm_keys", etDrm.text.toString().trim())
                        body.put("active", switchActive.isChecked)
                        val res = withContext(Dispatchers.IO) { ApiService.editChannel(ch.optString("_id"), body) }
                        if (res.optBoolean("success")) {
                            Toast.makeText(this@ChannelsListActivity, "Guardado ✓", Toast.LENGTH_SHORT).show()
                            loadChannels()
                        }
                    } catch (_: Exception) {}
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun confirmDelete(ch: JSONObject) {
        android.app.AlertDialog.Builder(this)
            .setTitle("🗑️ Eliminar canal")
            .setMessage("¿Eliminar ${ch.optString("name")}?")
            .setPositiveButton("Eliminar") { _, _ ->
                scope.launch {
                    val res = withContext(Dispatchers.IO) { ApiService.deleteChannel(ch.optString("_id")) }
                    if (res.optBoolean("success")) {
                        Toast.makeText(this@ChannelsListActivity, "Eliminado ✓", Toast.LENGTH_SHORT).show()
                        loadChannels()
                    }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

class ChannelsAdapter(
    private val channels: MutableList<JSONObject>,
    private val onEdit: (JSONObject) -> Unit,
    private val onDelete: (JSONObject) -> Unit
) : RecyclerView.Adapter<ChannelsAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvNumber: TextView = view.findViewById(R.id.tvNumber)
        val tvActive: TextView = view.findViewById(R.id.tvActive)
        val btnEdit: View = view.findViewById(R.id.btnEdit)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }

    fun updateList(newList: List<JSONObject>) {
        channels.clear(); channels.addAll(newList); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ch = channels[position]
        holder.tvName.text = ch.optString("name")
        holder.tvCategory.text = ch.optString("category")
        holder.tvNumber.text = "#${ch.optInt("number")}"
        val active = ch.optBoolean("active", true)
        holder.tvActive.text = if (active) "🟢" else "🔴"
        holder.btnEdit.setOnClickListener { onEdit(ch) }
        holder.btnDelete.setOnClickListener { onDelete(ch) }
    }

    override fun getItemCount() = channels.size
}
