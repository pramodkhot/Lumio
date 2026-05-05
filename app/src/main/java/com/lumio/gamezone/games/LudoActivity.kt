package com.lumio.gamezone.games

import android.os.Bundle
import android.view.*
import android.widget.*
import com.lumio.gamezone.ui.BaseGameActivity

class LudoActivity : BaseGameActivity() {
    private val COLORS = arrayOf("RED","BLUE","GREEN","YELLOW")
    private val COLORS_INT = intArrayOf(0xFFFF5252.toInt(),0xFF448AFF.toInt(),0xFF00E676.toInt(),0xFFFFD700.toInt())
    private var numPlayers = 4
    private var tokens = Array(4) { IntArray(4) { -1 } } // position -1=home, 0-56=board, 57=done
    private var current = 0; private var dice = 0; private var rolled = false
    private lateinit var tvStatus: TextView; private lateinit var tvDice: TextView
    private lateinit var tokenBtns: Array<Array<Button>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "LUDO"

        val scroll = ScrollView(this).apply { setBackgroundColor(0xFF080614.toInt()) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF080614.toInt())
            setPadding(20,20,20,20); gravity = Gravity.CENTER
        }

        tvStatus = TextView(this).apply {
            textSize = 16f; setTextColor(COLORS_INT[current])
            gravity = Gravity.CENTER; setPadding(0,0,0,12); typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        root.addView(tvStatus)

        tvDice = TextView(this).apply {
            textSize = 48f; setTextColor(0xFF00E5FF.toInt())
            gravity = Gravity.CENTER; setPadding(0,8,0,8)
        }
        root.addView(tvDice)

        val btnRoll = Button(this).apply {
            text = "🎲 ROLL DICE"; setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFF00E5FF.toInt())
            setOnClickListener { rollDice() }
        }
        root.addView(btnRoll)

        val grid = GridLayout(this).apply {
            rowCount = (numPlayers * 4 + 3) / 4; columnCount = 4
            setPadding(0, 16, 0, 0)
        }
        tokenBtns = Array(numPlayers) { p ->
            Array(4) { t ->
                Button(this).apply {
                    textSize = 11f
                    setTextColor(0xFFFFFFFF.toInt())
                    setBackgroundColor(COLORS_INT[p])
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0; height = ViewGroup.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(t, 1, 1f)
                        rowSpec = GridLayout.spec(p, 1f)
                        setMargins(4,4,4,4)
                    }
                    setOnClickListener { moveToken(p, t) }
                }
            }
        }
        (0 until numPlayers).forEach { p -> tokenBtns[p].forEach { grid.addView(it) } }
        root.addView(grid)

        scroll.addView(root)
        setContentView(scroll)
        updateUI()
    }

    private fun rollDice() {
        if (rolled) return
        dice = (1..6).random(); rolled = true
        tvDice.text = "$dice"
        updateUI()
    }

    private fun moveToken(p: Int, t: Int) {
        if (p != current || !rolled) return
        val pos = tokens[p][t]
        if (pos == 57) return
        if (pos == -1 && dice != 6) return
        if (pos == -1) tokens[p][t] = 0
        else tokens[p][t] = minOf(pos + dice, 57)
        rolled = false; dice = 0
        if (tokens[p].all { it == 57 }) {
            tvStatus.text = "${COLORS[p]} WINS! 🏆"
            tvDice.text = ""; return
        }
        if (dice != 6) current = (current + 1) % numPlayers
        updateUI()
    }

    private fun updateUI() {
        tvStatus.text = "${COLORS[current]}'S TURN"
        tvStatus.setTextColor(COLORS_INT[current])
        (0 until numPlayers).forEach { p ->
            (0 until 4).forEach { t ->
                val pos = tokens[p][t]
                tokenBtns[p][t].text = when (pos) { -1 -> "🏠 T${t+1}"; 57 -> "✅ T${t+1}"; else -> "⬡ $pos" }
                tokenBtns[p][t].alpha = if (p == current && rolled && canMove(p,t)) 1f else 0.6f
            }
        }
    }

    private fun canMove(p: Int, t: Int): Boolean {
        val pos = tokens[p][t]
        return pos != 57 && (pos != -1 || dice == 6) && (pos == -1 || pos + dice <= 57)
    }
}
