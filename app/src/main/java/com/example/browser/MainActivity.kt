package com.example.browser

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

                
                Handler(Looper.getMainLooper()).postDelayed({
                    applyVisibleStyleTest()
                }, 700)

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
        applySelectedStyleToPage()
    }

    private fun applySelectedStyleToPage() {
        val styleName = selectedStyle.name

        val script = """
            (function () {
                const STYLE = '$styleName';
                const BADGE_ID = 'browser-style-badge';

                function removeBadge() {
                    const badge = document.getElementById(BADGE_ID);
                    if (badge) badge.remove();
                }

                function showBadge(title) {
                    removeBadge();

                    const badge = document.createElement('div');
                    badge.id = BADGE_ID;
                    badge.textContent = 'Тестовый локальный стиль: ' + title;
                    badge.style.position = 'fixed';
                    badge.style.left = '10px';
                    badge.style.bottom = '10px';
                    badge.style.zIndex = '2147483647';
                    badge.style.padding = '7px 10px';
                    badge.style.borderRadius = '8px';
                    badge.style.background = 'rgba(25,25,25,.88)';
                    badge.style.color = '#fff';
                    badge.style.font = '13px sans-serif';
                    badge.style.boxShadow = '0 2px 8px rgba(0,0,0,.35)';
                    document.documentElement.appendChild(badge);
                }

                function collectTextNodes() {
                    if (window.__browserStyleNodes) return window.__browserStyleNodes;

                    const allowed = new Set([
                        'P', 'LI', 'BLOCKQUOTE',
                        'H1', 'H2', 'H3', 'H4',
                        'TD', 'TH', 'FIGCAPTION'
                    ]);

                    const nodes = [];
                    const walker = document.createTreeWalker(
                        document.body,
                        NodeFilter.SHOW_TEXT,
                        {
                            acceptNode(node) {
                                const parent = node.parentElement;
                                if (!parent || !allowed.has(parent.tagName)) {
                                    return NodeFilter.FILTER_REJECT;
                                }

                                const value = node.nodeValue || '';
                                if (value.trim().length < 35) {
                                    return NodeFilter.FILTER_REJECT;
                                }

                                return NodeFilter.FILTER_ACCEPT;
                            }
                        }
                    );

                    let node;
                    while ((node = walker.nextNode())) {
                        nodes.push({
                            node: node,
                            original: node.nodeValue
                        });
                    }

                    window.__browserStyleNodes = nodes;
                    return nodes;
                }

                function restore() {
                    const nodes = collectTextNodes();
                    nodes.forEach(item => {
                        if (item.node && item.node.isConnected) {
                            item.node.nodeValue = item.original;
                        }
                    });
                    removeBadge();
                    return nodes.length;
                }

                function preserveOuterSpaces(source, changed) {
                    const left = (source.match(/^\\s*/) || [''])[0];
                    const right = (source.match(/\\s*$/) || [''])[0];
                    return left + changed.trim() + right;
                }

                function army(source) {
                    let value = source
                        .replace(/в целях/gi, 'чтобы')
                        .replace(/необходимо осуществить/gi, 'нужно')
                        .replace(/необходимо/gi, 'нужно')
                        .replace(/следует произвести/gi, 'сделай')
                        .replace(/осуществляется/gi, 'делается')
                        .replace(/осуществить/gi, 'сделать')
                        .replace(/посредством/gi, 'через')
                        .replace(/в случае возникновения/gi, 'если возникнет')
                        .replace(/настоящий документ/gi, 'этот документ')
                        .replace(/данный/gi, 'этот')
                        .replace(/\\s+/g, ' ');

                    value = value.replace(/([.!?])\\s+/g, '$1\\n');
                    return preserveOuterSpaces(source, value);
                }

                function carlin(source) {
                    const value = source
                        .replace(/официальные лица/gi, 'люди с официальными лицами')
                        .replace(/было принято решение/gi, 'кто-то наверху решил')
                        .replace(/сообщается, что/gi, 'нам торжественно сообщают, что')
                        .replace(/в целях/gi, 'якобы ради того, чтобы')
                        .replace(/оптимизация/gi, 'магическое слово «оптимизация»')
                        .replace(/необходимо/gi, 'нам, разумеется, необходимо')
                        .replace(/\\s+/g, ' ');

                    return preserveOuterSpaces(source, value);
                }

                function satiricon(source) {
                    const value = source
                        .replace(/правительство/gi, 'почтенное правительство')
                        .replace(/чиновники/gi, 'служилые мужи')
                        .replace(/министерство/gi, 'высокое министерство')
                        .replace(/было принято решение/gi, 'совет мудрейших постановил')
                        .replace(/сообщило/gi, 'изволило сообщить')
                        .replace(/пользователи/gi, 'почтенная публика')
                        .replace(/граждане/gi, 'обыватели')
                        .replace(/\\s+/g, ' ');

                    return preserveOuterSpaces(source, value);
                }

                const count = restore();

                if (STYLE === 'ORIGINAL') {
                    console.log('BrowserStyle: восстановлен исходный текст, блоков: ' + count);
                    return 'ORIGINAL:' + count;
                }

                const nodes = collectTextNodes();
                nodes.forEach(item => {
                    if (!item.node || !item.node.isConnected) return;

                    if (STYLE === 'ARMY') {
                        item.node.nodeValue = army(item.original);
                    } else if (STYLE === 'CARLIN') {
                        item.node.nodeValue = carlin(item.original);
                    } else if (STYLE === 'SATIRICON') {
                        item.node.nodeValue = satiricon(item.original);
                    }
                });

                const titles = {
                    CARLIN: 'Карлин',
                    SATIRICON: 'Сатирикон',
                    ARMY: 'Армейский'
                };

                showBadge(titles[STYLE] || STYLE);
                console.log('BrowserStyle: применён ' + STYLE + ', блоков: ' + nodes.length);
                return STYLE + ':' + nodes.length;
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            appendConsole(
                "СТИЛЬ",
                "Обработано: ${result ?: "нет ответа"}",
                Color.GRAY
            )
        }
    }


    private fun applyVisibleStyleTest() {
        val styleCode = selectedStyle.name

        val script = """
            (function () {
                const style = '$styleCode';
                const badgeId = '__browser_style_test_badge';

                if (!window.__browserOriginalTexts) {
                    window.__browserOriginalTexts = [];
                }

                function removeBadge() {
                    const oldBadge = document.getElementById(badgeId);
                    if (oldBadge) oldBadge.remove();
                }

                function restoreOriginal() {
                    window.__browserOriginalTexts.forEach(item => {
                        if (item.node && item.node.isConnected) {
                            item.node.nodeValue = item.text;
                        }
                    });
                }

                if (window.__browserOriginalTexts.length === 0) {
                    const tags = new Set([
                        'P', 'LI', 'H1', 'H2', 'H3', 'H4',
                        'BLOCKQUOTE', 'TD', 'TH', 'FIGCAPTION'
                    ]);

                    const walker = document.createTreeWalker(
                        document.body,
                        NodeFilter.SHOW_TEXT
                    );

                    let node;
                    while ((node = walker.nextNode())) {
                        const parent = node.parentElement;
                        const value = node.nodeValue || '';

                        if (!parent || value.trim().length < 20) {
                            continue;
                        }

                        if (parent.closest(
                            'script, style, noscript, textarea, input, button, select'
                        )) {
                            continue;
                        }

                        const textBlock = parent.closest(
                            'p, li, h1, h2, h3, h4, blockquote, td, th, figcaption, article'
                        );

                        if (textBlock) {
                            window.__browserOriginalTexts.push({
                                node: node,
                                text: value
                            });
                        }
                    }
                }

                restoreOriginal();
                removeBadge();

                if (style === 'ORIGINAL') {
                    return 'Исходный: ' + window.__browserOriginalTexts.length;
                }

                const labels = {
                    CARLIN: '[КАРЛИН] ',
                    SATIRICON: '[САТИРИКОН] ',
                    ARMY: '[АРМЕЙСКИЙ] '
                };

                const titles = {
                    CARLIN: 'Карлин',
                    SATIRICON: 'Сатирикон',
                    ARMY: 'Армейский'
                };

                const label = labels[style] || '[СТИЛЬ] ';

                window.__browserOriginalTexts.forEach(item => {
                    if (item.node && item.node.isConnected) {
                        item.node.nodeValue = label + item.text;
                    }
                });

                const badge = document.createElement('div');
                badge.id = badgeId;
                badge.textContent =
                    'Режим работает: ' +
                    (titles[style] || style) +
                    ' · блоков: ' +
                    window.__browserOriginalTexts.length;

                badge.style.position = 'fixed';
                badge.style.left = '12px';
                badge.style.bottom = '12px';
                badge.style.zIndex = '2147483647';
                badge.style.background = '#111';
                badge.style.color = '#fff';
                badge.style.padding = '10px 14px';
                badge.style.borderRadius = '9px';
                badge.style.font = 'bold 14px sans-serif';
                badge.style.boxShadow = '0 3px 12px rgba(0,0,0,.45)';

                document.documentElement.appendChild(badge);

                return style + ': ' + window.__browserOriginalTexts.length;
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            appendConsole(
                "СТИЛЬ-ТЕСТ",
                "Результат: ${result ?: "нет ответа"}",
                Color.GRAY
            )
        }
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
