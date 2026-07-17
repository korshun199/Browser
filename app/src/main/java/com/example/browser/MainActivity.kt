package com.example.browser

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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

        BrowserSettings.init(this)
        AdBlocker.init(this)

        webView = findViewById(R.id.webView)
        urlBar = findViewById(R.id.urlBar)
        btnGo = findViewById(R.id.btnGo)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnSettings = findViewById(R.id.btnSettings)
        consoleOutput = findViewById(R.id.consoleOutput)
        consoleScroll = findViewById(R.id.consoleScroll)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { urlBar.setText(it) }
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

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let { msg ->
                    val level = msg.messageLevel()
                    val line = msg.lineNumber()
                    val source = msg.sourceId() ?: "неизвестно"
                    val message = msg.message()

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
                    spannable.setSpan(ForegroundColorSpan(color), 0, entry.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    consoleOutput.append(spannable)
                    consoleScroll.post { consoleScroll.fullScroll(ScrollView.FOCUS_DOWN) }
                }
                return true
            }
        }

        webView.loadUrl(BrowserSettings.homepage)

        // Показываем клавиатуру при клике на адресную строку
        urlBar.setOnClickListener {
            urlBar.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(urlBar, InputMethodManager.SHOW_IMPLICIT)
        }

        btnGo.setOnClickListener { loadUrlFromBar() }

        urlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadUrlFromBar()
                true
            } else false
        }

        // Клик по пустому месту или WebView скрывает клавиатуру
        webView.setOnClickListener {
            hideKeyboard()
        }

        btnBack.setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        btnForward.setOnClickListener { if (webView.canGoForward()) webView.goForward() }

        btnSettings.setOnClickListener {
            settingsChanged = true
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        consoleOutput.setOnClickListener {
            consoleScroll.visibility = if (consoleScroll.visibility == View.VISIBLE)
                View.GONE else View.VISIBLE
        }
    }

    private fun loadUrlFromBar() {
        var input = urlBar.text.toString().trim()
        if (input.isEmpty()) return
        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            input = if (input.contains(" ") || !input.contains("."))
                BrowserSettings.getSearchUrl(input) else "https://$input"
        }
        webView.loadUrl(input)
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        if (settingsChanged) {
            settingsChanged = false
            webView.reload()
        }
    }
}
