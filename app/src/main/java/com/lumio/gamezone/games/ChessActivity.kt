package com.lumio.gamezone.games

import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.*
import com.lumio.gamezone.ui.BaseGameActivity

class ChessActivity : BaseGameActivity() {

    data class Piece(val type: Char, val white: Boolean)

    private val board = Array(8) { arrayOfNulls<Piece>(8) }
    private var selectedR = -1; private var selectedC = -1
    private var validMoves = mutableListOf<Pair<Int,Int>>()
    private var whiteTurn = true
    private lateinit var cells: Array<Array<TextView>>
    private lateinit var tvStatus: TextView

    private val ICONS = mapOf(
        'K' to Pair("♔","♚"), 'Q' to Pair("♕","♛"), 'R' to Pair("♖","♜"),
        'B' to Pair("♗","♝"), 'N' to Pair("♘","♞"), 'P' to Pair("♙","♟")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "CHESS"
        setupBoard()

        val scroll = ScrollView(this).apply { setBackgroundColor(0xFF080614.toInt()) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF080614.toInt())
            setPadding(8, 16, 8, 16); gravity = Gravity.CENTER
        }

        tvStatus = TextView(this).apply {
            text = "WHITE's Turn"
            textSize = 14f; setTextColor(0xFFFFD700.toInt())
            gravity = Gravity.CENTER; setPadding(0, 0, 0, 12)
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(tvStatus)

        val grid = GridLayout(this).apply { rowCount = 8; columnCount = 8 }
        val sq = (resources.displayMetrics.widthPixels - 32) / 8
        cells = Array(8) { r ->
            Array(8) { c ->
                TextView(this).apply {
                    textSize = (sq * 0.5f / resources.displayMetrics.scaledDensity)
                    gravity = Gravity.CENTER
                    layoutParams = GridLayout.LayoutParams().apply { width = sq; height = sq }
                    val light = (r + c) % 2 == 0
                    setBackgroundColor(if (light) 0xFF1e2a5e.toInt() else 0xFF0f1435.toInt())
                    setOnClickListener { onSquareClick(r, c) }
                }
            }
        }
        cells.forEach { row -> row.forEach { grid.addView(it) } }
        root.addView(grid)

        val btnNew = Button(this).apply {
            text = "NEW GAME"; setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFF00E5FF.toInt())
            setOnClickListener { setupBoard(); renderBoard(); tvStatus.text = "WHITE's Turn"; whiteTurn = true }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 16 }
        }
        root.addView(btnNew)
        scroll.addView(root)
        setContentView(scroll)
        renderBoard()
    }

    private fun setupBoard() {
        for (r in 0..7) for (c in 0..7) board[r][c] = null
        val order = listOf('R','N','B','Q','K','B','N','R')
        order.forEachIndexed { c, t -> board[0][c] = Piece(t, false); board[7][c] = Piece(t, true) }
        for (c in 0..7) { board[1][c] = Piece('P', false); board[6][c] = Piece('P', true) }
    }

    private fun renderBoard() {
        for (r in 0..7) for (c in 0..7) {
            val p = board[r][c]
            val isSelected = r == selectedR && c == selectedC
            val isValid = validMoves.contains(Pair(r, c))
            val light = (r + c) % 2 == 0
            cells[r][c].apply {
                text = if (p != null) (if (p.white) ICONS[p.type]?.first else ICONS[p.type]?.second) ?: "" else ""
                setTextColor(if (p?.white == true) 0xFFFFFFFF.toInt() else 0xFFCCCCCC.toInt())
                setBackgroundColor(when {
                    isSelected -> 0x99FFD700.toInt()
                    isValid && board[r][c] != null -> 0x99FF4081.toInt()
                    isValid -> 0x5500E5FF.toInt()
                    light -> 0xFF1e2a5e.toInt()
                    else -> 0xFF0f1435.toInt()
                })
            }
        }
    }

    private fun onSquareClick(r: Int, c: Int) {
        val p = board[r][c]
        if (selectedR >= 0) {
            if (validMoves.contains(Pair(r, c))) {
                // Promote pawn
                var movePiece = board[selectedR][selectedC]!!
                if (movePiece.type == 'P' && (r == 0 || r == 7)) movePiece = Piece('Q', movePiece.white)
                board[r][c] = movePiece
                board[selectedR][selectedC] = null
                whiteTurn = !whiteTurn
                tvStatus.text = "${if (whiteTurn) "WHITE" else "BLACK"}'s Turn"
                selectedR = -1; selectedC = -1; validMoves.clear()
                renderBoard(); return
            }
            selectedR = -1; selectedC = -1; validMoves.clear()
        }
        if (p != null && p.white == whiteTurn) {
            selectedR = r; selectedC = c
            validMoves = getMoves(r, c)
        }
        renderBoard()
    }

    private fun getMoves(r: Int, c: Int): MutableList<Pair<Int,Int>> {
        val piece = board[r][c] ?: return mutableListOf()
        val moves = mutableListOf<Pair<Int,Int>>()
        val opp = !piece.white

        fun add(nr: Int, nc: Int): Boolean {
            if (nr !in 0..7 || nc !in 0..7) return false
            if (board[nr][nc]?.white == piece.white) return false
            moves.add(Pair(nr, nc))
            return board[nr][nc] == null
        }
        fun slide(dr: Int, dc: Int) { var nr=r+dr; var nc=c+dc; while(add(nr,nc)){nr+=dr;nc+=dc} }

        when (piece.type) {
            'P' -> {
                val d = if (piece.white) -1 else 1
                val start = if (piece.white) 6 else 1
                if (r+d in 0..7 && board[r+d][c] == null) {
                    moves.add(Pair(r+d, c))
                    if (r == start && board[r+2*d][c] == null) moves.add(Pair(r+2*d, c))
                }
                for (dc in listOf(-1, 1)) if (c+dc in 0..7 && board[r+d][c+dc]?.white == opp) moves.add(Pair(r+d, c+dc))
            }
            'N' -> listOf(-2 to -1,-2 to 1,-1 to -2,-1 to 2,1 to -2,1 to 2,2 to -1,2 to 1).forEach { (dr,dc) -> add(r+dr, c+dc) }
            'B' -> listOf(-1 to -1,-1 to 1,1 to -1,1 to 1).forEach { (dr,dc) -> slide(dr,dc) }
            'R' -> listOf(-1 to 0,1 to 0,0 to -1,0 to 1).forEach { (dr,dc) -> slide(dr,dc) }
            'Q' -> listOf(-1 to -1,-1 to 0,-1 to 1,0 to -1,0 to 1,1 to -1,1 to 0,1 to 1).forEach { (dr,dc) -> slide(dr,dc) }
            'K' -> listOf(-1 to -1,-1 to 0,-1 to 1,0 to -1,0 to 1,1 to -1,1 to 0,1 to 1).forEach { (dr,dc) -> add(r+dr, c+dc) }
        }
        return moves
    }
}
