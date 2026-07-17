package com.example.browser.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Модуль настроек браузера.
 * Хранит параметры в SharedPreferences.
 * При добавлении новых настроек — дополняем этот класс.
 */
object BrowserSettings {
    private const val PREFS_NAME = "browser_prefs"
    private const val KEY_HOMEPAGE = "homepage"
    private const val KEY_SEARCH_ENGINE = "search_engine"
    private const val KEY_AD_BLOCK = "ad_block"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Домашняя страница (по умолчанию about:blank)
    var homepage: String
        get() = prefs.getString(KEY_HOMEPAGE, "about:blank") ?: "about:blank"
        set(value) = prefs.edit().putString(KEY_HOMEPAGE, value).apply()

    // Поисковик: google, yandex, duckduckgo
    var searchEngine: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, "google") ?: "google"
        set(value) = prefs.edit().putString(KEY_SEARCH_ENGINE, value).apply()

    // Блокировка рекламы: вкл/выкл
    var adBlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_AD_BLOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_AD_BLOCK, value).apply()

    /**
     * Возвращает URL для поискового запроса.
     */
    fun getSearchUrl(query: String): String {
        return when (searchEngine) {
            "yandex" -> "https://yandex.ru/search/?text=${query.replace(" ", "+")}"
            "duckduckgo" -> "https://duckduckgo.com/?q=${query.replace(" ", "+")}"
            else -> "https://www.google.com/search?q=${query.replace(" ", "+")}"
        }
    }
}
