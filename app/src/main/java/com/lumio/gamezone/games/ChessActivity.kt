package com.lumio.gamezone.games

import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.*
import com.lumio.gamezone.ads.AdManager
import com.lumio.gamezone.ui.BaseGameActivity

class ChessActivity : BaseGameActivity() {

    data class Piece(val type: Char, val white: Boolean)

    // WHITE pieces = YELLOW (0xFFFFD700)
    // BLACK pieces = RED    (0xFFFF5252)
    private val WHITE_COLOR = 0xFFFFD700.toInt()  // Yellow for white side
    private val BLACK_COLOR = 0xFFFF5252.toInt()  // Red for black side
    private val WHITE_DIM   = 0xFFAA9200.toInt()  // Dimmed yellow (not active)
    private val BLACK_DIM   = 0xFFAA3030.toInt()  // Dimmed red (not active)

    private val board = Array(8) { arrayOfNulls<Piece>(8) }
    private var selectedR = -1; private var selectedC = -1
    private var validMoves = mutableListOf<Pair<Int,Int>>()
    private var whiteTurn = true
    private var gameOver = false
    private lateinit var cells: Array<Array<FrameLayout>>
    private lateinit var tvStatus: TextView
    private lateinit var tvWhiteInfo: TextView   // White side header (Yellow)
    private lateinit var tvBlackInfo: TextView   // Black side header (Red)
    private lateinit var tvWhiteCaptures: TextView
    private lateinit var tvBlackCaptures: TextView
    private lateinit var whitePanel: LinearLayout
    private lateinit var blackPanel: LinearLayout
    private val capturedByWhite = mutableListOf<Char>() // pieces white captured
    private val capturedByBlack = mutableListOf<Char>() // pieces black captured

    // Unicode pieces: WHITE shown in yellow, BLACK shown in red
    private val ICONS = mapOf(
        'K' to Pair("♔","♚"), 'Q' to Pair("♕","♛"), 'R' to Pair("♖","♜"),
        'B' to Pair("♗","♝"), 'N' to Pair("♘","♞"), 'P' to Pair("♙","♟")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF080614.toInt())
        }

        // ── BLACK panel (top — opponent) ─────────────────────────────────────
        blackPanel = buildSidePanel(isWhite = false)
        root.addView(blackPanel)

        // ── Status bar ───────────────────────────────────────────────────────
        tvStatus = TextView(this).apply {
            textSize = 15f; gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(16, 8, 16, 8)
            setBackgroundColor(0xFF050410.toInt())
        }
        root.addView(tvStatus)

        // ── Chess board ──────────────────────────────────────────────────────
        val boardWrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        // Row labels left
        val rowLabels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0D0B20.toInt())
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(24, LinearLayout.LayoutParams.MATCH_PARENT)
        }
        (8 downTo 1).forEach { n ->
            rowLabels.addView(TextView(this).apply {
                text = "$n"; textSize = 8f; setTextColor(0xFF8892B0.toInt())
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(24, 0, 1f)
            })
        }
        boardWrap.addView(rowLabels)

        // Board grid
        val grid = GridLayout(this).apply {
            rowCount = 8; columnCount = 8
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }
        val sq = (resources.displayMetrics.widthPixels - 24) / 8
        cells = Array(8) { r ->
            Array(8) { c ->
                FrameLayout(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply { width=sq; height=sq }
                    setBackgroundColor(if ((r+c)%2==0) 0xFF1e2a5e.toInt() else 0xFF0f1435.toInt())
                    setOnClickListener { onSquareClick(r,c) }
                    grid.addView(this)
                }
            }
        }
        boardWrap.addView(grid)
        root.addView(boardWrap)

        // Column labels bottom
        val colLabels = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF0D0B20.toInt())
            setPadding(24, 2, 0, 2)
        }
        listOf("a","b","c","d","e","f","g","h").forEach { l ->
            colLabels.addView(TextView(this).apply {
                text = l; textSize = 8f; setTextColor(0xFF8892B0.toInt())
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(sq, LinearLayout.LayoutParams.WRAP_CONTENT)
            })
        }
        root.addView(colLabels)

        // ── WHITE panel (bottom — current player) ────────────────────────────
        whitePanel = buildSidePanel(isWhite = true)
        root.addView(whitePanel)

        // ── New Game button ──────────────────────────────────────────────────
        root.addView(Button(this).apply {
            text = "♟  NEW GAME"; textSize = 13f
            setTextColor(0xFF000000.toInt()); setBackgroundColor(0xFF00E5FF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(16,8,16,16) }
            setOnClickListener { resetGame() }
        })

        setupBoard()
        setContentView(root)
        renderBoard()
        updatePanels()
    }

    private fun buildSidePanel(isWhite: Boolean): LinearLayout {
        val color = if (isWhite) WHITE_COLOR else BLACK_COLOR
        val label = if (isWhite) "YOU (WHITE ♙)" else "OPPONENT (BLACK ♟)"
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 10, 16, 10)
            setBackgroundColor(0xFF0D0B20.toInt())
        }
        val nameRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val tvName = TextView(this).apply {
            text = label; textSize = 13f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(color)
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvTurn = TextView(this).apply {
            text = "YOUR TURN ▶"; textSize = 11f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(color)
            setPadding(12, 4, 12, 4)
            setBackgroundColor(color and 0x00FFFFFF or 0x22000000)
        }
        nameRow.addView(tvName)
        nameRow.addView(tvTurn)

        val tvCap = TextView(this).apply {
            text = "Captured: —"; textSize = 12f; setTextColor(0xFF8892B0.toInt())
            setPadding(0, 4, 0, 0)
        }
        panel.addView(nameRow); panel.addView(tvCap)

        if (isWhite) {
            tvWhiteInfo = tvName
            tvWhiteCaptures = tvCap
        } else {
            tvBlackInfo = tvName
            tvBlackCaptures = tvCap
        }

        return panel
    }

    private fun updatePanels() {
        // Active side gets full color + "YOUR TURN" badge
        // Inactive side gets dimmed
        val wColor = if (whiteTurn) WHITE_COLOR else WHITE_DIM
        val bColor = if (!whiteTurn) BLACK_COLOR else BLACK_DIM

        whitePanel.setBackgroundColor(
            if (whiteTurn) 0xFF1A1500.toInt() else 0xFF0D0B20.toInt()
        )
        blackPanel.setBackgroundColor(
            if (!whiteTurn) 0xFF1A0500.toInt() else 0xFF0D0B20.toInt()
        )

        tvWhiteInfo.setTextColor(wColor)
        tvBlackInfo.setTextColor(bColor)

        // Update "YOUR TURN" indicator in each panel (child index 0 = nameRow, child 1 = tvCap)
        val whiteNameRow = whitePanel.getChildAt(0) as LinearLayout
        val blackNameRow = blackPanel.getChildAt(0) as LinearLayout
        (whiteNameRow.getChildAt(1) as TextView).apply {
            text = if (whiteTurn) "YOUR TURN ▶" else ""
            setTextColor(WHITE_COLOR)
            setBackgroundColor(if (whiteTurn) 0x33FFD700.toInt() else android.graphics.Color.TRANSPARENT)
        }
        (blackNameRow.getChildAt(1) as TextView).apply {
            text = if (!whiteTurn) "YOUR TURN ▶" else ""
            setTextColor(BLACK_COLOR)
            setBackgroundColor(if (!whiteTurn) 0x33FF5252.toInt() else android.graphics.Color.TRANSPARENT)
        }

        // Captured pieces
        tvWhiteCaptures.text = if (capturedByWhite.isEmpty()) "Captured: —"
        else "Captured: " + capturedByWhite.map { ICONS[it]?.second ?: "" }.joinToString(" ")

        tvBlackCaptures.text = if (capturedByBlack.isEmpty()) "Captured: —"
        else "Captured: " + capturedByBlack.map { ICONS[it]?.first ?: "" }.joinToString(" ")

        // Status bar
        tvStatus.text = if (gameOver) tvStatus.text
        else if (whiteTurn) "♙ WHITE's Turn — Tap a yellow piece"
        else "♟ BLACK's Turn — Tap a red piece"
        tvStatus.setTextColor(if (whiteTurn) WHITE_COLOR else BLACK_COLOR)
    }

    private fun setupBoard() {
        for (r in 0..7) for (c in 0..7) board[r][c] = null
        capturedByWhite.clear(); capturedByBlack.clear()
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
        val sq = (resources.displayMetrics.widthPixels - 24) / 8
        for (r in 0..7) for (c in 0..7) {
            val p = board[r][c]
            val isSel = r==selectedR && c==selectedC
            val isValid = validMoves.contains(r to c)
            val isCapture = isValid && board[r][c] != null
            val light = (r+c)%2==0

            val cell = cells[r][c]
            cell.removeAllViews()
            cell.setBackgroundColor(when {
                isSel     -> 0xAAFFD700.toInt()
                isCapture -> 0xAAFF4081.toInt()
                isValid   -> 0x5500E5FF.toInt()
                light     -> 0xFF1e2a5e.toInt()
                else      -> 0xFF0f1435.toInt()
            })

            // Valid move dot
            if (isValid && !isCapture) {
                cell.addView(View(this).apply {
                    val ds = sq / 3
                    layoutParams = FrameLayout.LayoutParams(ds, ds, Gravity.CENTER)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(0x8800E5FF.toInt())
                    }
                })
            }

            // Piece
            if (p != null) {
                // White pieces = YELLOW, Black pieces = RED
                val activeColor = if (p.white) WHITE_COLOR else BLACK_COLOR
                val dimColor    = if (p.white) WHITE_DIM   else BLACK_DIM
                // Highlight pieces of the active turn
                val pieceColor = if (p.white == whiteTurn) activeColor else dimColor

                // King in check = red background
                if (p.type == 'K' && p.white == whiteTurn && isInCheck(p.white)) {
                    cell.setBackgroundColor(0x88FF0000.toInt())
                }

                cell.addView(TextView(this).apply {
                    text = if (p.white) ICONS[p.type]?.first else ICONS[p.type]?.second
                    textSize = (sq * 0.46f / resources.displayMetrics.scaledDensity)
                    gravity = Gravity.CENTER
                    setTextColor(pieceColor)
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                })

                // Capture ring
                if (isCapture) {
                    cell.addView(View(this).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.OVAL
                            setStroke(4, 0xFFFF4081.toInt())
                            setColor(android.graphics.Color.TRANSPARENT)
                        }
                    })
                }
            }
        }
    }

    private fun onSquareClick(r: Int, c: Int) {
        if (gameOver) return
        val p = board[r][c]

        if (selectedR >= 0) {
            if (validMoves.contains(r to c)) {
                applyMove(r, c); return
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
        board[toR][toC]?.let { cap ->
            if (cap.type == 'K') {
                board[toR][toC] = if (piece.type=='P' && (toR==0||toR==7)) Piece('Q',piece.white) else piece
                board[selectedR][selectedC] = null
                selectedR=-1; selectedC=-1; validMoves.clear()
                gameOver = true
                AdManager.onGameCompleted() // trigger interstitial every 3rd game
                val winner = if (whiteTurn) "WHITE (Yellow)" else "BLACK (Red)"
                tvStatus.text = "👑 $winner WINS!"
                tvStatus.setTextColor(if (whiteTurn) WHITE_COLOR else BLACK_COLOR)
                renderBoard(); updatePanels(); return
            }
            // Track captures per side
            if (whiteTurn) capturedByWhite.add(cap.type)
            else capturedByBlack.add(cap.type)
        }

        var mp = piece
        if (piece.type=='P' && (toR==0||toR==7)) mp = Piece('Q', piece.white)
        board[toR][toC] = mp
        board[selectedR][selectedC] = null
        whiteTurn = !whiteTurn
        selectedR=-1; selectedC=-1; validMoves.clear()

        val inCheck = isInCheck(whiteTurn)
        if (inCheck) {
            tvStatus.text = "⚠️ ${if(whiteTurn) "WHITE" else "BLACK"} IN CHECK!"
            tvStatus.setTextColor(0xFFFF5252.toInt())
        }
        renderBoard(); updatePanels()
    }

    private fun resetGame() {
        setupBoard(); renderBoard(); updatePanels()
        tvStatus.text = "♙ WHITE's Turn — Tap a yellow piece"
        tvStatus.setTextColor(WHITE_COLOR)
    }

    private fun isInCheck(white: Boolean): Boolean {
        val king = findKing(white) ?: return false
        for (r in 0..7) for (c in 0..7) {
            val p = board[r][c] ?: continue
            if (p.white == white) continue
            if (getMoves(r,c).contains(king)) return true
        }
        return false
    }

    private fun findKing(white: Boolean): Pair<Int,Int>? {
        for (r in 0..7) for (c in 0..7)
            if (board[r][c]?.type=='K' && board[r][c]?.white==white) return r to c
        return null
    }

    private fun getMoves(r: Int, c: Int): MutableList<Pair<Int,Int>> {
        val piece = board[r][c] ?: return mutableListOf()
        val moves = mutableListOf<Pair<Int,Int>>()
        val opp = !piece.white

        fun add(nr: Int, nc: Int): Boolean {
            if (nr !in 0..7 || nc !in 0..7) return false
            if (board[nr][nc]?.white == piece.white) return false
            moves.add(nr to nc); return board[nr][nc] == null
        }
        fun slide(dr: Int, dc: Int) { var nr=r+dr; var nc=c+dc; while(add(nr,nc)){nr+=dr;nc+=dc} }

        when (piece.type) {
            'P' -> {
                val d = if (piece.white) -1 else 1; val st = if (piece.white) 6 else 1
                if (r+d in 0..7 && board[r+d][c]==null) {
                    moves.add(r+d to c)
                    if (r==st && board[r+2*d][c]==null) moves.add(r+2*d to c)
                }
                for (dc in listOf(-1,1))
                    if (c+dc in 0..7 && board[r+d][c+dc]?.white==opp) moves.add(r+d to c+dc)
            }
            'N' -> listOf(-2 to -1,-2 to 1,-1 to -2,-1 to 2,1 to -2,1 to 2,2 to -1,2 to 1)
                .forEach{(dr,dc)->add(r+dr,c+dc)}
            'B' -> listOf(-1 to -1,-1 to 1,1 to -1,1 to 1).forEach{(dr,dc)->slide(dr,dc)}
            'R' -> listOf(-1 to 0,1 to 0,0 to -1,0 to 1).forEach{(dr,dc)->slide(dr,dc)}
            'Q' -> listOf(-1 to -1,-1 to 0,-1 to 1,0 to -1,0 to 1,1 to -1,1 to 0,1 to 1)
                .forEach{(dr,dc)->slide(dr,dc)}
            'K' -> listOf(-1 to -1,-1 to 0,-1 to 1,0 to -1,0 to 1,1 to -1,1 to 0,1 to 1)
                .forEach{(dr,dc)->add(r+dr,c+dc)}
        }
        return moves
    }
}
