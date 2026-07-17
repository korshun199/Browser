package com.example.browser

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.browser.adblock.AdBlocker
import com.example.browser.settings.BrowserSettings
import com.example.browser.settings.SettingsActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var btnGo: Button
    private lateinit var btnBack: Button
    private lateinit var btnForward: Button
    private lateinit var btnSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация модулей
        BrowserSettings.init(this)
        AdBlocker.init(this)

        // Привязка элементов интерфейса
        webView = findViewById(R.id.webView)
        urlBar = findViewById(R.id.urlBar)
        btnGo = findViewById(R.id.btnGo)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnSettings = findViewById(R.id.btnSettings)

        // Настройка WebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { urlBar.setText(it) }
            }

            // Перехват запросов для блокировки рекламы
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (BrowserSettings.adBlockEnabled && request != null) {
                    if (AdBlocker.shouldBlock(request)) {
                        return AdBlocker.createEmptyResponse()
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        // Загрузка домашней страницы из настроек
        webView.loadUrl(BrowserSettings.homepage)

        // Кнопка Перейти
        btnGo.setOnClickListener {
            loadUrlFromBar()
        }

        // Enter на клавиатуре
        urlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadUrlFromBar()
                true
            } else {
                false
            }
        }

        // Кнопка Назад
        btnBack.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }

        // Кнопка Вперёд
        btnForward.setOnClickListener {
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }

        // Кнопка Настройки
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun loadUrlFromBar() {
        var input = urlBar.text.toString().trim()
        if (input.isEmpty()) return

        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            if (input.contains(" ") || !input.contains(".")) {
                input = BrowserSettings.getSearchUrl(input)
            } else {
                input = "https://$input"
            }
        }

        webView.loadUrl(input)
    }

    override fun onResume() {
        super.onResume()
        // При возврате из настроек обновляем настройки
    }
}
