package com.example.browser

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.inputmethod.EditorInfo
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
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
    private lateinit var consoleOutput: TextView
    private lateinit var consoleScroll: ScrollView
    private var settingsChanged = false

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
        consoleOutput = findViewById(R.id.consoleOutput)
        consoleScroll = findViewById(R.id.consoleScroll)

        // Настройка WebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        // Клиент для перехвата запросов и блокировки рекламы
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { urlBar.setText(it) }
                // Очищаем консоль при загрузке новой страницы
                consoleOutput.text = "Консоль JS"
            }

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

        // Клиент для перехвата сообщений консоли JS
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let { msg ->
                    val level = msg.messageLevel()
                    val line = msg.lineNumber()
                    val source = msg.sourceId() ?: "неизвестно"
                    val message = msg.message()

                    // Цвет зависит от уровня сообщения
                    val color = when (level) {
                        ConsoleMessage.MessageLevel.ERROR -> Color.RED
                        ConsoleMessage.MessageLevel.WARNING -> Color.parseColor("#FFAA00")
                        else -> Color.GRAY
                    }
                    val prefix = when (level) {
                        ConsoleMessage.MessageLevel.ERROR -> "ОШИБКА"
                        ConsoleMessage.MessageLevel.WARNING -> "ПРЕД"
                        ConsoleMessage.MessageLevel.LOG -> "ЛОГ"
                        ConsoleMessage.MessageLevel.DEBUG -> "ОТЛ"
                        ConsoleMessage.MessageLevel.TIP -> "СОВЕТ"
                        else -> "ИНФО"
                    }

                    val entry = "[$prefix] $source:$line — $message\n"
                    val spannable = SpannableString(entry)
                    // Окрашиваем строку целиком в цвет уровня
                    spannable.setSpan(ForegroundColorSpan(color), 0, entry.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                    consoleOutput.append(spannable)

                    // Автопрокрутка вниз
                    consoleScroll.post {
                        consoleScroll.fullScroll(ScrollView.FOCUS_DOWN)
                    }
                }
                return true
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
            settingsChanged = true
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Сворачивание/разворачивание консоли по нажатию
        consoleOutput.setOnClickListener {
            if (consoleScroll.visibility == android.view.View.VISIBLE) {
                consoleScroll.visibility = android.view.View.GONE
            } else {
                consoleScroll.visibility = android.view.View.VISIBLE
            }
        }

        // Убираем фокус с адресной строки, чтобы клавиатура не вылезала при старте
        webView.requestFocus()
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
        if (settingsChanged) {
            settingsChanged = false
            webView.reload()
        }
    }
}
