package com.timursarsembayev.danabalanumbers

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

class LanguageSettingsActivity : AppCompatActivity() {

    private lateinit var languageListContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupLanguageList()
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

    private fun setupLanguageList() {
        languageListContainer = findViewById(R.id.languageListContainer)
        languageListContainer.removeAllViews()

        val currentLanguage = LocaleManager.getCurrentLanguage(this)
        val supported = LocaleManager.getSupportedLanguages()

        supported.forEach { code ->
            languageListContainer.addView(createLanguageItem(code, code == currentLanguage))
        }
    }

    private fun createLanguageItem(languageCode: String, selected: Boolean): View {
        val context = this
        val card = MaterialCardView(context)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.setMargins(dp(8), dp(6), dp(8), dp(6))
        card.layoutParams = lp
        card.isClickable = true
        card.isFocusable = true
        card.radius = dp(14).toFloat()
        card.cardElevation = dp(1).toFloat()
        card.strokeWidth = 0 // без рамки, минимализм
        card.tag = languageCode
        // Фон с учётом выбранного состояния
        card.setCardBackgroundColor(ContextCompat.getColor(context, if (selected) R.color.language_selected_bg else R.color.language_unselected_bg))

        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(14), dp(12), dp(14), dp(12))

        val flag = ImageView(context)
        val flagParams = LinearLayout.LayoutParams(dp(36), dp(24))
        flag.layoutParams = flagParams
        flag.setImageResource(LocaleManager.getLanguageFlagRes(languageCode))

        val title = TextView(context)
        val titleParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        title.layoutParams = titleParams
        title.text = LocaleManager.getLanguageDisplayName(languageCode)
        title.textSize = 18f
        title.setPadding(dp(12), 0, 0, 0)
        title.setTextColor(ContextCompat.getColor(context, if (selected) R.color.primaryColor else R.color.text_color))

        row.addView(flag)
        row.addView(title)
        card.addView(row)

        card.setOnClickListener {
            val selectedLanguage = it.tag as String
            if (LocaleManager.shouldRecreateActivity(this, selectedLanguage)) {
                LocaleManager.setLanguage(this, selectedLanguage)
                // Полный перезапуск приложения с очисткой стека
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finishAffinity()
            } else {
                updateSelectionUI(selectedLanguage)
            }
        }

        return card
    }

    private fun updateSelectionUI(selectedCode: String) {
        for (i in 0 until languageListContainer.childCount) {
            val card = languageListContainer.getChildAt(i) as? MaterialCardView ?: continue
            val code = card.tag as? String
            val selected = code == selectedCode
            // Обновляем фон карточки
            card.setCardBackgroundColor(ContextCompat.getColor(this, if (selected) R.color.language_selected_bg else R.color.language_unselected_bg))
            // Обновляем цвет текста
            val row = card.getChildAt(0) as? LinearLayout
            val title = row?.getChildAt(1) as? TextView
            title?.setTextColor(ContextCompat.getColor(this, if (selected) R.color.primaryColor else R.color.text_color))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun attachBaseContext(newBase: android.content.Context?) {
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }
}
