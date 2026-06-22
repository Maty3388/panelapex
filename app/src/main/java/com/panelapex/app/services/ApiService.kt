package com.panelapex.app.services

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiService {
    private const val BASE = "http://149.104.92.205:25462"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    var token = ""
    var userRole = ""
    var userEmail = ""
    var userCredits = 0
    var loginError = ""

    fun login(email: String, password: String): Boolean {
        return try {
            val body = JSONObject().put("email", email).put("password", password).toString()
                .toRequestBody("application/json".toMediaType())
            val res = client.newCall(Request.Builder().url("$BASE/auth/login").post(body).build()).execute()
            val json = JSONObject(res.body?.string() ?: return false)
            loginError = json.optString("error", "")
            if (loginError.isNotEmpty()) return false
            token = json.optString("token", "")
            val user = json.optJSONObject("user")
            userRole = user?.optString("role", "") ?: ""
            userEmail = user?.optString("email", "") ?: ""
            userCredits = user?.optInt("credits", 0) ?: 0
            val adminRoles = listOf("admin", "distribuidor", "super_reseller", "reseller")
            token.isNotEmpty() && userRole in adminRoles
        } catch (_: Exception) { false }
    }

    fun getUsers(): List<JSONObject> {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/admin/users")
                .header("Authorization", "Bearer $token").build()).execute()
            val json = JSONObject(res.body?.string() ?: return emptyList())
            val arr = json.optJSONArray("users") ?: return emptyList()
            (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (_: Exception) { emptyList() }
    }

    fun getStats(): JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/admin/stats")
                .header("Authorization", "Bearer $token").build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun getCredits(): JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/admin/credits")
                .header("Authorization", "Bearer $token").build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun createUser(email: String, password: String, role: String, subEnd: String, notes: String): JSONObject {
        return try {
            val body = JSONObject().put("email", email).put("password", password).put("role", role)
            if (subEnd.isNotEmpty()) body.put("subscription_end", subEnd)
            if (notes.isNotEmpty()) body.put("notes", notes)
            val res = client.newCall(Request.Builder().url("$BASE/admin/users")
                .header("Authorization", "Bearer $token")
                .post(body.toString().toRequestBody("application/json".toMediaType())).build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun editUser(id: String, body: JSONObject): JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/admin/users/$id")
                .header("Authorization", "Bearer $token")
                .put(body.toString().toRequestBody("application/json".toMediaType())).build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun deleteUser(id: String): JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/admin/users/$id")
                .header("Authorization", "Bearer $token").delete().build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun renewUser(id: String, days: Int): JSONObject {
        return try {
            val body = JSONObject().put("days", days)
            val res = client.newCall(Request.Builder().url("$BASE/admin/users/$id/renew")
                .header("Authorization", "Bearer $token")
                .put(body.toString().toRequestBody("application/json".toMediaType())).build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }


    fun getChannels(): List<JSONObject> {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/channels?limit=5000")
                .header("Authorization", "Bearer $token").build()).execute()
            val json = JSONObject(res.body?.string() ?: return emptyList())
            val arr = json.optJSONArray("channels") ?: return emptyList()
            (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (_: Exception) { emptyList() }
    }

    fun editChannel(id: String, body: JSONObject): JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/channels/$id")
                .header("Authorization", "Bearer $token")
                .put(body.toString().toRequestBody("application/json".toMediaType())).build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun deleteChannel(id: String): JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/channels/$id")
                .header("Authorization", "Bearer $token").delete().build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun createChannel(body: JSONObject): JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/channels")
                .header("Authorization", "Bearer $token")
                .post(body.toString().toRequestBody("application/json".toMediaType())).build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }


    fun getMovies(): List<JSONObject> {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/movies?limit=500")
                .header("Authorization", "Bearer $token").build()).execute()
            val json = JSONObject(res.body?.string() ?: return emptyList())
            val arr = json.optJSONArray("movies") ?: return emptyList()
            (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (_: Exception) { emptyList() }
    }

    fun createMovie(body: JSONObject): JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/movies")
                .header("Authorization", "Bearer $token")
                .post(body.toString().toRequestBody("application/json".toMediaType())).build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun editMovie(id: String, body: JSONObject): JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/movies/$id")
                .header("Authorization", "Bearer $token")
                .put(body.toString().toRequestBody("application/json".toMediaType())).build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun deleteMovie(id: String): JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/movies/$id")
                .header("Authorization", "Bearer $token").delete().build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun getSeries(): List<JSONObject> {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/series")
                .header("Authorization", "Bearer $token").build()).execute()
            val json = JSONObject(res.body?.string() ?: return emptyList())
            val arr = json.optJSONArray("series") ?: return emptyList()
            (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (_: Exception) { emptyList() }
    }

    fun createSerie(body: JSONObject): JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/series")
                .header("Authorization", "Bearer $token")
                .post(body.toString().toRequestBody("application/json".toMediaType())).build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun editSerie(id: String, body: JSONObject): JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/series/$id")
                .header("Authorization", "Bearer $token")
                .put(body.toString().toRequestBody("application/json".toMediaType())).build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun deleteSerie(id: String): JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/series/$id")
                .header("Authorization", "Bearer $token").delete().build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun getExpiring(days: Int): JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/admin/expiring?days=$days")
                .header("Authorization", "Bearer $token").build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun assignCredits(userId: String, credits: Int): JSONObject {
        return try {
            val body = JSONObject().put("userId", userId).put("credits", credits)
            val res = client.newCall(Request.Builder().url("$BASE/admin/credits")
                .header("Authorization", "Bearer $token")
                .put(body.toString().toRequestBody("application/json".toMediaType())).build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }
}
