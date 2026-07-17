package com.example.browser.terminal

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.browser.R
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

    // Настройки подключения (позже вынесем в BrowserSettings)
    private val vpsHost = "46.8.221.179"
    private val vpsPort = 4101
    private val vpsUser = "oleg"
    private val sshKeyPath = "/sdcard/id_ed25519"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terminal)

        terminalOutput = findViewById(R.id.terminalOutput)
        commandInput = findViewById(R.id.commandInput)
        btnSend = findViewById(R.id.btnSend)
        btnConnect = findViewById(R.id.btnConnect)
        progressBar = findViewById(R.id.progressBar)
        scrollView = findViewById(R.id.scrollView)

        // Подключение при открытии
        connectToVps()

        btnConnect.setOnClickListener { connectToVps() }

        btnSend.setOnClickListener {
            val cmd = commandInput.text.toString().trim()
            if (cmd.isNotEmpty()) {
                executeCommand(cmd)
                commandInput.text.clear()
            }
        }
    }

    private fun connectToVps() {
        appendOutput("Подключение к $vpsHost:$vpsPort...")
        progressBar.visibility = View.VISIBLE
        btnConnect.isEnabled = false

        lifecycleScope.launch {
            val result = SshClient.connect(vpsHost, vpsPort, vpsUser, sshKeyPath)
            progressBar.visibility = View.GONE
            btnConnect.isEnabled = true

            result.onSuccess {
                appendOutput(it)
                appendOutput("Готов к работе. Введи команду.")
            }.onFailure {
                appendOutput("Ошибка подключения: ${it.message}")
            }
        }
    }

    private fun executeCommand(command: String) {
        appendOutput("\n> $command")
        progressBar.visibility = View.VISIBLE
        btnSend.isEnabled = false

        lifecycleScope.launch {
            val result = SshClient.execute(command)
            progressBar.visibility = View.GONE
            btnSend.isEnabled = true

            result.onSuccess {
                appendOutput(it)
            }.onFailure {
                appendOutput("Ошибка: ${it.message}")
            }
        }
    }

    private fun appendOutput(text: String) {
        terminalOutput.append("$text\n")
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            SshClient.disconnect()
        }
    }
}
