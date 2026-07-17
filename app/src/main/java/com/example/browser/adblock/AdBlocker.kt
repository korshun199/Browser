package com.example.browser.adblock

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.example.browser.R
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Модуль блокировки рекламы.
 * Загружает список доменов из файла hosts.txt и фильтрует запросы WebView.
 */
object AdBlocker {
    private val blockedHosts = mutableSetOf<String>()
    private var isLoaded = false

    /**
     * Загружает список заблокированных доменов из ресурсов.
     * Вызывается один раз при инициализации.
     */
    fun init(context: Context) {
        if (isLoaded) return
        try {
            val inputStream = context.resources.openRawResource(R.raw.hosts)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.forEachLine { line ->
                val trimmed = line.trim()
                // Игнорируем комментарии и пустые строки
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    // Формат: 0.0.0.0 domain.com или 127.0.0.1 domain.com
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        blockedHosts.add(parts[1].lowercase())
                    } else {
                        blockedHosts.add(trimmed.lowercase())
                    }
                }
            }
            reader.close()
            isLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Проверяет, нужно ли заблокировать запрос.
     * Возвращает true, если домен в списке блокировки.
     */
    fun shouldBlock(request: WebResourceRequest): Boolean {
        val host = request.url.host?.lowercase() ?: return false
        // Проверяем точное совпадение и родительские домены
        var domain = host
        while (domain.contains(".")) {
            if (blockedHosts.contains(domain)) return true
            domain = domain.substringAfter(".")
        }
        return blockedHosts.contains(host)
    }

    /**
     * Создаёт пустой ответ для заблокированного ресурса.
     */
    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse("text/plain", "UTF-8", java.io.ByteArrayInputStream("".toByteArray()))
    }
}
