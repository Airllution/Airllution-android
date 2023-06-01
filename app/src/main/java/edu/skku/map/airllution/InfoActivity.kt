package edu.skku.map.airllution

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton

class InfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        val backButton = findViewById<ImageButton>(R.id.infoBackButton)
        backButton.setOnClickListener {
            finish()
        }
    }
}