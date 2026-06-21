package com.panelapex.app.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.panelapex.app.R
import com.panelapex.app.services.ApiService
import kotlinx.coroutines.*
import org.json.JSONObject

class HistorialActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)
        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }
        loadHistorial()
    }

    private fun loadHistorial() {
        scope.launch {
            try {
                val data = withContext(Dispatchers.IO) { ApiService.getCredits() }
                val arr = data.optJSONArray("activations") ?: return@launch
                val list = (0 until arr.length()).map { arr.getJSONObject(it) }.reversed()
                val rv = findViewById<RecyclerView>(R.id.rvHistorial)
                rv.layoutManager = LinearLayoutManager(this@HistorialActivity)
                rv.adapter = HistorialAdapter(list)
                findViewById<TextView>(R.id.tvCount)?.text = "${list.size} activaciones"
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

class HistorialAdapter(private val items: List<JSONObject>) : RecyclerView.Adapter<HistorialAdapter.VH>() {
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvType: TextView = view.findViewById(R.id.tvType)
        val tvUser: TextView = view.findViewById(R.id.tvUser)
        val tvCredits: TextView = view.findViewById(R.id.tvCredits)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_historial, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val type = item.optString("type")
        holder.tvType.text = when(type) {
            "demo" -> "🎯 DEMO"
            "client" -> "👤 CLIENTE"
            "reseller" -> "🤝 RESELLER"
            "super_reseller" -> "⭐ SUPER RESELLER"
            else -> type.uppercase()
        }
        holder.tvUser.text = item.optString("user_created", "-")
        holder.tvCredits.text = "-${item.optInt("credits_used")} créditos"
        holder.tvCredits.setTextColor(if (item.optInt("credits_used") > 0) 0xFFFF6B6B.toInt() else 0xFF27AE60.toInt())
        val date = item.optString("created_at").take(10)
        holder.tvDate.text = date
    }

    override fun getItemCount() = items.size
}
