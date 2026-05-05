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
    private var gameOver = false
    private lateinit var cells: Array<Array<FrameLayout>>
    private lateinit var tvStatus: TextView
    private lateinit var tvCaptures: TextView
    private val capturedWhite = mutableListOf<Char>()
    private val capturedBlack = mutableListOf<Char>()

    private val ICONS = mapOf(
        'K' to Pair("♔","♚"), 'Q' to Pair("♕","♛"), 'R' to Pair("♖","♜"),
        'B' to Pair("♗","♝"), 'N' to Pair("♘","♞"), 'P' to Pair("♙","♟")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Full screen
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF080614.toInt())
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF0D0B20.toInt())
            setPadding(16, 40, 16, 12)
        }

        tvStatus = TextView(this).apply {
            text = "♟ CHESS — WHITE's Turn"
            textSize = 18f; setTextColor(0xFFFFD700.toInt())
            gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
        }
        header.addView(tvStatus)

        tvCaptures = TextView(this).apply {
            text = ""; textSize = 13f; setTextColor(0xFF8892B0.toInt())
            gravity = Gravity.CENTER; setPadding(0, 6, 0, 0)
        }
        header.addView(tvCaptures)

        val btnNew = Button(this).apply {
            text = "NEW GAME"; textSize = 11f
            setTextColor(0xFF000000.toInt()); setBackgroundColor(0xFF00E5FF.toInt())
            setPadding(24, 8, 24, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8; gravity = Gravity.CENTER_HORIZONTAL }
            setOnClickListener { resetGame() }
        }
        header.addView(btnNew)
        root.addView(header)

        // Column labels
        val colLabels = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF0D0B20.toInt())
            setPadding(32, 4, 16, 4)
        }
        colLabels.addView(Space(this).apply { layoutParams = LinearLayout.LayoutParams(32, 1) })
        listOf("a","b","c","d","e","f","g","h").forEach { l ->
            colLabels.addView(TextView(this).apply {
                text = l; textSize = 10f; setTextColor(0xFF8892B0.toInt())
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        root.addView(colLabels)

        // Board with row labels
        val boardRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        val rowLabelsLeft = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0D0B20.toInt())
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(32, LinearLayout.LayoutParams.MATCH_PARENT)
        }
        (8 downTo 1).forEach { n ->
            rowLabelsLeft.addView(TextView(this).apply {
                text = "$n"; textSize = 10f; setTextColor(0xFF8892B0.toInt())
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(32, 0, 1f)
            })
        }
        boardRow.addView(rowLabelsLeft)

        val grid = GridLayout(this).apply {
            rowCount = 8; columnCount = 8
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }

        val sq = (resources.displayMetrics.widthPixels - 32) / 8
        cells = Array(8) { r ->
            Array(8) { c ->
                FrameLayout(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply { width = sq; height = sq }
                    val light = (r + c) % 2 == 0
                    setBackgroundColor(if (light) 0xFF1e2a5e.toInt() else 0xFF0f1435.toInt())
                    setOnClickListener { onSquareClick(r, c) }
                    grid.addView(this)
                }
            }
        }
        boardRow.addView(grid)
        root.addView(boardRow)

        setupBoard()
        setContentView(root)
        renderBoard()
    }

    private fun setupBoard() {
        for (r in 0..7) for (c in 0..7) board[r][c] = null
        capturedWhite.clear(); capturedBlack.clear()
        val order = listOf('R','N','B','Q','K','B','N','R')
        order.forEachIndexed { c, t ->
            board[0][c] = Piece(t, false)
            board[7][c] = Piece(t, true)
        }
        for (c in 0..7) {
            board[1][c] = Piece('P', false)
            board[6][c] = Piece('P', true)
        }
        whiteTurn = true; gameOver = false
        selectedR = -1; selectedC = -1; validMoves.clear()
    }

    private fun renderBoard() {
        val sq = (resources.displayMetrics.widthPixels - 32) / 8
        for (r in 0..7) for (c in 0..7) {
            val p = board[r][c]
            val isSelected = r == selectedR && c == selectedC
            val isValid = validMoves.contains(Pair(r, c))
            val isCapture = isValid && board[r][c] != null
            val light = (r + c) % 2 == 0

            val cell = cells[r][c]
            cell.removeAllViews()

            // Background
            cell.setBackgroundColor(when {
                isSelected -> 0xAAFFD700.toInt()
                isCapture  -> 0xAAFF4081.toInt()
                isValid    -> 0x5500E5FF.toInt()
                light      -> 0xFF1e2a5e.toInt()
                else       -> 0xFF0f1435.toInt()
            })

            // Valid move dot
            if (isValid && !isCapture) {
                val dot = View(this).apply {
                    val dotSize = sq / 3
                    layoutParams = FrameLayout.LayoutParams(dotSize, dotSize, Gravity.CENTER)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(0x8800E5FF.toInt())
                    }
                }
                cell.addView(dot)
            }

            // Piece
            if (p != null) {
                val icon = if (p.white) ICONS[p.type]?.first else ICONS[p.type]?.second
                val tv = TextView(this).apply {
                    text = icon ?: ""
                    textSize = (sq * 0.48f / resources.displayMetrics.scaledDensity)
                    gravity = Gravity.CENTER
                    setTextColor(if (p.white) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt())
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    // Red overlay if king in check
                    if (p.type == 'K' && p.white == whiteTurn && isInCheck(p.white)) {
                        setBackgroundColor(0x55FF0000)
                    }
                }
                cell.addView(tv)
            }

            // Capture ring
            if (isCapture) {
                val ring = View(this).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setStroke(4, 0xFFFF4081.toInt())
                        setColor(android.graphics.Color.TRANSPARENT)
                    }
                }
                cell.addView(ring)
            }
        }

        // Update captures display
        val wCap = capturedBlack.joinToString(" ") { ICONS[it]?.first ?: "" }
        val bCap = capturedWhite.joinToString(" ") { ICONS[it]?.second ?: "" }
        tvCaptures.text = if (wCap.isNotEmpty() || bCap.isNotEmpty())
            "White took: $bCap   Black took: $wCap" else ""
    }

    private fun onSquareClick(r: Int, c: Int) {
        if (gameOver) return
        val p = board[r][c]

        if (selectedR >= 0) {
            if (validMoves.contains(Pair(r, c))) {
                applyMove(r, c)
                return
            }
            selectedR = -1; selectedC = -1; validMoves.clear()
        }

        if (p != null && p.white == whiteTurn) {
            selectedR = r; selectedC = c
            validMoves = getMoves(r, c)
        }
        renderBoard()
    }

    private fun applyMove(toR: Int, toC: Int) {
        val piece = board[selectedR][selectedC]!!
        // Capture
        board[toR][toC]?.let {
            if (it.white) capturedWhite.add(it.type)
            else capturedBlack.add(it.type)
            // King captured = game over
            if (it.type == 'K') {
                board[toR][toC] = if (piece.type == 'P' && (toR == 0 || toR == 7)) Piece('Q', piece.white) else piece
                board[selectedR][selectedC] = null
                selectedR = -1; selectedC = -1; validMoves.clear()
                gameOver = true
                val winner = if (whiteTurn) "WHITE" else "BLACK"
                tvStatus.text = "👑 $winner WINS! ☠️"
                tvStatus.setTextColor(0xFFFF4081.toInt())
                renderBoard()
                return
            }
        }
        // Pawn promotion
        var movePiece = piece
        if (piece.type == 'P' && (toR == 0 || toR == 7)) movePiece = Piece('Q', piece.white)

        board[toR][toC] = movePiece
        board[selectedR][selectedC] = null
        whiteTurn = !whiteTurn
        selectedR = -1; selectedC = -1; validMoves.clear()

        val inCheck = isInCheck(whiteTurn)
        val turn = if (whiteTurn) "WHITE" else "BLACK"
        tvStatus.text = if (inCheck) "⚠️ $turn IN CHECK!" else "♟ $turn's Turn"
        tvStatus.setTextColor(if (inCheck) 0xFFFF5252.toInt() else 0xFFFFD700.toInt())
        renderBoard()
    }

    private fun isInCheck(white: Boolean): Boolean {
        val kingPos = findKing(white) ?: return false
        for (r in 0..7) for (c in 0..7) {
            val p = board[r][c] ?: continue
            if (p.white == white) continue
            if (getMoves(r, c).contains(kingPos)) return true
        }
        return false
    }

    private fun findKing(white: Boolean): Pair<Int,Int>? {
        for (r in 0..7) for (c in 0..7)
            if (board[r][c]?.type == 'K' && board[r][c]?.white == white) return Pair(r, c)
        return null
    }

    private fun resetGame() {
        setupBoard()
        tvStatus.text = "♟ CHESS — WHITE's Turn"
        tvStatus.setTextColor(0xFFFFD700.toInt())
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
                val startRow = if (piece.white) 6 else 1
                if (r+d in 0..7 && board[r+d][c] == null) {
                    moves.add(Pair(r+d, c))
                    if (r == startRow && board[r+2*d][c] == null) moves.add(Pair(r+2*d, c))
                }
                for (dc in listOf(-1,1))
                    if (c+dc in 0..7 && board[r+d][c+dc]?.white == opp) moves.add(Pair(r+d, c+dc))
            }
            'N' -> listOf(-2 to -1,-2 to 1,-1 to -2,-1 to 2,1 to -2,1 to 2,2 to -1,2 to 1)
                .forEach { (dr,dc) -> add(r+dr,c+dc) }
            'B' -> listOf(-1 to -1,-1 to 1,1 to -1,1 to 1).forEach { (dr,dc) -> slide(dr,dc) }
            'R' -> listOf(-1 to 0,1 to 0,0 to -1,0 to 1).forEach { (dr,dc) -> slide(dr,dc) }
            'Q' -> listOf(-1 to -1,-1 to 0,-1 to 1,0 to -1,0 to 1,1 to -1,1 to 0,1 to 1)
                .forEach { (dr,dc) -> slide(dr,dc) }
            'K' -> listOf(-1 to -1,-1 to 0,-1 to 1,0 to -1,0 to 1,1 to -1,1 to 0,1 to 1)
                .forEach { (dr,dc) -> add(r+dr,c+dc) }
        }
        return moves
    }
}
