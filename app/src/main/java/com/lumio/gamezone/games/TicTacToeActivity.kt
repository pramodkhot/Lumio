package com.lumio.gamezone.games

import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.*
import android.widget.*
import com.lumio.gamezone.ui.BaseGameActivity

class TicTacToeActivity : BaseGameActivity() {

    private val board = Array(3) { Array(3) { 0 } }
    private var current = 1
    private var gameOver = false
    private val scores = intArrayOf(0, 0, 0)
    private lateinit var cells: Array<Array<TextView>>
    private lateinit var tvStatus: TextView
    private lateinit var tvScore: TextView
    private lateinit var celebrationView: TextView
    private lateinit var rootLayout: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "TIC TAC TOE"

        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(0xFF080614.toInt())
        }

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 40, 24, 24)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        tvScore = TextView(this).apply {
            text = "X: 0   DRAW: 0   O: 0"
            textSize = 16f
            setTextColor(0xFF00E5FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
            typeface = Typeface.DEFAULT_BOLD
        }
        main.addView(tvScore)

        tvStatus = TextView(this).apply {
            text = "Player X's Turn"
            textSize = 18f
            setTextColor(0xFFFFD700.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
            typeface = Typeface.DEFAULT_BOLD
        }
        main.addView(tvStatus)

        // Board
        val boardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }
        val screenW = resources.displayMetrics.widthPixels
        val cellSize = (screenW - 80) / 3

        cells = Array(3) { r ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            Array(3) { c ->
                TextView(this).apply {
                    textSize = 52f
                    gravity = Gravity.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(0xFF00E5FF.toInt())
                    setBackgroundColor(0xFF0F1435.toInt())
                    layoutParams = LinearLayout.LayoutParams(cellSize, cellSize).apply {
                        setMargins(5, 5, 5, 5)
                    }
                    setOnClickListener { onCellClick(r, c) }
                    row.addView(this)
                }
            }.also { boardContainer.addView(row) }
        }
        main.addView(boardContainer)

        val btnNew = Button(this).apply {
            text = "NEW ROUND"
            textSize = 14f
            setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFF00E5FF.toInt())
            setPadding(40, 16, 40, 16)
            setOnClickListener { resetBoard() }
        }
        main.addView(btnNew)

        // Celebration overlay
        celebrationView = TextView(this).apply {
            textSize = 64f
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { gravity = Gravity.CENTER }
        }

        rootLayout.addView(main)
        rootLayout.addView(celebrationView)
        setContentView(rootLayout)
    }

    private fun onCellClick(r: Int, c: Int) {
        if (gameOver || board[r][c] != 0) return
        board[r][c] = current
        cells[r][c].text = if (current == 1) "X" else "O"
        cells[r][c].setTextColor(if (current == 1) 0xFF00E5FF.toInt() else 0xFFFF4081.toInt())

        // Bounce animation
        val bounce = ScaleAnimation(0.5f, 1.1f, 0.5f, 1.1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 200; fillAfter = true
        }
        cells[r][c].startAnimation(bounce)

        val result = checkWinner()
        if (result != 0) { endGame(result); return }
        current = if (current == 1) 2 else 1
        tvStatus.text = "Player ${if (current == 1) "X" else "O"}'s Turn"
        tvStatus.setTextColor(if (current == 1) 0xFF00E5FF.toInt() else 0xFFFF4081.toInt())
    }

    private fun checkWinner(): Int {
        val lines = listOf(
            listOf(0 to 0, 0 to 1, 0 to 2), listOf(1 to 0, 1 to 1, 1 to 2),
            listOf(2 to 0, 2 to 1, 2 to 2), listOf(0 to 0, 1 to 0, 2 to 0),
            listOf(0 to 1, 1 to 1, 2 to 1), listOf(0 to 2, 1 to 2, 2 to 2),
            listOf(0 to 0, 1 to 1, 2 to 2), listOf(0 to 2, 1 to 1, 2 to 0)
        )
        for (line in lines) {
            val vals = line.map { board[it.first][it.second] }
            if (vals.all { it == 1 }) { highlightWin(line, 0xFF00E5FF.toInt()); return 1 }
            if (vals.all { it == 2 }) { highlightWin(line, 0xFFFF4081.toInt()); return 2 }
        }
        if (board.all { row -> row.all { it != 0 } }) return 3
        return 0
    }

    private fun highlightWin(line: List<Pair<Int,Int>>, color: Int) {
        line.forEach { (r, c) ->
            cells[r][c].setBackgroundColor(color and 0x00FFFFFF or 0x33000000)
            cells[r][c].setTextColor(color)
        }
    }

    private fun endGame(result: Int) {
        gameOver = true
        when (result) {
            1 -> { scores[0]++; showCelebration("🎉\nX WINS!\n🏆", 0xFF00E5FF.toInt()); tvStatus.text = "X Wins! 🏆" }
            2 -> { scores[1]++; showCelebration("🎉\nO WINS!\n🏆", 0xFFFF4081.toInt()); tvStatus.text = "O Wins! 🏆" }
            3 -> { scores[2]++; showCelebration("🤝\nDRAW!\n😅", 0xFFFFD700.toInt()); tvStatus.text = "Draw! 🤝" }
        }
        updateScore()
    }

    private fun showCelebration(msg: String, color: Int) {
        celebrationView.apply {
            text = msg
            setTextColor(color)
            setBackgroundColor(color and 0x00FFFFFF or 0xCC000000.toInt())
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f).setDuration(300).start()
        }
        // Auto-dismiss after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            celebrationView.animate().alpha(0f).setDuration(300).withEndAction {
                celebrationView.visibility = View.GONE
            }.start()
        }, 2000)
    }

    private fun updateScore() {
        tvScore.text = "X: ${scores[0]}   DRAW: ${scores[2]}   O: ${scores[1]}"
    }

    private fun resetBoard() {
        board.forEach { row -> row.fill(0) }
        cells.forEach { row -> row.forEach {
            it.text = ""
            it.setBackgroundColor(0xFF0F1435.toInt())
        }}
        current = 1; gameOver = false
        tvStatus.text = "Player X's Turn"
        tvStatus.setTextColor(0xFFFFD700.toInt())
        celebrationView.visibility = View.GONE
    }
}
