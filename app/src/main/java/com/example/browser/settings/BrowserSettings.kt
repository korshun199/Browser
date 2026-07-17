package com.example.browser.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Модуль настроек браузера.
 * Все параметры сохраняются в SharedPreferences.
 */
object BrowserSettings {
    private const val PREFS_NAME = "browser_prefs"
    private const val KEY_HOMEPAGE = "homepage"
    private const val KEY_SEARCH_ENGINE = "search_engine"
    private const val KEY_AD_BLOCK = "ad_block"
    private const val KEY_VPS_HOST = "vps_host"
    private const val KEY_VPS_PORT = "vps_port"
    private const val KEY_VPS_USER = "vps_user"
    private const val KEY_VPS_KEY_PATH = "vps_key_path"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var homepage: String
        get() = prefs.getString(KEY_HOMEPAGE, "about:blank") ?: "about:blank"
        set(value) = prefs.edit().putString(KEY_HOMEPAGE, value).apply()

    var searchEngine: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, "google") ?: "google"
        set(value) = prefs.edit().putString(KEY_SEARCH_ENGINE, value).apply()

    var adBlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_AD_BLOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_AD_BLOCK, value).apply()

    // Настройки VPS
    var vpsHost: String
        get() = prefs.getString(KEY_VPS_HOST, "46.8.221.179") ?: "46.8.221.179"
        set(value) = prefs.edit().putString(KEY_VPS_HOST, value).apply()

    var vpsPort: Int
        get() = prefs.getInt(KEY_VPS_PORT, 4101)
        set(value) = prefs.edit().putInt(KEY_VPS_PORT, value).apply()

    var vpsUser: String
        get() = prefs.getString(KEY_VPS_USER, "oleg") ?: "oleg"
        set(value) = prefs.edit().putString(KEY_VPS_USER, value).apply()

    var vpsKeyPath: String
        get() = prefs.getString(KEY_VPS_KEY_PATH, "/data/data/com.example.browser/files/ssh/id_rsa") ?: "/data/data/com.example.browser/files/ssh/id_rsa"
        set(value) = prefs.edit().putString(KEY_VPS_KEY_PATH, value).apply()

    fun getSearchUrl(query: String): String {
        return when (searchEngine) {
            "yandex" -> "https://yandex.ru/search/?text=${query.replace(" ", "+")}"
            "duckduckgo" -> "https://duckduckgo.com/?q=${query.replace(" ", "+")}"
            else -> "https://www.google.com/search?q=${query.replace(" ", "+")}"
        }
    }
}
