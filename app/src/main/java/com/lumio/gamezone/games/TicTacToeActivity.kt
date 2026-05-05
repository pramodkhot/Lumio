package com.lumio.gamezone.games

import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.*
import com.lumio.gamezone.ui.BaseGameActivity

class TicTacToeActivity : BaseGameActivity() {

    private val board = Array(3) { Array(3) { 0 } } // 0=empty 1=X 2=O
    private var current = 1
    private var gameOver = false
    private val scores = intArrayOf(0, 0, 0) // X, O, draws
    private lateinit var cells: Array<Array<TextView>>
    private lateinit var tvStatus: TextView
    private lateinit var tvScore: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "TIC TAC TOE"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF080614.toInt())
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
        }

        tvScore = TextView(this).apply {
            text = "X: 0   DRAW: 0   O: 0"
            textSize = 14f; setTextColor(0xFF00E5FF.toInt())
            gravity = Gravity.CENTER; setPadding(0, 0, 0, 16)
        }
        root.addView(tvScore)

        tvStatus = TextView(this).apply {
            text = "Player X's Turn"
            textSize = 16f; setTextColor(0xFFFFD700.toInt())
            gravity = Gravity.CENTER; setPadding(0, 0, 0, 24)
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(tvStatus)

        // 3x3 grid
        val grid = GridLayout(this).apply {
            rowCount = 3; columnCount = 3
            setPadding(0, 0, 0, 24)
        }
        cells = Array(3) { r ->
            Array(3) { c ->
                TextView(this).apply {
                    textSize = 36f; gravity = Gravity.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(0xFF00E5FF.toInt())
                    setBackgroundColor(0xFF0F1435.toInt())
                    val size = (resources.displayMetrics.widthPixels - 120) / 3
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = size; height = size
                        setMargins(4, 4, 4, 4)
                    }
                    setOnClickListener { onCellClick(r, c) }
                }
            }
        }
        cells.forEach { row -> row.forEach { grid.addView(it) } }
        root.addView(grid)

        val btnNew = Button(this).apply {
            text = "NEW ROUND"
            setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFF00E5FF.toInt())
            setOnClickListener { resetBoard() }
        }
        root.addView(btnNew)
        setContentView(root)
    }

    private fun onCellClick(r: Int, c: Int) {
        if (gameOver || board[r][c] != 0) return
        board[r][c] = current
        cells[r][c].text = if (current == 1) "X" else "O"
        cells[r][c].setTextColor(if (current == 1) 0xFF00E5FF.toInt() else 0xFFFF4081.toInt())
        checkWinner()
        current = if (current == 1) 2 else 1
        if (!gameOver) tvStatus.text = "Player ${if (current == 1) "X" else "O"}'s Turn"
    }

    private fun checkWinner() {
        val lines = listOf(
            listOf(0 to 0, 0 to 1, 0 to 2), listOf(1 to 0, 1 to 1, 1 to 2),
            listOf(2 to 0, 2 to 1, 2 to 2), listOf(0 to 0, 1 to 0, 2 to 0),
            listOf(0 to 1, 1 to 1, 2 to 1), listOf(0 to 2, 1 to 2, 2 to 2),
            listOf(0 to 0, 1 to 1, 2 to 2), listOf(0 to 2, 1 to 1, 2 to 0)
        )
        for (line in lines) {
            val vals = line.map { board[it.first][it.second] }
            if (vals.all { it == 1 }) { endGame("X Wins! 🏆"); scores[0]++; updateScore(); return }
            if (vals.all { it == 2 }) { endGame("O Wins! 🏆"); scores[1]++; updateScore(); return }
        }
        if (board.all { row -> row.all { it != 0 } }) {
            endGame("Draw! 🤝"); scores[2]++; updateScore()
        }
    }

    private fun endGame(msg: String) {
        gameOver = true
        tvStatus.text = msg
    }

    private fun updateScore() {
        tvScore.text = "X: ${scores[0]}   DRAW: ${scores[2]}   O: ${scores[1]}"
    }

    private fun resetBoard() {
        board.forEach { row -> row.fill(0) }
        cells.forEach { row -> row.forEach { it.text = "" } }
        current = 1; gameOver = false
        tvStatus.text = "Player X's Turn"
    }
}
