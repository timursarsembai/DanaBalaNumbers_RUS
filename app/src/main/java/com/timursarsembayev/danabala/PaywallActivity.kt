package com.timursarsembayev.danabalanumbers

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import android.text.InputType
import kotlin.random.Random

class PaywallActivity : AppCompatActivity() {
    private val billing by lazy { (application as DanaBalApplication).billing }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleManager.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paywall)

        val btnBuy = findViewById<Button>(R.id.btnBuy)
        val btnRestore = findViewById<Button>(R.id.btnRestore)
        val btnClose = findViewById<Button>(R.id.btnClose)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)

        // Скрытый вход для ревью: длительное нажатие по заголовку
        tvTitle.setOnLongClickListener {
            showReviewUnlockDialog()
            true
        }

        btnBuy.setOnClickListener {
            // Показываем предупреждение о родительском контроле
            showParentalControlDialog()
        }

        btnRestore.setOnClickListener {
            billing.restorePurchases()
            Toast.makeText(this, getString(R.string.paywall_restore_requested), Toast.LENGTH_SHORT).show()
        }

        btnClose.setOnClickListener { finish() }
    }

    private fun showParentalControlDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.parental_control_title))
            .setMessage(getString(R.string.parental_control_message))
            .setPositiveButton(getString(R.string.parental_access)) { _, _ ->
                showParentalVerification()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                Toast.makeText(this, getString(R.string.purchase_cancelled), Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun showParentalVerification() {
        // Создаем простую математическую задачу для взрослых
        val num1 = Random.nextInt(15, 50)
        val num2 = Random.nextInt(10, 30)
        val correctAnswer = num1 + num2

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = getString(R.string.enter_answer)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.parental_verification_title))
            .setMessage(getString(R.string.parental_verification_message, num1, num2))
            .setView(input)
            .setPositiveButton(getString(R.string.check_answer)) { _, _ ->
                val userAnswer = input.text.toString().toIntOrNull()
                if (userAnswer == correctAnswer) {
                    proceedWithPurchase()
                } else {
                    Toast.makeText(this, getString(R.string.incorrect_answer), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                Toast.makeText(this, getString(R.string.purchase_cancelled), Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun proceedWithPurchase() {
        val launched = billing.launchPurchase(this)
        if (!launched) {
            Toast.makeText(this, getString(R.string.paywall_purchase_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showReviewUnlockDialog() {
        val input = EditText(this)
        input.hint = getString(R.string.paywall_review_dialog_hint)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.paywall_review_dialog_title))
            .setView(input)
            .setPositiveButton(getString(R.string.paywall_review_unlock)) { _, _ ->
                val code = input.text?.toString()?.trim() ?: ""
                if (code.equals("REVIEW", ignoreCase = true)) {
                    billing.grantPremiumForReview()
                    Toast.makeText(this, getString(R.string.paywall_review_access_granted), Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, getString(R.string.paywall_review_invalid_code), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.paywall_review_cancel), null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        when {
            billing.isPremium() -> {
                Toast.makeText(this, getString(R.string.paywall_purchase_activated), Toast.LENGTH_SHORT).show()
                finish()
            }
            billing.isPending() -> {
                Toast.makeText(this, getString(R.string.paywall_purchase_pending), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
