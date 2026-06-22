package com.panelapex.app.activities

import android.os.Bundle
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

class ExpiringActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var days = 7

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expiring)

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }

        val options = arrayOf("3 días", "7 días", "15 días", "30 días")
        val daysArray = arrayOf(3, 7, 15, 30)
        val spinner = findViewById<Spinner>(R.id.spinnerDays)
        spinner?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                days = daysArray[position]
                loadExpiring()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadExpiring()
    }

    private fun loadExpiring() {
        scope.launch {
            try {
                val data = withContext(Dispatchers.IO) { ApiService.getExpiring(days) }
                val arr = data.optJSONArray("users") ?: return@launch
                val list = (0 until arr.length()).map { arr.getJSONObject(it) }
                val rv = findViewById<RecyclerView>(R.id.rvExpiring)
                rv.layoutManager = LinearLayoutManager(this@ExpiringActivity)
                rv.adapter = ExpiringAdapter(list)
                findViewById<TextView>(R.id.tvCount)?.text = "${list.size} usuarios por vencer"
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

class ExpiringAdapter(private val users: List<JSONObject>) : RecyclerView.Adapter<ExpiringAdapter.VH>() {
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmail: TextView = view.findViewById(R.id.tvEmail)
        val tvSubEnd: TextView = view.findViewById(R.id.tvSubEnd)
        val tvRole: TextView = view.findViewById(R.id.tvRole)
        val tvDaysLeft: TextView = view.findViewById(R.id.tvDaysLeft)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_expiring, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = users[position]
        holder.tvEmail.text = user.optString("email")
        holder.tvSubEnd.text = user.optString("subscription_end").take(10)
        holder.tvRole.text = user.optString("role").uppercase()

        val subEnd = user.optString("subscription_end").take(10)
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val diff = sdf.parse(subEnd)!!.time - System.currentTimeMillis()
            val daysLeft = (diff / (1000 * 60 * 60 * 24)).toInt()
            holder.tvDaysLeft.text = "$daysLeft días"
            holder.tvDaysLeft.setTextColor(when {
                daysLeft <= 3 -> 0xFFFF4444.toInt()
                daysLeft <= 7 -> 0xFFFF8C00.toInt()
                else -> 0xFFC9A84C.toInt()
            })
        } catch (_: Exception) { holder.tvDaysLeft.text = "-" }
    }

    override fun getItemCount() = users.size
}
