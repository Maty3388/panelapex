package com.panelapex.app.activities

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

class SeriesActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var allSeries = listOf<JSONObject>()
    private lateinit var adapter: SeriesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series)

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }
        findViewById<View>(R.id.btnAdd)?.setOnClickListener { showCreateDialog() }

        val rv = findViewById<RecyclerView>(R.id.rvSeries)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = SeriesAdapter(mutableListOf(),
            onEdit = { showEditDialog(it) },
            onDelete = { confirmDelete(it) }
        )
        rv.adapter = adapter

        findViewById<EditText>(R.id.etSearch)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { filterSeries(s.toString()) }
        })

        loadSeries()
    }

    override fun onResume() { super.onResume(); loadSeries() }

    private fun loadSeries() {
        scope.launch {
            val series = withContext(Dispatchers.IO) { ApiService.getSeries() }
            allSeries = series
            filterSeries("")
            findViewById<TextView>(R.id.tvCount)?.text = "${series.size} series"
        }
    }

    private fun filterSeries(query: String) {
        val filtered = if (query.length < 2) allSeries
        else allSeries.filter { it.optString("title").contains(query, ignoreCase = true) }
        adapter.updateList(filtered)
    }

    private fun showCreateDialog() {
        val dp = resources.displayMetrics.density
        val layout = buildSerieForm(dp)
        android.app.AlertDialog.Builder(this)
            .setTitle("➕ Agregar Serie")
            .setView(layout.first)
            .setPositiveButton("Agregar") { _, _ ->
                scope.launch {
                    val body = buildSerieBody(layout.second)
                    val res = withContext(Dispatchers.IO) { ApiService.createSerie(body) }
                    if (res.optBoolean("success")) {
                        Toast.makeText(this@SeriesActivity, "✓ Serie agregada", Toast.LENGTH_SHORT).show()
                        loadSeries()
                    } else Toast.makeText(this@SeriesActivity, res.optString("error", "Error"), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun showEditDialog(serie: JSONObject) {
        val dp = resources.displayMetrics.density
        val layout = buildSerieForm(dp, serie)
        android.app.AlertDialog.Builder(this)
            .setTitle("✏️ Editar Serie")
            .setView(layout.first)
            .setPositiveButton("Guardar") { _, _ ->
                scope.launch {
                    val body = buildSerieBody(layout.second)
                    val res = withContext(Dispatchers.IO) { ApiService.editSerie(serie.optString("_id"), body) }
                    if (res.optBoolean("success")) {
                        Toast.makeText(this@SeriesActivity, "✓ Guardado", Toast.LENGTH_SHORT).show()
                        loadSeries()
                    }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun buildSerieForm(dp: Float, serie: JSONObject? = null): Pair<LinearLayout, Map<String, EditText>> {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16*dp).toInt(), (16*dp).toInt(), (16*dp).toInt(), (8*dp).toInt())
        }
        fun et(hint: String, value: String = "") = EditText(this).apply {
            this.hint = hint; setText(value)
            setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF555566.toInt())
            layout.addView(this)
        }
        val fields = mapOf(
            "title" to et("Título *", serie?.optString("title") ?: ""),
            "category" to et("Categoría", serie?.optString("category") ?: ""),
            "stream_url" to et("URL del stream", serie?.optString("stream_url") ?: ""),
            "posterUrl" to et("URL del poster", serie?.optString("posterUrl") ?: ""),
            "description" to et("Descripción", serie?.optString("description") ?: ""),
            "rating" to et("Rating", serie?.optString("rating") ?: ""),
            "year" to et("Año", serie?.optString("year") ?: "")
        )
        return Pair(layout, fields)
    }

    private fun buildSerieBody(fields: Map<String, EditText>): JSONObject {
        val body = JSONObject()
        fields.forEach { (key, et) -> if (et.text.isNotEmpty()) body.put(key, et.text.toString().trim()) }
        return body
    }

    private fun confirmDelete(serie: JSONObject) {
        android.app.AlertDialog.Builder(this)
            .setTitle("🗑️ Eliminar")
            .setMessage("¿Eliminar ${serie.optString("title")}?")
            .setPositiveButton("Eliminar") { _, _ ->
                scope.launch {
                    val res = withContext(Dispatchers.IO) { ApiService.deleteSerie(serie.optString("_id")) }
                    if (res.optBoolean("success")) {
                        Toast.makeText(this@SeriesActivity, "Eliminado ✓", Toast.LENGTH_SHORT).show()
                        loadSeries()
                    }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

class SeriesAdapter(
    private val series: MutableList<JSONObject>,
    private val onEdit: (JSONObject) -> Unit,
    private val onDelete: (JSONObject) -> Unit
) : RecyclerView.Adapter<SeriesAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvYear: TextView = view.findViewById(R.id.tvYear)
        val btnEdit: View = view.findViewById(R.id.btnEdit)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }

    fun updateList(newList: List<JSONObject>) {
        series.clear(); series.addAll(newList); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_movie, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = series[position]
        holder.tvTitle.text = s.optString("title")
        holder.tvCategory.text = s.optString("category")
        holder.tvYear.text = s.optString("year")
        holder.btnEdit.setOnClickListener { onEdit(s) }
        holder.btnDelete.setOnClickListener { onDelete(s) }
    }

    override fun getItemCount() = series.size
}
