package com.timursarsembayev.danabalanumbers

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.EditText
import androidx.appcompat.app.AlertDialog

class PaywallActivity : AppCompatActivity() {
    private val billing by lazy { (application as DanaBalApplication).billing }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paywall)

        val btnBuy = findViewById<Button>(R.id.btnBuy)
        val btnRestore = findViewById<Button>(R.id.btnRestore)
        val btnClose = findViewById<Button>(R.id.btnClose)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)

        // Скрытый вход для ревью: длительное нажатие по загол��вку
        tvTitle.setOnLongClickListener {
            showReviewUnlockDialog()
            true
        }

        btnBuy.setOnClickListener {
            val launched = billing.launchPurchase(this)
            if (!launched) {
                Toast.makeText(this, "Покупка пока недоступна. Попробуйте позже.", Toast.LENGTH_SHORT).show()
            }
        }
        btnRestore.setOnClickListener {
            billing.restorePurchases()
            Toast.makeText(this, "Запрос на восстановление отправлен", Toast.LENGTH_SHORT).show()
        }
        btnClose.setOnClickListener { finish() }
    }

    private fun showReviewUnlockDialog() {
        val input = EditText(this)
        input.hint = "Enter code"
        AlertDialog.Builder(this)
            .setTitle("Enter access code")
            .setView(input)
            .setPositiveButton("Unlock") { _, _ ->
                val code = input.text?.toString()?.trim() ?: ""
                if (code.equals("REVIEW", ignoreCase = true)) {
                    billing.grantPremiumForReview()
                    Toast.makeText(this, "Full access enabled for review", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Invalid code", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (billing.isPremium()) {
            Toast.makeText(this, "Покупка активирована. Спасибо!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
