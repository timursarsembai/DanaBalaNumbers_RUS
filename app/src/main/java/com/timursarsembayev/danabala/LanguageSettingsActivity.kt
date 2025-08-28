package com.timursarsembayev.danabalanumbers

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LanguageSettingsActivity : AppCompatActivity() {

    private lateinit var languageRadioGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupLanguageSelection()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.language_settings_title)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupLanguageSelection() {
        languageRadioGroup = findViewById(R.id.languageRadioGroup)

        val currentLanguage = LocaleManager.getCurrentLanguage(this)

        // Создаем радио кнопки для каждого поддерживаемого языка
        LocaleManager.getSupportedLanguages().forEachIndexed { index, languageCode ->
            val radioButton = RadioButton(this).apply {
                id = index
                text = LocaleManager.getLanguageDisplayName(languageCode)
                tag = languageCode
                textSize = 18f
                setPadding(16, 16, 16, 16)
                isChecked = languageCode == currentLanguage
            }
            languageRadioGroup.addView(radioButton)
        }

        languageRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedRadioButton = findViewById<RadioButton>(checkedId)
            val selectedLanguage = selectedRadioButton.tag as String

            if (LocaleManager.shouldRecreateActivity(this, selectedLanguage)) {
                LocaleManager.setLanguage(this, selectedLanguage)

                // Перезапускаем приложение с новым языком
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    override fun attachBaseContext(newBase: android.content.Context?) {
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }
}
