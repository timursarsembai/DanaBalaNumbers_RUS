package com.example.danabala

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MathExercisesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_math_exercises)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupExerciseCards()
    }

    private fun setupExerciseCards() {
        findViewById<CardView>(R.id.cardNumbers).setOnClickListener {
            val intent = Intent(this, NumberRecognitionActivity::class.java)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardCounting).setOnClickListener {
            val intent = Intent(this, CountingActivity::class.java)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardObjectCounting).setOnClickListener {
            val intent = Intent(this, ObjectCountingActivity::class.java)
            startActivity(intent)
        }
    }
}
