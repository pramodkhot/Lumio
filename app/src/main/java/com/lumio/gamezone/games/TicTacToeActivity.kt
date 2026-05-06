package com.lumio.gamezone.games

import android.animation.*
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.*
import android.widget.*
import com.lumio.gamezone.ads.AdManager
import com.lumio.gamezone.ui.BaseGameActivity

class TicTacToeActivity : BaseGameActivity() {

    private val board = Array(3) { IntArray(3) }
    private var currentPlayer = 1
    private var gameOver = false
    private var moveCount = 0
    private val scores = intArrayOf(0, 0, 0)
    private var winLine: List<Pair<Int,Int>>? = null
    private var gameMode = 0
    private var playerNames = arrayOf("Player X", "Player O")

    private lateinit var rootFrame: FrameLayout
    private lateinit var boardView: BoardView
    private lateinit var tvP1Name: TextView
    private lateinit var tvP2Name: TextView
    private lateinit var tvP1Score: TextView
    private lateinit var tvP2Score: TextView
    private lateinit var tvP1Card: LinearLayout
    private lateinit var tvP2Card: LinearLayout
    private lateinit var tvStatus: TextView
    private lateinit var tvRuleHint: TextView
    private lateinit var overlayLayout: LinearLayout
    private lateinit var tvOverlayEmoji: TextView
    private lateinit var tvOverlayMsg: TextView
    private lateinit var modeContainer: LinearLayout

    inner class BoardView(ctx: android.content.Context) : View(ctx) {
        private var cs = 0f
        private var ox = 0f
        private var oy = 0f
        var winProgress = 1f

        private val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
            color = 0xFF2A3A8E.toInt()
        }
        private val xP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
            color = 0xFF00E5FF.toInt()
        }
        private val oP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
            color = 0xFFFF4081.toInt()
        }
        private val winLineP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
            color = 0xFFFFD700.toInt()
        }
        private val cellP = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
            val s = minOf(w, h).toFloat()
            cs = s / 3f; ox = (w - s) / 2f; oy = (h - s) / 2f
            gridP.strokeWidth = s * 0.013f
            xP.strokeWidth = s * 0.05f
            oP.strokeWidth = s * 0.05f
            winLineP.strokeWidth = s * 0.032f
        }

        override fun onDraw(canvas: Canvas) {
            for (r in 0..2) for (c in 0..2) {
                val l = ox + c * cs; val t = oy + r * cs
                val isWin = winLine?.contains(r to c) == true
                cellP.color = when {
                    isWin          -> 0xFF162810.toInt()
                    board[r][c]==1 -> 0xFF001828.toInt()
                    board[r][c]==2 -> 0xFF280018.toInt()
                    else           -> 0xFF0F1435.toInt()
                }
                canvas.drawRoundRect(RectF(l+5f,t+5f,l+cs-5f,t+cs-5f), 18f, 18f, cellP)
            }
            val s = cs * 3f
            for (i in 1..2) {
                canvas.drawLine(ox+i*cs, oy+20f, ox+i*cs, oy+s-20f, gridP)
                canvas.drawLine(ox+20f, oy+i*cs, ox+s-20f, oy+i*cs, gridP)
            }
            for (r in 0..2) for (c in 0..2) {
                if (board[r][c] == 0) continue
                val cx = ox + c*cs + cs/2f; val cy = oy + r*cs + cs/2f
                val pad = cs * 0.24f
                val isWin = winLine?.contains(r to c) == true
                if (board[r][c] == 1) {
                    val p = if (isWin) Paint(xP).apply { color = 0xFFFFD700.toInt() } else xP
                    canvas.drawLine(cx-pad,cy-pad,cx+pad,cy+pad, p)
                    canvas.drawLine(cx+pad,cy-pad,cx-pad,cy+pad, p)
                } else {
                    val p = if (isWin) Paint(oP).apply { color = 0xFFFFD700.toInt() } else oP
                    canvas.drawCircle(cx, cy, pad, p)
                }
            }
            winLine?.let { line ->
                val s0 = line.first(); val e0 = line.last()
                val sx = ox + s0.second*cs + cs/2f; val sy = oy + s0.first*cs + cs/2f
                val ex = ox + e0.second*cs + cs/2f; val ey = oy + e0.first*cs + cs/2f
                canvas.drawLine(sx, sy, sx+(ex-sx)*winProgress, sy+(ey-sy)*winProgress, winLineP)
            }
        }

        fun animatePiece(r: Int, c: Int) {
            startAnimation(ScaleAnimation(0.2f, 1.1f, 0.2f, 1.1f,
                Animation.ABSOLUTE, ox+c*cs+cs/2f, Animation.ABSOLUTE, oy+r*cs+cs/2f).apply {
                duration = 200; interpolator = OvershootInterpolator(2.5f); fillAfter = true
            })
            invalidate()
        }

        fun animateWinLine() {
            winProgress = 0f
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 600; interpolator = DecelerateInterpolator()
                addUpdateListener { winProgress = it.animatedValue as Float; invalidate() }
                start()
            }
        }

        fun cellAt(x: Float, y: Float): Pair<Int,Int>? {
            val c = ((x-ox)/cs).toInt(); val r = ((y-oy)/cs).toInt()
            return if (r in 0..2 && c in 0..2) r to c else null
        }

        override fun onMeasure(wSpec: Int, hSpec: Int) {
            val w = MeasureSpec.getSize(wSpec)
            setMeasuredDimension(w, w)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "TIC TAC TOE"

        rootFrame = FrameLayout(this).apply {
            setBackgroundColor(0xFF080614.toInt())
        }

        // ── Scrollable main content ──────────────────────────────────────────
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF080614.toInt())
        }

        // ── Score header ─────────────────────────────────────────────────────
        main.addView(buildHeader())

        // ── Mode selector ────────────────────────────────────────────────────
        modeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(12, 8, 12, 8)
            setBackgroundColor(0xFF0A0820.toInt())
        }
        listOf("👥 2 Players", "🤖 Easy AI", "🧠 Hard AI").forEachIndexed { i, label ->
            modeContainer.addView(TextView(this).apply {
                text = label; textSize = 11f; gravity = Gravity.CENTER
                setPadding(4, 10, 4, 10)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(3,0,3,0) }
                tag = i
                setOnClickListener { setMode(i) }
            })
        }
        main.addView(modeContainer)

        // ── Status — fixed height so it never hides ──────────────────────────
        tvStatus = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            // Fixed height = always visible, never collapses
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 56.dp
            )
            setTextColor(0xFFFFD700.toInt())
        }
        main.addView(tvStatus)

        // ── Board ────────────────────────────────────────────────────────────
        boardView = BoardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(20, 0, 20, 0) }
            setOnTouchListener { _, ev ->
                if (ev.action == MotionEvent.ACTION_UP && !gameOver)
                    boardView.cellAt(ev.x, ev.y)?.let { (r,c) -> onCellTap(r,c) }
                true
            }
        }
        main.addView(boardView)

        // ── Rule hint — BELOW board, clearly separated ───────────────────────
        val ruleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0820.toInt())
            setPadding(20, 12, 20, 12)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(20, 12, 20, 4) }
        }

        // Divider line above rule box
        ruleBox.addView(View(this).apply {
            setBackgroundColor(0xFF1A2A6E.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { bottomMargin = 10 }
        })

        tvRuleHint = TextView(this).apply {
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(0xFF8892B0.toInt())
            setLineSpacing(0f, 1.5f)
        }
        ruleBox.addView(tvRuleHint)
        main.addView(ruleBox)

        // ── Buttons ──────────────────────────────────────────────────────────
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(20, 12, 20, 24)
        }
        val btnNew = Button(this).apply {
            text = "🔄  NEW ROUND"; textSize = 13f
            setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFF00E5FF.toInt())
            setPadding(20, 14, 20, 14)
            setOnClickListener { resetBoard() }
        }
        val btnReset = Button(this).apply {
            text = "🗑  RESET"; textSize = 13f
            setTextColor(0xFF00E5FF.toInt())
            setBackgroundColor(0xFF1A2A6E.toInt())
            setPadding(20, 14, 20, 14)
            setOnClickListener { resetScores() }
        }
        btnNew.layoutParams = LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 8 }
        btnReset.layoutParams = LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 8 }
        btnRow.addView(btnNew); btnRow.addView(btnReset)
        main.addView(btnRow)

        scroll.addView(main)
        rootFrame.addView(scroll)

        // ── Full-screen overlay ───────────────────────────────────────────────
        overlayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        tvOverlayEmoji = TextView(this).apply { textSize = 72f; gravity = Gravity.CENTER }
        tvOverlayMsg = TextView(this).apply {
            textSize = 26f; gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(24, 8, 24, 40)
        }
        val btnPlay = Button(this).apply {
            text = "▶  PLAY AGAIN"; textSize = 16f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF000000.toInt()); setBackgroundColor(0xFF00E5FF.toInt())
            setPadding(48, 18, 48, 18)
            setOnClickListener { hideOverlay(); resetBoard() }
        }
        overlayLayout.addView(tvOverlayEmoji)
        overlayLayout.addView(tvOverlayMsg)
        overlayLayout.addView(btnPlay)
        rootFrame.addView(overlayLayout)

        setContentView(rootFrame)
        setMode(0)
    }

    // dp extension
    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()

    private fun buildHeader(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF0D0B20.toInt())
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 16, 8, 16)
        }

        fun makeCard(isX: Boolean): Triple<LinearLayout, TextView, TextView> {
            val color = if (isX) 0xFF00E5FF.toInt() else 0xFFFF4081.toInt()
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
                setPadding(8, 10, 8, 10)
            }
            val sym = TextView(this).apply {
                text = if (isX) "✕" else "○"
                textSize = 28f; gravity = Gravity.CENTER
                setTextColor(color); typeface = Typeface.DEFAULT_BOLD
            }
            val name = TextView(this).apply {
                text = if (isX) "Player X" else "Player O"
                textSize = 10f; gravity = Gravity.CENTER
                setTextColor(0xFF8892B0.toInt())
                setPadding(0, 2, 0, 2)
            }
            val score = TextView(this).apply {
                text = "0"; textSize = 28f; gravity = Gravity.CENTER
                setTextColor(color); typeface = Typeface.DEFAULT_BOLD
            }
            card.addView(sym); card.addView(name); card.addView(score)
            return Triple(card, name, score)
        }

        val (c1, n1, s1) = makeCard(true)
        tvP1Card = c1; tvP1Name = n1; tvP1Score = s1

        header.addView(c1)
        header.addView(TextView(this).apply {
            text = "VS"; textSize = 13f; setTextColor(0xFF8892B0.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val (c2, n2, s2) = makeCard(false)
        tvP2Card = c2; tvP2Name = n2; tvP2Score = s2
        header.addView(c2)

        return header
    }

    private fun setMode(mode: Int) {
        gameMode = mode
        playerNames = when (mode) {
            1 -> arrayOf("You (X)", "Easy AI")
            2 -> arrayOf("You (X)", "Hard AI")
            else -> arrayOf("Player X", "Player O")
        }
        tvP1Name.text = playerNames[0]; tvP2Name.text = playerNames[1]
        for (i in 0 until modeContainer.childCount) {
            val tv = modeContainer.getChildAt(i) as TextView
            val sel = (tv.tag as Int) == mode
            tv.setTextColor(if (sel) 0xFF000000.toInt() else 0xFF00E5FF.toInt())
            tv.setBackgroundColor(if (sel) 0xFF00E5FF.toInt() else 0xFF0F1435.toInt())
        }
        resetScores(); resetBoard()
    }

    private fun onCellTap(r: Int, c: Int) {
        if (gameOver || board[r][c] != 0) return
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
            moveCount == 9 -> endGame(0)
            else -> {
                currentPlayer = if (currentPlayer == 1) 2 else 1
                updateTurnUI()
                if (gameMode > 0 && currentPlayer == 2)
                    Handler(Looper.getMainLooper()).postDelayed({ aiMove() }, 550)
            }
        }
    }

    private fun checkWinner(): Int {
        val lines = listOf(
            listOf(0 to 0,0 to 1,0 to 2), listOf(1 to 0,1 to 1,1 to 2),
            listOf(2 to 0,2 to 1,2 to 2), listOf(0 to 0,1 to 0,2 to 0),
            listOf(0 to 1,1 to 1,2 to 1), listOf(0 to 2,1 to 2,2 to 2),
            listOf(0 to 0,1 to 1,2 to 2), listOf(0 to 2,1 to 1,2 to 0)
        )
        for (line in lines) {
            val v = line.map { board[it.first][it.second] }
            if (v.all{it==1}) { winLine=line; boardView.animateWinLine(); return 1 }
            if (v.all{it==2}) { winLine=line; boardView.animateWinLine(); return 2 }
        }
        return 0
    }

    private fun endGame(winner: Int) {
        gameOver = true
        boardView.invalidate()
        AdManager.onGameCompleted()
        when (winner) {
            1 -> {
                scores[0]++; tvP1Score.text = scores[0].toString()
                tvStatus.text = "🏆  ${playerNames[0]} Wins!"
                tvStatus.setTextColor(0xFFFFD700.toInt())
                showOverlay("🎉", "${playerNames[0]} WINS!", 0xFF00E5FF.toInt())
            }
            2 -> {
                scores[1]++; tvP2Score.text = scores[1].toString()
                tvStatus.text = "🏆  ${playerNames[1]} Wins!"
                tvStatus.setTextColor(0xFFFFD700.toInt())
                showOverlay("🎊", "${playerNames[1]} WINS!", 0xFFFF4081.toInt())
            }
            else -> {
                scores[2]++
                tvStatus.text = "🤝  It's a Draw!"
                tvStatus.setTextColor(0xFFFFD700.toInt())
                showOverlay("🤝", "DRAW — GREAT GAME!", 0xFFFFD700.toInt())
            }
        }
        // Highlight active player card
        updateCardHighlight(0) // no active player after game ends
    }

    private fun aiMove() {
        if (gameOver) return
        val move = if (gameMode == 2) bestMove() else randomMove()
        move?.let { makeMove(it.first, it.second) }
    }

    private fun randomMove(): Pair<Int,Int>? {
        val e = mutableListOf<Pair<Int,Int>>()
        for (r in 0..2) for (c in 0..2) if (board[r][c]==0) e.add(r to c)
        return e.randomOrNull()
    }

    private fun bestMove(): Pair<Int,Int>? {
        var best = Int.MIN_VALUE; var move: Pair<Int,Int>? = null
        for (r in 0..2) for (c in 0..2) if (board[r][c]==0) {
            board[r][c] = 2
            val s = minimax(false, 0); board[r][c] = 0
            if (s > best) { best = s; move = r to c }
        }
        return move
    }

    private fun minimax(isMax: Boolean, depth: Int): Int {
        val w = minicheckWinner()
        if (w == 2) return 10 - depth
        if (w == 1) return depth - 10
        if ((0..2).all { r -> (0..2).all { c -> board[r][c] != 0 } }) return 0
        return if (isMax) {
            var b = Int.MIN_VALUE
            for (r in 0..2) for (c in 0..2) if (board[r][c]==0) {
                board[r][c]=2; b=maxOf(b,minimax(false,depth+1)); board[r][c]=0
            }
            b
        } else {
            var b = Int.MAX_VALUE
            for (r in 0..2) for (c in 0..2) if (board[r][c]==0) {
                board[r][c]=1; b=minOf(b,minimax(true,depth+1)); board[r][c]=0
            }
            b
        }
    }

    private fun minicheckWinner(): Int {
        val lines = listOf(
            listOf(0,1,2).map{0 to it}, listOf(0,1,2).map{1 to it}, listOf(0,1,2).map{2 to it},
            listOf(0,1,2).map{it to 0}, listOf(0,1,2).map{it to 1}, listOf(0,1,2).map{it to 2},
            listOf(0,1,2).map{it to it}, listOf(0,1,2).map{it to 2-it}
        )
        for (line in lines) {
            val v = line.map { board[it.first][it.second] }
            if (v.all{it==1}) return 1; if (v.all{it==2}) return 2
        }
        return 0
    }

    private fun updateTurnUI() {
        val name = if (currentPlayer==1) playerNames[0] else playerNames[1]
        val color = if (currentPlayer==1) 0xFF00E5FF.toInt() else 0xFFFF4081.toInt()
        val sym = if (currentPlayer==1) "✕" else "○"
        tvStatus.text = "$sym  $name's Turn"
        tvStatus.setTextColor(color)
        tvRuleHint.text = when (gameMode) {
            1 -> "You are ✕  •  Easy AI plays randomly\nGet 3 in a row → ↓ ↘ ↗"
            2 -> "You are ✕  •  Hard AI (Minimax — unbeatable!)\nTip: Always take center first"
            else -> "Pass & Play  •  Get 3 in a row to win\nRows  •  Columns  •  Diagonals"
        }
        updateCardHighlight(currentPlayer)
    }

    private fun updateCardHighlight(activePlayer: Int) {
        // Highlight active player card with glowing border effect
        tvP1Card.setBackgroundColor(
            if (activePlayer == 1) 0xFF0A2040.toInt() else 0x00000000.toInt()
        )
        tvP2Card.setBackgroundColor(
            if (activePlayer == 2) 0xFF400020.toInt() else 0x00000000.toInt()
        )
    }

    private fun showOverlay(emoji: String, msg: String, color: Int) {
        tvOverlayEmoji.text = emoji
        tvOverlayMsg.text = msg
        tvOverlayMsg.setTextColor(0xFFFFD700.toInt())
        overlayLayout.setBackgroundColor(0xF2080614.toInt())
        overlayLayout.visibility = View.VISIBLE
        overlayLayout.alpha = 0f
        overlayLayout.animate().alpha(1f).setDuration(350).start()
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(tvOverlayEmoji, "scaleX", 0.3f, 1.2f, 1f),
                ObjectAnimator.ofFloat(tvOverlayEmoji, "scaleY", 0.3f, 1.2f, 1f)
            )
            duration = 500; start()
        }
    }

    private fun hideOverlay() {
        overlayLayout.animate().alpha(0f).setDuration(250).withEndAction {
            overlayLayout.visibility = View.GONE
        }.start()
    }

    private fun resetBoard() {
        for (r in 0..2) board[r].fill(0)
        currentPlayer = 1; gameOver = false; moveCount = 0; winLine = null
        boardView.winProgress = 1f; boardView.invalidate()
        hideOverlay(); updateTurnUI()
    }

    private fun resetScores() {
        scores.fill(0); tvP1Score.text = "0"; tvP2Score.text = "0"
    }
}
