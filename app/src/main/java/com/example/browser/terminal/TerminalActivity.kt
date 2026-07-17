package com.example.browser.terminal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    private val vpsHost = "46.8.221.179"
    private val vpsPort = 4101
    private val vpsUser = "oleg"
    private val sshKeyPath = "/sdcard/id_ed25519"

    private val REQUEST_STORAGE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terminal)

        terminalOutput = findViewById(R.id.terminalOutput)
        commandInput = findViewById(R.id.commandInput)
        btnSend = findViewById(R.id.btnSend)
        btnConnect = findViewById(R.id.btnConnect)
        progressBar = findViewById(R.id.progressBar)
        scrollView = findViewById(R.id.scrollView)

        btnConnect.setOnClickListener {
            checkPermissionAndConnect()
        }

        btnSend.setOnClickListener {
            val cmd = commandInput.text.toString().trim()
            if (cmd.isNotEmpty()) {
                executeCommand(cmd)
                commandInput.text.clear()
            }
        }

        // Запрашиваем разрешение при открытии
        checkPermissionAndConnect()
    }

    private fun checkPermissionAndConnect() {
        // На Android 13+ разрешение на чтение файлов не требуется для /sdcard
        // Но проверим для старых версий
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_STORAGE
                )
                return
            }
        }
        connectToVps()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectToVps()
            } else {
                appendOutput("Ошибка: нет доступа к файлам. Разреши доступ в настройках.")
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
