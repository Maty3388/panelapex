package com.panelapex.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.panelapex.app.R
import com.panelapex.app.services.ApiService
import com.panelapex.app.utils.Prefs
import kotlinx.coroutines.*

class DashboardActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val role = Prefs.getRole(this)
        val email = Prefs.getEmail(this)

        findViewById<TextView>(R.id.tvWelcome)?.text = "Hola, $email"
        findViewById<TextView>(R.id.tvRole)?.text = role.uppercase().replace("_", " ")

        loadStats()

        findViewById<android.view.View>(R.id.btnUsers)?.setOnClickListener {
            startActivity(Intent(this, UsersActivity::class.java))
        }
        findViewById<android.view.View>(R.id.btnCredits)?.setOnClickListener {
            startActivity(Intent(this, CreditsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.btnMovies)?.setOnClickListener {
            startActivity(Intent(this, MoviesActivity::class.java))
        }
        findViewById<android.view.View>(R.id.btnSeries)?.setOnClickListener {
            startActivity(Intent(this, SeriesActivity::class.java))
        }
        findViewById<android.view.View>(R.id.btnHistorial)?.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }
        findViewById<android.view.View>(R.id.btnChannels)?.setOnClickListener {
            startActivity(Intent(this, ChannelsListActivity::class.java))
        }
        findViewById<android.view.View>(R.id.btnLogout)?.setOnClickListener {
            Prefs.logout(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    private fun loadStats() {
        scope.launch {
            try {
                val stats = withContext(Dispatchers.IO) { ApiService.getStats() }
                val credits = withContext(Dispatchers.IO) { ApiService.getCredits() }
                findViewById<TextView>(R.id.tvStatTotal)?.text = "${stats.optInt("total")}"
                findViewById<TextView>(R.id.tvStatActive)?.text = "${stats.optInt("active")}"
                findViewById<TextView>(R.id.tvStatDemos)?.text = "${stats.optInt("demos")}"

                findViewById<TextView>(R.id.tvCredits)?.text = "${credits.optInt("credits")}"
            } catch (_: Exception) {}
        }
    }

    override fun onResume() { super.onResume(); loadStats() }
    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
