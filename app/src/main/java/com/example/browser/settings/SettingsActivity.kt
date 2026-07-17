package com.example.browser.settings

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.example.browser.R

/**
 * Экран настроек браузера и VPS.
 */
class SettingsActivity : AppCompatActivity() {
    private lateinit var etHomepage: EditText
    private lateinit var spinnerSearch: Spinner
    private lateinit var swAdBlock: Switch
    private lateinit var etVpsHost: EditText
    private lateinit var etVpsPort: EditText
    private lateinit var etVpsUser: EditText
    private lateinit var etVpsKeyPath: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etHomepage = findViewById(R.id.etHomepage)
        spinnerSearch = findViewById(R.id.spinnerSearch)
        swAdBlock = findViewById(R.id.swAdBlock)
        etVpsHost = findViewById(R.id.etVpsHost)
        etVpsPort = findViewById(R.id.etVpsPort)
        etVpsUser = findViewById(R.id.etVpsUser)
        etVpsKeyPath = findViewById(R.id.etVpsKeyPath)
        btnSave = findViewById(R.id.btnSave)

        // Текущие значения
        etHomepage.setText(BrowserSettings.homepage)

        val engines = listOf("Google", "Yandex", "DuckDuckGo")
        val engineKeys = listOf("google", "yandex", "duckduckgo")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, engines)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSearch.adapter = adapter
        spinnerSearch.setSelection(engineKeys.indexOf(BrowserSettings.searchEngine).coerceAtLeast(0))

        swAdBlock.isChecked = BrowserSettings.adBlockEnabled

        etVpsHost.setText(BrowserSettings.vpsHost)
        etVpsPort.setText(BrowserSettings.vpsPort.toString())
        etVpsUser.setText(BrowserSettings.vpsUser)
        etVpsKeyPath.setText(BrowserSettings.vpsKeyPath)

        btnSave.setOnClickListener {
            BrowserSettings.homepage = etHomepage.text.toString().trim()
            BrowserSettings.searchEngine = engineKeys[spinnerSearch.selectedItemPosition]
            BrowserSettings.adBlockEnabled = swAdBlock.isChecked
            BrowserSettings.vpsHost = etVpsHost.text.toString().trim()
            BrowserSettings.vpsPort = etVpsPort.text.toString().toIntOrNull() ?: 22
            BrowserSettings.vpsUser = etVpsUser.text.toString().trim()
            BrowserSettings.vpsKeyPath = etVpsKeyPath.text.toString().trim()
            finish()
        }
    }
}
