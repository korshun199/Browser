package com.example.browser.settings

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.example.browser.R

/**
 * Экран настроек браузера.
 */
class SettingsActivity : AppCompatActivity() {
    private lateinit var etHomepage: EditText
    private lateinit var spinnerSearch: Spinner
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etHomepage = findViewById(R.id.etHomepage)
        spinnerSearch = findViewById(R.id.spinnerSearch)
        btnSave = findViewById(R.id.btnSave)

        // Заполняем текущими значениями
        etHomepage.setText(BrowserSettings.homepage)

        // Список поисковиков
        val engines = listOf("Google", "Yandex", "DuckDuckGo")
        val engineKeys = listOf("google", "yandex", "duckduckgo")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, engines)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSearch.adapter = adapter

        // Выбираем текущий поисковик
        val currentEngine = BrowserSettings.searchEngine
        spinnerSearch.setSelection(engineKeys.indexOf(currentEngine).coerceAtLeast(0))

        // Сохранение
        btnSave.setOnClickListener {
            BrowserSettings.homepage = etHomepage.text.toString().trim()
            BrowserSettings.searchEngine = engineKeys[spinnerSearch.selectedItemPosition]
            finish()
        }
    }
}
