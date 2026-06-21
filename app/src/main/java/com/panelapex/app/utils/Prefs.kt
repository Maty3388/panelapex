package com.panelapex.app.utils

import android.content.Context

object Prefs {
    private const val NAME = "panelapex_prefs"

    fun saveToken(ctx: Context, t: String) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString("token", t).apply()
    fun getToken(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("token", "") ?: ""
    fun saveEmail(ctx: Context, e: String) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString("email", e).apply()
    fun getEmail(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("email", "") ?: ""
    fun saveRole(ctx: Context, r: String) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString("role", r).apply()
    fun getRole(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("role", "") ?: ""
    fun saveCredits(ctx: Context, c: Int) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putInt("credits", c).apply()
    fun getCredits(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getInt("credits", 0)
    fun isLoggedIn(ctx: Context) = getToken(ctx).isNotEmpty()
    fun logout(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().clear().apply()
}
