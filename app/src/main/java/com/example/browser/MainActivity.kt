package com.example.browser

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var btnGo: Button
    private lateinit var btnBack: Button
    private lateinit var btnForward: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Привязка элементов интерфейса
        webView = findViewById(R.id.webView)
        urlBar = findViewById(R.id.urlBar)
        btnGo = findViewById(R.id.btnGo)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)

        // Настройка WebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Отображаем текущий URL в адресной строке
                url?.let { urlBar.setText(it) }
            }
        }

        // Загрузка стартовой страницы (позже настраивается в настройках)
        webView.loadUrl("about:blank")

        // Кнопка Перейти — загружает URL из адресной строки
        btnGo.setOnClickListener {
            loadUrlFromBar()
        }

        // Клавиша Enter на клавиатуре тоже вызывает переход
        urlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadUrlFromBar()
                true
            } else {
                false
            }
        }

        // Кнопка Назад по истории WebView
        btnBack.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }

        // Кнопка Вперёд по истории WebView
        btnForward.setOnClickListener {
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }
    }

    /**
     * Загружает URL из адресной строки.
     * Если пользователь не указал протокол, добавляет https://.
     * Если введён поисковый запрос (содержит пробелы или не является URL), ищет через Google.
     */
    private fun loadUrlFromBar() {
        var input = urlBar.text.toString().trim()
        if (input.isEmpty()) return

        // Добавляем протокол, если не указан
        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            // Если есть пробелы или нет точки — считаем поисковым запросом
            if (input.contains(" ") || !input.contains(".")) {
                input = "https://www.google.com/search?q=${input.replace(" ", "+")}"
            } else {
                input = "https://$input"
            }
        }

        webView.loadUrl(input)
    }
}
