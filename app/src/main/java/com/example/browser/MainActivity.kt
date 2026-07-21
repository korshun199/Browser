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
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.browser.adblock.AdBlocker
import com.example.browser.settings.BrowserSettings
import com.example.browser.settings.SettingsActivity
import com.example.browser.terminal.TerminalActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var btnGo: Button
    private lateinit var btnBack: Button
    private lateinit var btnForward: Button
    private lateinit var btnStyle: Button
    private lateinit var btnSettings: Button
    private lateinit var btnTerminal: Button
    private lateinit var consoleOutput: TextView
    private lateinit var consoleScroll: ScrollView

    private var settingsChanged = false
    private var selectedStyle = PageStyle.ORIGINAL

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
        btnStyle = findViewById(R.id.btnStyle)
        btnSettings = findViewById(R.id.btnSettings)
        btnTerminal = findViewById(R.id.btnTerminal)
        consoleOutput = findViewById(R.id.consoleOutput)
        consoleScroll = findViewById(R.id.consoleScroll)

        selectedStyle = loadSelectedStyle()
        updateStyleButton()

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { urlBar.setText(it) }
                consoleOutput.text = "Консоль JS"

                // Пока стилизация не подключена к ИИ, только фиксируем выбранный режим.
                if (selectedStyle != PageStyle.ORIGINAL) {
                    appendConsole("СТИЛЬ", "Выбран режим: ${selectedStyle.title}", Color.GRAY)
                }
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
                    val color = when (msg.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR -> Color.RED
                        ConsoleMessage.MessageLevel.WARNING -> Color.parseColor("#FFAA00")
                        else -> Color.GRAY
                    }
                    val prefix = when (msg.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR -> "ОШИБКА"
                        ConsoleMessage.MessageLevel.WARNING -> "ПРЕД"
                        ConsoleMessage.MessageLevel.LOG -> "ЛОГ"
                        ConsoleMessage.MessageLevel.DEBUG -> "ОТЛ"
                        ConsoleMessage.MessageLevel.TIP -> "СОВЕТ"
                        else -> "ИНФО"
                    }
                    val source = msg.sourceId() ?: "неизвестно"
                    appendConsole(prefix, "$source:${msg.lineNumber()} — ${msg.message()}", color)
                }
                return true
            }
        }

        webView.loadUrl(BrowserSettings.homepage)

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
            } else {
                false
            }
        }

        webView.setOnClickListener { hideKeyboard() }
        btnBack.setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        btnForward.setOnClickListener { if (webView.canGoForward()) webView.goForward() }
        btnStyle.setOnClickListener { showStyleMenu() }

        btnSettings.setOnClickListener {
            settingsChanged = true
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnTerminal.setOnClickListener {
            startActivity(Intent(this, TerminalActivity::class.java))
        }

        consoleOutput.setOnClickListener {
            consoleScroll.visibility = if (consoleScroll.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    private fun showStyleMenu() {
        val popup = PopupMenu(this, btnStyle)

        PageStyle.entries.forEachIndexed { index, style ->
            popup.menu.add(0, style.menuId, index, style.title).apply {
                isCheckable = true
                isChecked = style == selectedStyle
            }
        }
        popup.menu.setGroupCheckable(0, true, true)

        popup.setOnMenuItemClickListener { item ->
            val style = PageStyle.fromMenuId(item.itemId) ?: return@setOnMenuItemClickListener false
            selectStyle(style)
            true
        }

        popup.show()
    }

    private fun selectStyle(style: PageStyle) {
        selectedStyle = style
        saveSelectedStyle(style)
        updateStyleButton()

        val message = if (style == PageStyle.ORIGINAL) {
            "Показан исходный текст"
        } else {
            "Выбран стиль «${style.title}». ИИ-перевод подключим следующим этапом"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        appendConsole("СТИЛЬ", message, Color.GRAY)
    }

    private fun updateStyleButton() {
        btnStyle.text = selectedStyle.shortTitle
        btnStyle.contentDescription = "Стиль страницы: ${selectedStyle.title}"
    }

    private fun saveSelectedStyle(style: PageStyle) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_PAGE_STYLE, style.name)
            .apply()
    }

    private fun loadSelectedStyle(): PageStyle {
        val savedName = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_PAGE_STYLE, PageStyle.ORIGINAL.name)
        return PageStyle.entries.firstOrNull { it.name == savedName } ?: PageStyle.ORIGINAL
    }

    private fun appendConsole(prefix: String, message: String, color: Int) {
        val entry = "[$prefix] $message\n"
        val spannable = SpannableString(entry)
        spannable.setSpan(
            ForegroundColorSpan(color),
            0,
            entry.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        consoleOutput.append(spannable)
        consoleScroll.post { consoleScroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun loadUrlFromBar() {
        var input = urlBar.text.toString().trim()
        if (input.isEmpty()) return

        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            input = if (input.contains(" ") || !input.contains(".")) {
                BrowserSettings.getSearchUrl(input)
            } else {
                "https://$input"
            }
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

    private enum class PageStyle(
        val menuId: Int,
        val title: String,
        val shortTitle: String
    ) {
        ORIGINAL(100, "Исходный", "О"),
        CARLIN(101, "Карлин", "К"),
        SATIRICON(102, "Сатирикон", "С"),
        ARMY(103, "Армейский", "А");

        companion object {
            fun fromMenuId(menuId: Int): PageStyle? = entries.firstOrNull { it.menuId == menuId }
        }
    }

    companion object {
        private const val PREFS_NAME = "browser_ui"
        private const val KEY_PAGE_STYLE = "page_style"
    }
}
