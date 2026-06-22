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

class MoviesActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var allMovies = listOf<JSONObject>()
    private lateinit var adapter: MoviesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movies)

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }
        findViewById<View>(R.id.btnAdd)?.setOnClickListener { showCreateDialog() }

        val rv = findViewById<RecyclerView>(R.id.rvMovies)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = MoviesAdapter(mutableListOf(),
            onEdit = { showEditDialog(it) },
            onDelete = { confirmDelete(it) }
        )
        rv.adapter = adapter

        findViewById<EditText>(R.id.etSearch)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { filterMovies(s.toString()) }
        })

        loadMovies()
    }

    override fun onResume() { super.onResume(); loadMovies() }

    private fun loadMovies() {
        scope.launch {
            val movies = withContext(Dispatchers.IO) { ApiService.getMovies() }
            allMovies = movies
            filterMovies("")
            findViewById<TextView>(R.id.tvCount)?.text = "${movies.size} películas"
        }
    }

    private fun filterMovies(query: String) {
        val filtered = if (query.length < 2) allMovies
        else allMovies.filter { it.optString("title").contains(query, ignoreCase = true) }
        adapter.updateList(filtered)
    }

    private fun showCreateDialog() {
        val dp = resources.displayMetrics.density
        val layout = buildMovieForm(dp)
        val fields = layout.second

        android.app.AlertDialog.Builder(this)
            .setTitle("➕ Agregar Película")
            .setView(layout.first)
            .setPositiveButton("Agregar") { _, _ ->
                scope.launch {
                    val body = buildMovieBody(fields)
                    val res = withContext(Dispatchers.IO) { ApiService.createMovie(body) }
                    if (res.optBoolean("success")) {
                        Toast.makeText(this@MoviesActivity, "✓ Película agregada", Toast.LENGTH_SHORT).show()
                        loadMovies()
                    } else Toast.makeText(this@MoviesActivity, res.optString("error", "Error"), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun showEditDialog(movie: JSONObject) {
        val dp = resources.displayMetrics.density
        val layout = buildMovieForm(dp, movie)
        val fields = layout.second

        android.app.AlertDialog.Builder(this)
            .setTitle("✏️ Editar Película")
            .setView(layout.first)
            .setPositiveButton("Guardar") { _, _ ->
                scope.launch {
                    val body = buildMovieBody(fields)
                    val res = withContext(Dispatchers.IO) { ApiService.editMovie(movie.optString("_id"), body) }
                    if (res.optBoolean("success")) {
                        Toast.makeText(this@MoviesActivity, "✓ Guardado", Toast.LENGTH_SHORT).show()
                        loadMovies()
                    }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun buildMovieForm(dp: Float, movie: JSONObject? = null): Pair<LinearLayout, Map<String, EditText>> {
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
            "title" to et("Título *", movie?.optString("title") ?: ""),
            "category" to et("Categoría", movie?.optString("category") ?: ""),
            "stream_url" to et("URL del stream *", movie?.optString("stream_url") ?: ""),
            "posterUrl" to et("URL del poster", movie?.optString("posterUrl") ?: ""),
            "backdropUrl" to et("URL del backdrop", movie?.optString("backdropUrl") ?: ""),
            "description" to et("Descripción", movie?.optString("description") ?: ""),
            "rating" to et("Rating (ej: 8.5)", movie?.optString("rating") ?: ""),
            "year" to et("Año", movie?.optString("year") ?: "")
        )
        return Pair(layout, fields)
    }

    private fun buildMovieBody(fields: Map<String, EditText>): JSONObject {
        val body = JSONObject()
        fields.forEach { (key, et) -> if (et.text.isNotEmpty()) body.put(key, et.text.toString().trim()) }
        return body
    }

    private fun confirmDelete(movie: JSONObject) {
        android.app.AlertDialog.Builder(this)
            .setTitle("🗑️ Eliminar")
            .setMessage("¿Eliminar ${movie.optString("title")}?")
            .setPositiveButton("Eliminar") { _, _ ->
                scope.launch {
                    val res = withContext(Dispatchers.IO) { ApiService.deleteMovie(movie.optString("_id")) }
                    if (res.optBoolean("success")) {
                        Toast.makeText(this@MoviesActivity, "Eliminado ✓", Toast.LENGTH_SHORT).show()
                        loadMovies()
                    }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

class MoviesAdapter(
    private val movies: MutableList<JSONObject>,
    private val onEdit: (JSONObject) -> Unit,
    private val onDelete: (JSONObject) -> Unit
) : RecyclerView.Adapter<MoviesAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvYear: TextView = view.findViewById(R.id.tvYear)
        val btnEdit: View = view.findViewById(R.id.btnEdit)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }

    fun updateList(newList: List<JSONObject>) {
        movies.clear(); movies.addAll(newList); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_movie, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = movies[position]
        holder.tvTitle.text = m.optString("title")
        holder.tvCategory.text = m.optString("category")
        holder.tvYear.text = m.optString("year")
        holder.btnEdit.setOnClickListener { onEdit(m) }
        holder.btnDelete.setOnClickListener { onDelete(m) }
    }

    override fun getItemCount() = movies.size
}
