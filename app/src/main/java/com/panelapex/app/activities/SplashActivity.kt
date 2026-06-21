package com.panelapex.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.panelapex.app.services.ApiService
import com.panelapex.app.utils.Prefs
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scope.launch {
            delay(1000)
            if (Prefs.isLoggedIn(this@SplashActivity)) {
                ApiService.token = Prefs.getToken(this@SplashActivity)
                ApiService.userRole = Prefs.getRole(this@SplashActivity)
                ApiService.userEmail = Prefs.getEmail(this@SplashActivity)
                startActivity(Intent(this@SplashActivity, DashboardActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
