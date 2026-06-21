package com.panelapex.app.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.panelapex.app.R
import com.panelapex.app.services.ApiService
import kotlinx.coroutines.*
import org.json.JSONObject

class ChannelsActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channels)

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }
        findViewById<View>(R.id.btnSave)?.setOnClickListener { saveChannel() }
    }

    private fun saveChannel() {
        val name = findViewById<EditText>(R.id.etName)?.text.toString().trim()
        val category = findViewById<EditText>(R.id.etCategory)?.text.toString().trim()
        val streamUrl = findViewById<EditText>(R.id.etStreamUrl)?.text.toString().trim()
        val logo = findViewById<EditText>(R.id.etLogo)?.text.toString().trim()
        val number = findViewById<EditText>(R.id.etNumber)?.text.toString().trim().toIntOrNull() ?: 999
        val drmKeys = findViewById<EditText>(R.id.etDrmKeys)?.text.toString().trim()

        if (name.isEmpty() || streamUrl.isEmpty()) {
            Toast.makeText(this, "Nombre y URL son requeridos", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            try {
                val body = JSONObject()
                body.put("name", name)
                body.put("category", category)
                body.put("stream_url", streamUrl)
                body.put("logo", logo)
                body.put("number", number)
                if (drmKeys.isNotEmpty()) body.put("drm_keys", drmKeys)

                val res = withContext(Dispatchers.IO) {
                    val reqBody = body.toString().toRequestBody("application/json".toMediaType())
                    val req = okhttp3.Request.Builder()
                        .url("http://31.40.212.205:25461/channels")
                        .header("Authorization", "Bearer ${ApiService.token}")
                        .post(reqBody).build()
                    val response = okhttp3.OkHttpClient().newCall(req).execute()
                    JSONObject(response.body?.string() ?: "{}")
                }
                if (res.optBoolean("success")) {
                    Toast.makeText(this@ChannelsActivity, "✓ Canal agregado", Toast.LENGTH_SHORT).show()
                    clearForm()
                } else {
                    Toast.makeText(this@ChannelsActivity, res.optString("error", "Error"), Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@ChannelsActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearForm() {
        listOf(R.id.etName, R.id.etCategory, R.id.etStreamUrl, R.id.etLogo, R.id.etNumber, R.id.etDrmKeys)
            .forEach { findViewById<EditText>(it)?.setText("") }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

fun String.toRequestBody(mediaType: okhttp3.MediaType) =
    okhttp3.RequestBody.create(mediaType, this)

fun String.toMediaType() = okhttp3.MediaType.parse(this)!!
