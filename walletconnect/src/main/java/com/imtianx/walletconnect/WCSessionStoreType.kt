package com.imtianx.walletconnect

import android.content.SharedPreferences
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.GsonBuilder

class WCSessionStoreType(
    private val sharedPreferences: SharedPreferences,
    builder: GsonBuilder = GsonBuilder()
) : WCSessionStore {
    private val gson = builder
        .serializeNulls()
        .create()

    private fun store(item: WCSessionStoreItem?) {
        if (item != null) {
            sharedPreferences.edit().putString(SESSION_KEY, gson.toJson(item)).apply()
        } else {
            sharedPreferences.edit().remove(SESSION_KEY).apply()
        }
    }

    private fun load(): WCSessionStoreItem? {
        val json = sharedPreferences.getString(SESSION_KEY, null) ?: return null
        return gson.fromJson(json)
    }

    override var session: WCSessionStoreItem?
        set(item) = store(item)
        get() = load()


    fun store(item: WCSessionStoreItem?, sessionKey: String) {
        if (item != null) {
            sharedPreferences.edit().putString(sessionKey, gson.toJson(item)).apply()
        } else {
            sharedPreferences.edit().remove(sessionKey).apply()
        }
    }

    fun remove(sessionKey: String) {
        sharedPreferences.edit().remove(sessionKey).apply()
    }

    fun load(sessionKey: String): WCSessionStoreItem? {
        val json = sharedPreferences.getString(sessionKey, null) ?: return null
        return gson.fromJson(json)
    }

    companion object {
        private const val SESSION_KEY = "org.walletconnect.session"
    }
}