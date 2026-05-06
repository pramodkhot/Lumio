package com.lumio.gamezone.games

import android.animation.*
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.*
import android.widget.*
import com.lumio.gamezone.ui.BaseGameActivity

class TicTacToeActivity : BaseGameActivity() {

    // ── Game state ────────────────────────────────────────────────────────────
    private val board = Array(3) { IntArray(3) { 0 } } // 0=empty 1=X 2=O
    private var currentPlayer = 1   // 1=X 2=O
    private var gameOver = false
    private var moveCount = 0
    private val scores = intArrayOf(0, 0, 0) // X wins, O wins, draws
    private var winLine: List<Pair<Int,Int>>? = null

    // Mode: 0=PVP  1=vs Easy AI  2=vs Hard AI
    private var gameMode = 0
    private var playerNames = arrayOf("Player X", "Player O")

    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var boardView: TicTacToeBoardView
    private lateinit var tvP1Name: TextView
    private lateinit var tvP2Name: TextView
    private lateinit var tvP1Score: TextView
    private lateinit var tvP2Score: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvRuleHint: TextView
    private lateinit var overlayLayout: FrameLayout
    private lateinit var tvOverlayTitle: TextView
    private lateinit var tvOverlaySubtitle: TextView
    private lateinit var btnOverlayAction: Button
    private lateinit var modeContainer: LinearLayout

    // ── Board custom view ─────────────────────────────────────────────────────
    inner class TicTacToeBoardView(context: android.content.Context) : View(context) {

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = 0xFF1A2A6E.toInt()
        }
        private val xPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = 0xFF00E5FF.toInt()
        }
        private val oPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = 0xFFFF4081.toInt()
        }
        private val winPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = 0xFFFFD700.toInt()
        }
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = 0x55FFD700.toInt()
        }
        private val cellHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        private var cellSize = 0f
        private var boardLeft = 0f
        private var boardTop = 0f
        var drawProgress = 1f // 0..1 for draw animation
        var winProgress = 1f  // 0..1 for win line animation

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            val size = minOf(w, h).toFloat()
            cellSize = size / 3f
            boardLeft = (w - size) / 2f
            boardTop = (h - size) / 2f

            linePaint.strokeWidth = size * 0.012f
            xPaint.strokeWidth = size * 0.055f
            oPaint.strokeWidth = size * 0.055f
            winPaint.strokeWidth = size * 0.06f
            glowPaint.strokeWidth = size * 0.1f
        }

        override fun onDraw(canvas: Canvas) {
            val size = cellSize * 3

            // Background per cell
            for (r in 0..2) for (c in 0..2) {
                val l = boardLeft + c * cellSize
                val t = boardTop + r * cellSize
                val isWinCell = winLine?.contains(Pair(r, c)) == true

                cellHighlightPaint.color = when {
                    isWinCell -> 0xFF1A2E10.toInt()
                    board[r][c] == 1 -> 0xFF001525.toInt()
                    board[r][c] == 2 -> 0xFF250015.toInt()
                    else -> 0xFF0F1435.toInt()
                }
                val cellRect = RectF(l + 6f, t + 6f, l + cellSize - 6f, t + cellSize - 6f)
                canvas.drawRoundRect(cellRect, 16f, 16f, cellHighlightPaint)
            }

            // Grid lines
            for (i in 1..2) {
                // Vertical
                canvas.drawLine(boardLeft + i*cellSize, boardTop + 20f,
                    boardLeft + i*cellSize, boardTop + size - 20f, linePaint)
                // Horizontal
                canvas.drawLine(boardLeft + 20f, boardTop + i*cellSize,
                    boardLeft + size - 20f, boardTop + i*cellSize, linePaint)
            }

            // Pieces
            for (r in 0..2) for (c in 0..2) {
                if (board[r][c] == 0) continue
                val cx = boardLeft + c * cellSize + cellSize / 2f
                val cy = boardTop + r * cellSize + cellSize / 2f
                val pad = cellSize * 0.22f

                if (board[r][c] == 1) {
                    // X
                    val paint = if (winLine?.contains(Pair(r, c)) == true) winPaint else xPaint
                    canvas.drawLine(cx - pad, cy - pad, cx + pad, cy + pad, paint)
                    canvas.drawLine(cx + pad, cy - pad, cx - pad, cy + pad, paint)
                } else {
                    // O
                    val paint = if (winLine?.contains(Pair(r, c)) == true) winPaint else oPaint
                    canvas.drawCircle(cx, cy, pad, paint)
                }
            }

            // Win line
            if (winLine != null && winProgress < 1f) {
                drawWinLine(canvas, winLine!!, winProgress)
            } else if (winLine != null) {
                drawWinLine(canvas, winLine!!, 1f)
            }
        }

        private fun drawWinLine(canvas: Canvas, line: List<Pair<Int,Int>>, progress: Float) {
            val start = line.first()
            val end = line.last()
            val sx = boardLeft + start.second * cellSize + cellSize / 2f
            val sy = boardTop + start.first * cellSize + cellSize / 2f
            val ex = boardLeft + end.second * cellSize + cellSize / 2f
            val ey = boardTop + end.first * cellSize + cellSize / 2f
            val mx = sx + (ex - sx) * progress
            val my = sy + (ey - sy) * progress

            // Glow
            canvas.drawLine(sx, sy, mx, my, glowPaint)
            // Line
            canvas.drawLine(sx, sy, mx, my, winPaint)
        }

        fun animatePiece(r: Int, c: Int) {
            val anim = ScaleAnimation(
                0.3f, 1.05f, 0.3f, 1.05f,
                Animation.ABSOLUTE, boardLeft + c * cellSize + cellSize / 2f,
                Animation.ABSOLUTE, boardTop + r * cellSize + cellSize / 2f
            ).apply {
                duration = 180
                interpolator = OvershootInterpolator(2f)
                fillAfter = true
            }
            startAnimation(anim)
            invalidate()
        }

        fun animateWinLine() {
            winProgress = 0f
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 500
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    winProgress = it.animatedValue as Float
                    invalidate()
                }
            }
            anim.start()
        }

        fun getCellAt(x: Float, y: Float): Pair<Int,Int>? {
            if (x < boardLeft || y < boardTop) return null
            val c = ((x - boardLeft) / cellSize).toInt()
            val r = ((y - boardTop) / cellSize).toInt()
            if (r in 0..2 && c in 0..2) return Pair(r, c)
            return null
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val w = MeasureSpec.getSize(widthMeasureSpec)
            setMeasuredDimension(w, w) // Square
        }
    }

    // ── Activity lifecycle ────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "TIC TAC TOE"
        setContentView(buildUI())
        resetBoard()
    }

    private fun buildUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF080614.toInt())
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // ── Header ──────────────────────────────────────────────────────────
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0D0B20.toInt())
            setPadding(0, 16, 0, 16)
        }

        // Score row
        val scoreRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // P1 card
        val p1Card = buildPlayerCard(isX = true).also { (card, name, score) ->
            tvP1Name = name; tvP1Score = score
            scoreRow.addView(card)
        }

        // VS divider
        scoreRow.addView(TextView(this).apply {
            text = "VS"
            textSize = 14f; setTextColor(0xFF8892B0.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        // P2 card
        buildPlayerCard(isX = false).also { (card, name, score) ->
            tvP2Name = name; tvP2Score = score
            scoreRow.addView(card)
        }

        header.addView(scoreRow)

        // Status
        tvStatus = TextView(this).apply {
            textSize = 16f; gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(16, 12, 16, 0)
        }
        header.addView(tvStatus)
        root.addView(header)

        // ── Mode selector ────────────────────────────────────────────────────
        modeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(16, 12, 16, 12)
            setBackgroundColor(0xFF080614.toInt())
        }

        val modes = listOf("👥 2 Players", "🤖 Easy AI", "🧠 Hard AI")
        modes.forEachIndexed { i, label ->
            val btn = TextView(this).apply {
                text = label; textSize = 12f
                gravity = Gravity.CENTER
                setPadding(20, 10, 20, 10)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(4, 0, 4, 0)
                }
                setOnClickListener { setMode(i) }
                tag = i
            }
            modeContainer.addView(btn)
        }
        root.addView(modeContainer)

        // ── Board ────────────────────────────────────────────────────────────
        boardView = TicTacToeBoardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(16, 8, 16, 8) }
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP && !gameOver) {
                    val cell = getCellAt(event.x, event.y)
                    if (cell != null) onCellTap(cell.first, cell.second)
                }
                true
            }
        }
        root.addView(boardView)

        // ── Rule hint ────────────────────────────────────────────────────────
        tvRuleHint = TextView(this).apply {
            textSize = 13f; gravity = Gravity.CENTER
            setTextColor(0xFF8892B0.toInt())
            setPadding(24, 8, 24, 8)
            setLineSpacing(0f, 1.4f)
        }
        root.addView(tvRuleHint)

        // ── Bottom buttons ───────────────────────────────────────────────────
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(16, 8, 16, 24)
        }
        val btnNew = buildButton("🔄 NEW ROUND", 0xFF00E5FF.toInt()) { resetBoard() }
        val btnReset = buildButton("🗑 RESET SCORES", 0xFF1A2A6E.toInt()) { resetScores() }
        btnNew.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 8 }
        btnReset.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 8 }
        btnRow.addView(btnNew)
        btnRow.addView(btnReset)
        root.addView(btnRow)

        // ── Game over overlay (inside FrameLayout wrapper) ───────────────────
        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        root.addView(frame) // occupies remaining space

        // Overlay
        overlayLayout = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(0xEE080614.toInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val overlayContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        tvOverlayTitle = TextView(this).apply {
            textSize = 64f; gravity = Gravity.CENTER
        }
        overlayContent.addView(tvOverlayTitle)

        tvOverlaySubtitle = TextView(this).apply {
            textSize = 24f; gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 8, 0, 32)
        }
        overlayContent.addView(tvOverlaySubtitle)

        btnOverlayAction = buildButton("PLAY AGAIN", 0xFF00E5FF.toInt()) {
            hideOverlay()
            resetBoard()
        }
        btnOverlayAction.textSize = 16f
        overlayContent.addView(btnOverlayAction)

        overlayLayout.addView(overlayContent)
        frame.addView(overlayLayout)

        return root
    }

    private fun buildPlayerCard(isX: Boolean): Triple<View, TextView, TextView> {
        val color = if (isX) 0xFF00E5FF.toInt() else 0xFFFF4081.toInt()
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 12, 24, 12)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }
        val symbol = TextView(this).apply {
            text = if (isX) "✕" else "○"
            textSize = 32f; gravity = Gravity.CENTER
            setTextColor(color); typeface = Typeface.DEFAULT_BOLD
        }
        val name = TextView(this).apply {
            text = if (isX) "Player X" else "Player O"
            textSize = 11f; gravity = Gravity.CENTER
            setTextColor(0xFF8892B0.toInt())
        }
        val score = TextView(this).apply {
            text = "0"
            textSize = 28f; gravity = Gravity.CENTER
            setTextColor(color); typeface = Typeface.DEFAULT_BOLD
        }
        card.addView(symbol); card.addView(name); card.addView(score)
        return Triple(card, name, score)
    }

    private fun buildButton(label: String, bgColor: Int, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label; textSize = 13f
            setTextColor(if (bgColor == 0xFF00E5FF.toInt()) 0xFF000000.toInt() else 0xFF00E5FF.toInt())
            setBackgroundColor(bgColor)
            setPadding(20, 14, 20, 14)
            setOnClickListener { onClick() }
        }
    }

    // ── Mode selection ────────────────────────────────────────────────────────
    private fun setMode(mode: Int) {
        gameMode = mode
        playerNames = when (mode) {
            0 -> arrayOf("Player X", "Player O")
            1 -> arrayOf("You (X)", "Easy AI")
            2 -> arrayOf("You (X)", "Hard AI")
            else -> arrayOf("Player X", "Player O")
        }
        tvP1Name.text = playerNames[0]
        tvP2Name.text = playerNames[1]

        // Highlight selected mode button
        for (i in 0 until modeContainer.childCount) {
            val btn = modeContainer.getChildAt(i) as TextView
            if (btn.tag == mode) {
                btn.setTextColor(0xFF000000.toInt())
                btn.setBackgroundColor(0xFF00E5FF.toInt())
            } else {
                btn.setTextColor(0xFF00E5FF.toInt())
                btn.setBackgroundColor(0xFF0F1435.toInt())
            }
        }

        resetScores()
        resetBoard()
    }

    // ── Game logic ────────────────────────────────────────────────────────────
    private fun onCellTap(r: Int, c: Int) {
        if (gameOver || board[r][c] != 0) return
        // In AI mode, only respond to human taps (player 1 = X = currentPlayer 1)
        if (gameMode > 0 && currentPlayer == 2) return

        makeMove(r, c)
    }

    private fun makeMove(r: Int, c: Int) {
        board[r][c] = currentPlayer
        boardView.animatePiece(r, c)
        moveCount++

        val winner = checkWinner()
        when {
            winner > 0 -> endGame(winner)
            moveCount == 9 -> endGame(0) // draw
            else -> {
                currentPlayer = if (currentPlayer == 1) 2 else 1
                updateTurnUI()
                if (gameMode > 0 && currentPlayer == 2) {
                    Handler(Looper.getMainLooper()).postDelayed({ aiMove() }, 500)
                }
            }
        }
    }

    private fun checkWinner(): Int {
        val WIN_LINES = listOf(
            listOf(0 to 0, 0 to 1, 0 to 2),
            listOf(1 to 0, 1 to 1, 1 to 2),
            listOf(2 to 0, 2 to 1, 2 to 2),
            listOf(0 to 0, 1 to 0, 2 to 0),
            listOf(0 to 1, 1 to 1, 2 to 1),
            listOf(0 to 2, 1 to 2, 2 to 2),
            listOf(0 to 0, 1 to 1, 2 to 2),
            listOf(0 to 2, 1 to 1, 2 to 0)
        )
        for (line in WIN_LINES) {
            val vals = line.map { board[it.first][it.second] }
            if (vals.all { it == 1 }) { winLine = line; boardView.animateWinLine(); return 1 }
            if (vals.all { it == 2 }) { winLine = line; boardView.animateWinLine(); return 2 }
        }
        return 0
    }

    private fun endGame(winner: Int) {
        gameOver = true
        boardView.invalidate()

        when (winner) {
            1 -> {
                scores[0]++
                tvP1Score.text = scores[0].toString()
                updateStatus("${playerNames[0]} Wins! 🏆", 0xFF00E5FF.toInt())
                showOverlay("🎉", "${playerNames[0]} WINS!", 0xFF00E5FF.toInt())
            }
            2 -> {
                scores[1]++
                tvP2Score.text = scores[1].toString()
                updateStatus("${playerNames[1]} Wins! 🏆", 0xFFFF4081.toInt())
                showOverlay("🎊", "${playerNames[1]} WINS!", 0xFFFF4081.toInt())
            }
            else -> {
                scores[2]++
                updateStatus("It's a Draw! 🤝", 0xFFFFD700.toInt())
                showOverlay("🤝", "DRAW — GREAT MATCH!", 0xFFFFD700.toInt())
            }
        }
    }

    // ── AI ────────────────────────────────────────────────────────────────────
    private fun aiMove() {
        if (gameOver) return
        val move = if (gameMode == 2) bestMove() else randomMove()
        move?.let { makeMove(it.first, it.second) }
    }

    private fun randomMove(): Pair<Int,Int>? {
        val empty = mutableListOf<Pair<Int,Int>>()
        for (r in 0..2) for (c in 0..2) if (board[r][c] == 0) empty.add(Pair(r,c))
        return empty.randomOrNull()
    }

    private fun bestMove(): Pair<Int,Int>? {
        var best = Int.MIN_VALUE
        var bestMove: Pair<Int,Int>? = null
        for (r in 0..2) for (c in 0..2) {
            if (board[r][c] == 0) {
                board[r][c] = 2
                val score = minimax(false, 0)
                board[r][c] = 0
                if (score > best) { best = score; bestMove = Pair(r,c) }
            }
        }
        return bestMove
    }

    private fun minimax(isMax: Boolean, depth: Int): Int {
        val w = checkWinnerForMinimax()
        if (w == 2) return 10 - depth
        if (w == 1) return depth - 10
        if ((0..2).all { r -> (0..2).all { c -> board[r][c] != 0 } }) return 0

        if (isMax) {
            var best = Int.MIN_VALUE
            for (r in 0..2) for (c in 0..2) if (board[r][c] == 0) {
                board[r][c] = 2
                best = maxOf(best, minimax(false, depth + 1))
                board[r][c] = 0
            }
            return best
        } else {
            var best = Int.MAX_VALUE
            for (r in 0..2) for (c in 0..2) if (board[r][c] == 0) {
                board[r][c] = 1
                best = minOf(best, minimax(true, depth + 1))
                board[r][c] = 0
            }
            return best
        }
    }

    private fun checkWinnerForMinimax(): Int {
        val lines = listOf(
            listOf(0,1,2).map { Pair(0,it) }, listOf(0,1,2).map { Pair(1,it) }, listOf(0,1,2).map { Pair(2,it) },
            listOf(0,1,2).map { Pair(it,0) }, listOf(0,1,2).map { Pair(it,1) }, listOf(0,1,2).map { Pair(it,2) },
            listOf(0,1,2).map { Pair(it,it) }, listOf(0,1,2).map { Pair(it,2-it) }
        )
        for (line in lines) {
            val v = line.map { board[it.first][it.second] }
            if (v.all { it == 1 }) return 1
            if (v.all { it == 2 }) return 2
        }
        return 0
    }

    // ── UI helpers ────────────────────────────────────────────────────────────
    private fun updateTurnUI() {
        val name = if (currentPlayer == 1) playerNames[0] else playerNames[1]
        val color = if (currentPlayer == 1) 0xFF00E5FF.toInt() else 0xFFFF4081.toInt()
        val symbol = if (currentPlayer == 1) "✕" else "○"
        updateStatus("$symbol  $name's Turn", color)
        updateRuleHint()
    }

    private fun updateStatus(msg: String, color: Int) {
        tvStatus.text = msg
        tvStatus.setTextColor(color)
    }

    private fun updateRuleHint() {
        tvRuleHint.text = when (gameMode) {
            0 -> "Pass & Play — Get 3 in a row to win!\nRows  •  Columns  •  Diagonals"
            1 -> "You are X — Easy AI makes random moves\nGet 3 in a row: →  ↓  ↘  ↗"
            2 -> "You are X — Hard AI uses Minimax strategy\nThink ahead! Block diagonals early"
            else -> ""
        }
    }

    private fun showOverlay(emoji: String, message: String, color: Int) {
        tvOverlayTitle.text = emoji
        tvOverlaySubtitle.text = message
        tvOverlaySubtitle.setTextColor(color)
        overlayLayout.setBackgroundColor(color and 0x00FFFFFF or 0xEE000000.toInt())
        overlayLayout.visibility = View.VISIBLE
        overlayLayout.alpha = 0f
        overlayLayout.animate().alpha(1f).setDuration(400).start()

        // Pulse animation on emoji
        val pulse = ObjectAnimator.ofFloat(tvOverlayTitle, "scaleX", 1f, 1.3f, 1f).apply {
            duration = 600; repeatCount = 2
        }
        val pulse2 = ObjectAnimator.ofFloat(tvOverlayTitle, "scaleY", 1f, 1.3f, 1f).apply {
            duration = 600; repeatCount = 2
        }
        AnimatorSet().apply { playTogether(pulse, pulse2); start() }
    }

    private fun hideOverlay() {
        overlayLayout.animate().alpha(0f).setDuration(300).withEndAction {
            overlayLayout.visibility = View.GONE
        }.start()
    }

    private fun resetBoard() {
        for (r in 0..2) board[r].fill(0)
        currentPlayer = 1; gameOver = false; moveCount = 0; winLine = null
        boardView.winProgress = 1f
        boardView.invalidate()
        hideOverlay()
        updateTurnUI()
    }

    private fun resetScores() {
        scores.fill(0)
        tvP1Score.text = "0"
        tvP2Score.text = "0"
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    override fun onResume() {
        super.onResume()
        setMode(0) // default to PvP
    }
}
