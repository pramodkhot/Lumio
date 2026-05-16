package com.lumio.gamezone.games

import android.animation.*
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.*
import com.lumio.gamezone.ads.AdManager
import com.lumio.gamezone.ui.BaseGameActivity

class HexaFallActivity : BaseGameActivity() {

    private val COLS = 7; private val ROWS = 11
    private val COLORS = intArrayOf(
        0xFFFF5252.toInt(), 0xFF448AFF.toInt(), 0xFF00E676.toInt(),
        0xFFFFD700.toInt(), 0xFFFF4081.toInt(), 0xFF00E5FF.toInt()
    )

    private val grid = Array(ROWS) { IntArray(COLS) { 0 } } // 0=empty
    private var nextColor = 0
    private var score = 0
    private var gameOver = false
    private var highlightCol = -1

    private lateinit var gameView: GameView
    private lateinit var tvScore: TextView
    private lateinit var tvNext: TextView
    private lateinit var tvStatus: TextView
    private lateinit var overlayLayout: FrameLayout
    private lateinit var tvOverlayMsg: TextView
    private val clearGlow = Array(ROWS) { BooleanArray(COLS) }
    private var glowAlpha = 0f

    inner class GameView(ctx: android.content.Context) : View(ctx) {
        var cellW = 0f; var cellH = 0f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.5f; color = 0xFF1A2A6E.toInt()
        }
        private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; color = 0xFFFFD700.toInt()
        }
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }

        override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
            cellW = w.toFloat() / COLS
            cellH = h.toFloat() / ROWS
        }

        override fun onDraw(canvas: Canvas) {
            for (r in 0 until ROWS) {
                for (c in 0 until COLS) {
                    val x = c * cellW; val y = r * cellH
                    val rect = RectF(x+3f, y+3f, x+cellW-3f, y+cellH-3f)

                    paint.color = if (grid[r][c] != 0) grid[r][c] else 0xFF0F1435.toInt()
                    canvas.drawRoundRect(rect, 8f, 8f, paint)

                    // Glow on cleared
                    if (clearGlow[r][c] && glowAlpha > 0f) {
                        glowPaint.color = 0xFFFFFFFF.toInt()
                        glowPaint.alpha = (glowAlpha * 255).toInt()
                        canvas.drawRoundRect(rect, 8f, 8f, glowPaint)
                    }

                    canvas.drawRoundRect(rect, 8f, 8f, strokePaint)

                    // Highlight column
                    if (c == highlightCol && grid[r][c] == 0) {
                        canvas.drawRoundRect(rect, 8f, 8f, highlightPaint)
                    }
                }
            }

            // Drop arrow at top of highlighted column
            if (highlightCol >= 0) {
                arrowPaint.color = 0xFFFFD700.toInt()
                arrowPaint.textSize = cellH * 0.7f
                canvas.drawText("▼", highlightCol * cellW + cellW / 2, cellH * 0.8f, arrowPaint)
            }
        }

        override fun onMeasure(wSpec: Int, hSpec: Int) {
            val w = MeasureSpec.getSize(wSpec)
            setMeasuredDimension(w, (w * ROWS / COLS.toFloat()).toInt())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "HEXA FALL"

        val root = FrameLayout(this).apply { setBackgroundColor(0xFF080614.toInt()) }
        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF080614.toInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // ── Header ────────────────────────────────────────────────────────────
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF0D0B20.toInt())
            gravity = Gravity.CENTER_VERTICAL; setPadding(16,12,16,12)
        }
        tvScore = TextView(this).apply {
            textSize = 22f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF00E5FF.toInt()); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val nextLabel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvNext = TextView(this).apply {
            textSize = 28f; gravity = Gravity.CENTER
        }
        nextLabel.addView(TextView(this).apply {
            text = "NEXT"; textSize = 9f; setTextColor(0xFF8892B0.toInt()); gravity = Gravity.CENTER
        })
        nextLabel.addView(tvNext)

        tvStatus = TextView(this).apply {
            textSize = 13f; setTextColor(0xFFFFD700.toInt()); gravity = Gravity.CENTER
            minHeight = 44.dp
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }
        header.addView(tvScore); header.addView(tvStatus); header.addView(nextLabel)
        main.addView(header)

        // ── Column buttons ────────────────────────────────────────────────────
        val colBtns = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF0A0820.toInt())
        }
        val screenW = resources.displayMetrics.widthPixels
        val btnW = screenW / COLS
        for (c in 0 until COLS) {
            colBtns.addView(TextView(this).apply {
                text = "▼"; textSize = 16f; gravity = Gravity.CENTER
                setTextColor(0xFF3A5A9E.toInt())
                layoutParams = LinearLayout.LayoutParams(btnW, 36.dp)
                val col = c
                setOnClickListener {
                    highlightCol = col
                    gameView.invalidate()
                    dropPiece(col)
                }
                setOnHoverListener { _, _ -> setTextColor(0xFFFFD700.toInt()); false }
            })
        }
        main.addView(colBtns)

        // ── Game view ─────────────────────────────────────────────────────────
        gameView = GameView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(4,4,4,4) }
            setOnTouchListener { _, ev ->
                if (ev.action == MotionEvent.ACTION_UP) {
                    val col = (ev.x / (width.toFloat() / COLS)).toInt().coerceIn(0, COLS-1)
                    highlightCol = col
                    gameView.invalidate()
                    dropPiece(col)
                } else if (ev.action == MotionEvent.ACTION_MOVE) {
                    val col = (ev.x / (width.toFloat() / COLS)).toInt().coerceIn(0, COLS-1)
                    highlightCol = col; gameView.invalidate()
                }
                true
            }
        }
        main.addView(gameView)

        // ── Help text ─────────────────────────────────────────────────────────
        main.addView(buildHelpBar("Tap a column to drop the colored piece  •  3 or more same color in a row clears them  •  Column full = game over"))

        root.addView(main)

        // ── Overlay ───────────────────────────────────────────────────────────
        overlayLayout = FrameLayout(this).apply {
            visibility = View.GONE; setBackgroundColor(0xF0080614.toInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        val oc = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        val tvOIcon = TextView(this).apply { text = "💥"; textSize = 64f; gravity = Gravity.CENTER }
        tvOverlayMsg = TextView(this).apply {
            textSize = 20f; setTextColor(0xFFFFD700.toInt())
            gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD; setPadding(0,8,0,32)
        }
        val btnRestart = Button(this).apply {
            text = "▶  PLAY AGAIN"; textSize = 16f
            setTextColor(0xFF000000.toInt()); setBackgroundColor(0xFF00E5FF.toInt())
            setPadding(48, 16, 48, 16)
            setOnClickListener { hideOverlay(); startGame() }
        }
        oc.addView(tvOIcon); oc.addView(tvOverlayMsg); oc.addView(btnRestart)
        overlayLayout.addView(oc)
        root.addView(overlayLayout)

        setContentView(root)
        startGame()
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()

    private fun startGame() {
        for (r in 0 until ROWS) grid[r].fill(0)
        for (r in 0 until ROWS) clearGlow[r].fill(false)
        score = 0; gameOver = false; highlightCol = -1
        nextColor = COLORS.random()
        updateHeader()
        tvStatus.text = "TAP A COLUMN TO DROP THE PIECE"
        tvStatus.setTextColor(0xFFFFD700.toInt())
        gameView.invalidate()
    }

    private fun dropPiece(col: Int) {
        if (gameOver) return
        val color = nextColor

        // Find bottom-most empty row
        var targetRow = -1
        for (r in ROWS - 1 downTo 0) {
            if (grid[r][col] == 0) { targetRow = r; break }
        }

        if (targetRow == -1) {
            // Column full
            tvStatus.text = "❌ COLUMN FULL! Choose another column"
            tvStatus.setTextColor(0xFFFF5252.toInt())
            return
        }

        grid[targetRow][col] = color
        score += 1
        nextColor = COLORS.random()
        updateHeader()
        gameView.invalidate()

        tvStatus.text = "DROPPED! Check for matches..."
        tvStatus.setTextColor(0xFF00E5FF.toInt())

        // Check matches
        val matched = findMatches()
        if (matched > 0) {
            animateClear(matched)
        } else {
            tvStatus.text = "TAP A COLUMN TO DROP"
            tvStatus.setTextColor(0xFFFFD700.toInt())
            checkGameOver()
        }
    }

    private fun findMatches(): Int {
        var count = 0
        for (r in 0 until ROWS) clearGlow[r].fill(false)

        // Check horizontal matches
        for (r in 0 until ROWS) {
            var c = 0
            while (c < COLS) {
                if (grid[r][c] == 0) { c++; continue }
                val color = grid[r][c]; var len = 1
                while (c + len < COLS && grid[r][c + len] == color) len++
                if (len >= 3) { for (i in 0 until len) clearGlow[r][c+i] = true; count += len }
                c += len
            }
        }
        // Check vertical matches
        for (c in 0 until COLS) {
            var r = 0
            while (r < ROWS) {
                if (grid[r][c] == 0) { r++; continue }
                val color = grid[r][c]; var len = 1
                while (r + len < ROWS && grid[r + len][c] == color) len++
                if (len >= 3) { for (i in 0 until len) clearGlow[r+i][c] = true; count += len }
                r += len
            }
        }
        return count
    }

    private fun animateClear(count: Int) {
        score += count * 5
        updateHeader()
        tvStatus.text = "✨ $count CELLS CLEARED! +${count*5} pts"
        tvStatus.setTextColor(0xFF00E676.toInt())

        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 500
            addUpdateListener { glowAlpha = it.animatedValue as Float; gameView.invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) {
                    for (r in 0 until ROWS) for (c in 0 until COLS)
                        if (clearGlow[r][c]) grid[r][c] = 0
                    for (r in 0 until ROWS) clearGlow[r].fill(false)
                    glowAlpha = 0f
                    applyGravity()
                    gameView.invalidate()
                    tvStatus.text = "TAP A COLUMN TO DROP"
                    tvStatus.setTextColor(0xFFFFD700.toInt())
                    checkGameOver()
                }
            })
            start()
        }
    }

    private fun applyGravity() {
        for (c in 0 until COLS) {
            val column = (0 until ROWS).map { grid[it][c] }.filter { it != 0 }
            for (r in 0 until ROWS) grid[r][c] = 0
            column.forEachIndexed { i, color -> grid[ROWS - column.size + i][c] = color }
        }
    }

    private fun checkGameOver() {
        val allFull = (0 until COLS).all { c -> grid[0][c] != 0 }
        if (allFull) {
            gameOver = true
            AdManager.onGameCompleted()
            tvOverlayMsg.text = "GRID FULL!\nScore: $score"
            overlayLayout.visibility = View.VISIBLE
            overlayLayout.alpha = 0f
            overlayLayout.animate().alpha(1f).setDuration(400).start()
        }
    }

    private fun updateHeader() {
        tvScore.text = "⭐ $score"
        tvNext.text = "⬜"
        tvNext.setTextColor(nextColor)
        tvNext.setBackgroundColor(nextColor and 0x00FFFFFF or 0x22000000)
    }

    private fun hideOverlay() {
        overlayLayout.animate().alpha(0f).setDuration(250).withEndAction {
            overlayLayout.visibility = View.GONE
        }.start()
    }

    private fun buildHelpBar(text: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF050410.toInt())
            setPadding(16, 8, 16, 12)
            addView(View(this@HexaFallActivity).apply {
                setBackgroundColor(0xFF1A2A6E.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply { bottomMargin = 8 }
            })
            addView(TextView(this@HexaFallActivity).apply {
                this.text = text; textSize = 11f
                setTextColor(0xFF8892B0.toInt())
                gravity = Gravity.CENTER; setLineSpacing(0f, 1.4f)
            })
        }
    }
}
