package com.example.browser.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.browser.R
import com.example.browser.settings.BrowserSettings
import kotlinx.coroutines.launch

/**
 * Экран терминала для управления VPS.
 */
class TerminalActivity : AppCompatActivity() {
    private lateinit var terminalOutput: TextView
    private lateinit var commandInput: EditText
    private lateinit var btnSend: Button
    private lateinit var btnConnect: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terminal)

        terminalOutput = findViewById(R.id.terminalOutput)
        commandInput = findViewById(R.id.commandInput)
        btnSend = findViewById(R.id.btnSend)
        btnConnect = findViewById(R.id.btnConnect)
        progressBar = findViewById(R.id.progressBar)
        scrollView = findViewById(R.id.scrollView)

        val btnTab = findViewById<Button>(R.id.btnTab)
        val btnEsc = findViewById<Button>(R.id.btnEsc)
        val btnCtrlC = findViewById<Button>(R.id.btnCtrlC)
        val btnCopy = findViewById<Button>(R.id.btnCopy)
        val btnPaste = findViewById<Button>(R.id.btnPaste)
        val btnEnter = findViewById<Button>(R.id.btnEnter)

        // Tab — вставляет символ табуляции
        btnTab.setOnClickListener {
            val pos = commandInput.selectionStart
            val text = commandInput.text
            text.insert(pos, "\t")
            commandInput.setSelection(pos + 1)
        }

        // Esc — отправляет escape-последовательность (прерывает текущую операцию)
        btnEsc.setOnClickListener {
            executeCommand("\u001B")
        }

        // Ctrl+C — отправляет SIGINT
        btnCtrlC.setOnClickListener {
            executeCommand("\u0003")
        }

        // Копировать — копирует весь вывод терминала в буфер обмена
        btnCopy.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("terminal", terminalOutput.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Вывод скопирован", Toast.LENGTH_SHORT).show()
        }

        // Вставить — вставляет текст из буфера обмена в поле ввода
        btnPaste.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text ?: ""
                val pos = commandInput.selectionStart
                commandInput.text.insert(pos, text)
                commandInput.setSelection(pos + text.length)
            }
        }

        // Enter — отправляет команду из поля ввода
        btnEnter.setOnClickListener {
            val cmd = commandInput.text.toString().trim()
            if (cmd.isNotEmpty()) {
                executeCommand(cmd)
                commandInput.text.clear()
            }
        }

        btnConnect.setOnClickListener { connectToVps() }
        btnSend.setOnClickListener {
            val cmd = commandInput.text.toString().trim()
            if (cmd.isNotEmpty()) {
                executeCommand(cmd)
                commandInput.text.clear()
            }
        }

        connectToVps()
    }

    private fun connectToVps() {
        val host = BrowserSettings.vpsHost
        val port = BrowserSettings.vpsPort
        val user = BrowserSettings.vpsUser
        val keyPath = BrowserSettings.vpsKeyPath

        appendOutput("Подключение к $host:$port...")
        progressBar.visibility = View.VISIBLE
        btnConnect.isEnabled = false

        lifecycleScope.launch {
            val result = SshClient.connect(host, port, user, keyPath)
            progressBar.visibility = View.GONE
            btnConnect.isEnabled = true

            result.onSuccess {
                appendOutput(it)
                appendOutput("Готов к работе.")
            }.onFailure {
                appendOutput("Ошибка подключения: ${it.message}")
            }
        }
    }

    private fun executeCommand(command: String) {
        appendOutput("\n> $command")
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = SshClient.execute(command)
            progressBar.visibility = View.GONE

            result.onSuccess { appendOutput(it) }
                .onFailure { appendOutput("Ошибка: ${it.message}") }
        }
    }

    private fun appendOutput(text: String) {
        terminalOutput.append("$text\n")
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch { SshClient.disconnect() }
    }
}
