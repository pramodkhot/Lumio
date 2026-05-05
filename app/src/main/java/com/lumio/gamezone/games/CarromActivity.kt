package com.lumio.gamezone.games

import android.os.Bundle
import android.graphics.*
import android.view.*
import android.widget.*
import com.lumio.gamezone.ui.BaseGameActivity

class CarromActivity : BaseGameActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "CARROM"
        setContentView(buildCarromView())
    }

    private fun buildCarromView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF080614.toInt())
            gravity = Gravity.CENTER
            setPadding(24, 40, 24, 24)
        }
        val tvTitle = TextView(this).apply {
            text = "CARROM"
            textSize = 28f; setTextColor(0xFF00E5FF.toInt())
            gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
        }
        val tvDesc = TextView(this).apply {
            text = "Game coming soon!\nFull implementation in next update."
            textSize = 16f; setTextColor(0xFF8892B0.toInt())
            gravity = Gravity.CENTER; setPadding(0, 0, 0, 32)
        }
        val btn = Button(this).apply {
            text = "← BACK"
            setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFF00E5FF.toInt())
            setOnClickListener { finish() }
        }
        root.addView(tvTitle)
        root.addView(tvDesc)
        root.addView(btn)
        return root
    }
}
