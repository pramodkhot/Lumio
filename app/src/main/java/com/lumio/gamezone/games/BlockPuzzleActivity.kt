package com.lumio.gamezone.games

import android.animation.*
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import com.lumio.gamezone.ads.AdManager
import com.lumio.gamezone.ui.BaseGameActivity

class BlockPuzzleActivity : BaseGameActivity() {

    // ── Constants ─────────────────────────────────────────────────────────────
    private val GRID = 8
    private val COLORS = intArrayOf(
        0xFF00E5FF.toInt(), 0xFFFF4081.toInt(), 0xFFFFD700.toInt(),
        0xFF00E676.toInt(), 0xFF9C27B0.toInt(), 0xFFFF9800.toInt(),
        0xFF448AFF.toInt(), 0xFFFF5252.toInt()
    )

    private val SHAPES = listOf(
        // 1x1
        listOf(listOf(1)),
        // 1x2
        listOf(listOf(1,1)),
        // 2x1
        listOf(listOf(1), listOf(1)),
        // 1x3
        listOf(listOf(1,1,1)),
        // 3x1
        listOf(listOf(1), listOf(1), listOf(1)),
        // 1x4
        listOf(listOf(1,1,1,1)),
        // 4x1
        listOf(listOf(1), listOf(1), listOf(1), listOf(1)),
        // 2x2
        listOf(listOf(1,1), listOf(1,1)),
        // L shape
        listOf(listOf(1,0), listOf(1,0), listOf(1,1)),
        // J shape
        listOf(listOf(0,1), listOf(0,1), listOf(1,1)),
        // T shape
        listOf(listOf(1,1,1), listOf(0,1,0)),
        // S shape
        listOf(listOf(0,1,1), listOf(1,1,0)),
        // Z shape
        listOf(listOf(1,1,0), listOf(0,1,1)),
        // 3x2
        listOf(listOf(1,1,1), listOf(1,1,1)),
        // corner
        listOf(listOf(1,1), listOf(1,0))
    )

    // ── State ─────────────────────────────────────────────────────────────────
    private val grid = Array(GRID) { IntArray(GRID) { 0 } }      // 0=empty, color=filled
    private val glowGrid = Array(GRID) { BooleanArray(GRID) }     // cells to animate clear
    private var score = 0
    private var highScore = 0
    private var gameOver = false

    data class Block(val shape: List<List<Int>>, val color: Int)
    private var blocks = mutableListOf<Block?>()
    private var selectedBlock = -1
    private var hoverRow = -1
    private var hoverCol = -1
    private var canPlace = false

    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var gameView: GameView
    private lateinit var blockViews: Array<BlockPreviewView?>
    private lateinit var tvScore: TextView
    private lateinit var tvHigh: TextView
    private lateinit var tvStatus: TextView
    private lateinit var overlayLayout: FrameLayout
    private lateinit var tvOverlayTitle: TextView
    private lateinit var tvOverlayScore: TextView

    // ── GameView ──────────────────────────────────────────────────────────────
    inner class GameView(ctx: android.content.Context) : View(ctx) {
        var cellSize = 0f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.5f; color = 0xFF1A2A6E.toInt()
        }
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        var glowAlpha = 0f

        override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
            cellSize = w.toFloat() / GRID
        }

        override fun onDraw(canvas: Canvas) {
            val cs = cellSize
            for (r in 0 until GRID) {
                for (c in 0 until GRID) {
                    val x = c * cs; val y = r * cs
                    val rect = RectF(x+2f, y+2f, x+cs-2f, y+cs-2f)

                    // Cell background
                    paint.color = if (grid[r][c] != 0) grid[r][c] else 0xFF0F1435.toInt()
                    canvas.drawRoundRect(rect, 6f, 6f, paint)

                    // Glow on cleared cells
                    if (glowGrid[r][c] && glowAlpha > 0f) {
                        glowPaint.color = 0xFFFFFFFF.toInt()
                        glowPaint.alpha = (glowAlpha * 255).toInt()
                        canvas.drawRoundRect(rect, 6f, 6f, glowPaint)
                    }

                    // Grid line
                    canvas.drawRoundRect(rect, 6f, 6f, strokePaint)

                    // Hover preview
                    if (selectedBlock >= 0 && hoverRow >= 0) {
                        val sel = blocks[selectedBlock] ?: return
                        val sr = hoverRow; val sc = hoverCol
                        val sh = sel.shape
                        for (br in sh.indices) for (bc in sh[br].indices) {
                            if (sh[br][bc] == 1 && sr+br == r && sc+bc == c) {
                                paint.color = if (canPlace)
                                    (sel.color and 0x00FFFFFF) or 0x99000000.toInt()
                                else 0x99FF0000.toInt()
                                canvas.drawRoundRect(rect, 6f, 6f, paint)
                            }
                        }
                    }
                }
            }
        }

        override fun onMeasure(wSpec: Int, hSpec: Int) {
            val w = MeasureSpec.getSize(wSpec)
            setMeasuredDimension(w, w)
        }
    }

    // ── Block Preview View ────────────────────────────────────────────────────
    inner class BlockPreviewView(ctx: android.content.Context, var block: Block?) : View(ctx) {
        var isSelected = false
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; color = 0xFFFFD700.toInt()
        }

        override fun onDraw(canvas: Canvas) {
            val b = block ?: run {
                paint.color = 0xFF0A0820.toInt()
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                return
            }
            val rows = b.shape.size
            val cols = b.shape.maxOf { it.size }
            val cs = minOf(width.toFloat() / (cols + 1), height.toFloat() / (rows + 1))
            val ox = (width - cols * cs) / 2f
            val oy = (height - rows * cs) / 2f

            b.shape.forEachIndexed { r, row ->
                row.forEachIndexed { c, cell ->
                    if (cell == 1) {
                        val x = ox + c * cs; val y = oy + r * cs
                        paint.color = b.color
                        canvas.drawRoundRect(RectF(x+2f,y+2f,x+cs-2f,y+cs-2f), 6f,6f, paint)
                    }
                }
            }
            if (isSelected) {
                canvas.drawRoundRect(RectF(2f,2f,width-2f,height-2f), 12f,12f, selPaint)
            }
        }
    }

    // ── onCreate ──────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "BLOCK PUZZLE"

        val root = FrameLayout(this).apply {
            setBackgroundColor(0xFF080614.toInt())
        }

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF080614.toInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // ── Header ───────────────────────────────────────────────────────────
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF0D0B20.toInt())
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 12, 16, 12)
        }
        tvScore = TextView(this).apply {
            textSize = 22f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF00E5FF.toInt()); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvScoreLabel = TextView(this).apply {
            text = "SCORE"; textSize = 10f; setTextColor(0xFF8892B0.toInt())
            gravity = Gravity.CENTER
        }
        val scoreCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        scoreCol.addView(tvScore); scoreCol.addView(tvScoreLabel)

        tvHigh = TextView(this).apply {
            textSize = 22f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFD700.toInt()); gravity = Gravity.CENTER
        }
        val tvHighLabel = TextView(this).apply {
            text = "BEST"; textSize = 10f; setTextColor(0xFF8892B0.toInt())
            gravity = Gravity.CENTER
        }
        val highCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        highCol.addView(tvHigh); highCol.addView(tvHighLabel)

        tvStatus = TextView(this).apply {
            textSize = 13f; setTextColor(0xFF00E5FF.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }
        header.addView(scoreCol); header.addView(tvStatus); header.addView(highCol)
        main.addView(header)

        // ── Game board ────────────────────────────────────────────────────────
        gameView = GameView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(8, 8, 8, 8) }
            setOnTouchListener { _, ev -> onBoardTouch(ev); true }
        }
        main.addView(gameView)

        // ── Block selector ────────────────────────────────────────────────────
        val blockRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF0D0B20.toInt())
            setPadding(8, 12, 8, 12)
        }
        blockViews = arrayOfNulls(3)
        for (i in 0..2) {
            blockViews[i] = BlockPreviewView(this, null).apply {
                val size = (resources.displayMetrics.widthPixels - 48) / 3
                layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(4,0,4,0) }
                setBackgroundColor(0xFF0F1435.toInt())
                val idx = i
                setOnClickListener {
                    if (block == null || gameOver) return@setOnClickListener
                    selectedBlock = idx
                    blockViews.forEachIndexed { j, v ->
                        v?.isSelected = (j == idx); v?.invalidate()
                    }
                    tvStatus.text = "TAP THE GRID TO PLACE"
                    tvStatus.setTextColor(0xFF00E5FF.toInt())
                }
            }
            blockRow.addView(blockViews[i])
        }
        main.addView(blockRow)

        // ── Help text ─────────────────────────────────────────────────────────
        main.addView(buildHelpBar(
            "① Tap a block below  ② Tap grid to place  ③ Fill rows or columns to clear  ④ Game over when no block fits"
        ))

        root.addView(main)

        // ── Game over overlay ─────────────────────────────────────────────────
        overlayLayout = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(0xF0080614.toInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        val overlayContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        tvOverlayTitle = TextView(this).apply {
            text = "GAME OVER"; textSize = 36f
            setTextColor(0xFFFF4081.toInt()); gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD; setPadding(0,0,0,8)
        }
        tvOverlayScore = TextView(this).apply {
            textSize = 20f; setTextColor(0xFFFFD700.toInt())
            gravity = Gravity.CENTER; setPadding(0,0,0,32)
        }
        val btnRestart = Button(this).apply {
            text = "▶  PLAY AGAIN"; textSize = 16f
            setTextColor(0xFF000000.toInt()); setBackgroundColor(0xFF00E5FF.toInt())
            setPadding(48, 16, 48, 16)
            setOnClickListener { hideOverlay(); startNewGame() }
        }
        overlayContent.addView(tvOverlayTitle)
        overlayContent.addView(tvOverlayScore)
        overlayContent.addView(btnRestart)
        overlayLayout.addView(overlayContent)
        root.addView(overlayLayout)

        setContentView(root)
        startNewGame()
    }

    // ── Game logic ────────────────────────────────────────────────────────────
    private fun startNewGame() {
        for (r in 0 until GRID) grid[r].fill(0)
        for (r in 0 until GRID) glowGrid[r].fill(false)
        score = 0; gameOver = false; selectedBlock = -1
        hoverRow = -1; hoverCol = -1
        refillBlocks()
        updateScore()
        tvStatus.text = "SELECT A BLOCK BELOW"
        tvStatus.setTextColor(0xFF00E5FF.toInt())
        gameView.invalidate()
    }

    private fun refillBlocks() {
        blocks.clear()
        for (i in 0..2) {
            blocks.add(Block(SHAPES.random(), COLORS.random()))
        }
        updateBlockViews()
    }

    private fun updateBlockViews() {
        for (i in 0..2) {
            blockViews[i]?.block = blocks.getOrNull(i)
            blockViews[i]?.isSelected = (i == selectedBlock)
            blockViews[i]?.invalidate()
        }
    }

    private fun onBoardTouch(ev: MotionEvent) {
        if (gameOver || selectedBlock < 0) return
        val cs = gameView.cellSize
        val r = (ev.y / cs).toInt()
        val c = (ev.x / cs).toInt()

        when (ev.action) {
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                hoverRow = r; hoverCol = c
                val sel = blocks[selectedBlock] ?: return
                canPlace = canPlaceBlock(sel.shape, r, c)
                gameView.invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val sel = blocks[selectedBlock] ?: return
                if (canPlaceBlock(sel.shape, r, c)) {
                    placeBlock(sel, r, c)
                } else {
                    hoverRow = -1; hoverCol = -1
                    tvStatus.text = "❌ CAN'T PLACE HERE — TRY ANOTHER SPOT"
                    tvStatus.setTextColor(0xFFFF5252.toInt())
                    gameView.invalidate()
                }
            }
        }
    }

    private fun canPlaceBlock(shape: List<List<Int>>, row: Int, col: Int): Boolean {
        for (r in shape.indices) for (c in shape[r].indices) {
            if (shape[r][c] == 0) continue
            val gr = row + r; val gc = col + c
            if (gr < 0 || gr >= GRID || gc < 0 || gc >= GRID) return false
            if (grid[gr][gc] != 0) return false
        }
        return true
    }

    private fun placeBlock(block: Block, row: Int, col: Int) {
        // Place
        for (r in block.shape.indices) for (c in block.shape[r].indices) {
            if (block.shape[r][c] == 1) grid[row+r][col+c] = block.color
        }
        score += block.shape.sumOf { it.sum() }

        // Remove used block
        blocks[selectedBlock] = null
        selectedBlock = -1
        hoverRow = -1; hoverCol = -1

        // Animate placement
        gameView.invalidate()

        // Check and clear lines
        Handler(Looper.getMainLooper()).postDelayed({ checkAndClearLines() }, 150)
    }

    private fun checkAndClearLines() {
        val rowsToClear = mutableListOf<Int>()
        val colsToClear = mutableListOf<Int>()

        for (r in 0 until GRID) if (grid[r].all { it != 0 }) rowsToClear.add(r)
        for (c in 0 until GRID) if ((0 until GRID).all { grid[it][c] != 0 }) colsToClear.add(c)

        if (rowsToClear.isEmpty() && colsToClear.isEmpty()) {
            afterClear()
            return
        }

        // Mark cells for glow animation
        for (r in rowsToClear) for (c in 0 until GRID) glowGrid[r][c] = true
        for (c in colsToClear) for (r in 0 until GRID) glowGrid[r][c] = true

        // Score
        val cleared = rowsToClear.size * GRID + colsToClear.size * GRID
        score += cleared * 2
        if (score > highScore) highScore = score

        // Glow animation
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 500
            addUpdateListener {
                gameView.glowAlpha = it.animatedValue as Float
                gameView.invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Clear cells
                    for (r in rowsToClear) grid[r].fill(0)
                    for (c in colsToClear) for (r in 0 until GRID) grid[r][c] = 0
                    for (r in 0 until GRID) glowGrid[r].fill(false)
                    gameView.glowAlpha = 0f

                    val linesMsg = buildString {
                        if (rowsToClear.isNotEmpty()) append("${rowsToClear.size} ROW")
                        if (rowsToClear.isNotEmpty() && colsToClear.isNotEmpty()) append(" + ")
                        if (colsToClear.isNotEmpty()) append("${colsToClear.size} COL")
                        append(" CLEARED! +$cleared pts")
                    }
                    tvStatus.text = "✨ $linesMsg"
                    tvStatus.setTextColor(0xFF00E676.toInt())

                    updateScore()
                    afterClear()
                }
            })
            start()
        }
    }

    private fun afterClear() {
        updateBlockViews()
        // Refill if all 3 used
        if (blocks.all { it == null }) refillBlocks()

        // Check game over
        val hasMove = blocks.any { block ->
            block != null && (0 until GRID).any { r ->
                (0 until GRID).any { c -> canPlaceBlock(block.shape, r, c) }
            }
        }

        if (!hasMove) {
            gameOver = true
            AdManager.onGameCompleted()
            showGameOver()
        } else {
            if (tvStatus.text.toString().startsWith("✨").not()) {
                tvStatus.text = "SELECT A BLOCK BELOW"
                tvStatus.setTextColor(0xFF00E5FF.toInt())
            }
        }
        gameView.invalidate()
    }

    private fun updateScore() {
        tvScore.text = score.toString()
        tvHigh.text = highScore.toString()
    }

    private fun showGameOver() {
        tvOverlayTitle.text = "GAME OVER"
        tvOverlayScore.text = "Score: $score\nBest: $highScore"
        overlayLayout.visibility = View.VISIBLE
        overlayLayout.alpha = 0f
        overlayLayout.animate().alpha(1f).setDuration(400).start()
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
            addView(View(this@BlockPuzzleActivity).apply {
                setBackgroundColor(0xFF1A2A6E.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply { bottomMargin = 8 }
            })
            addView(TextView(this@BlockPuzzleActivity).apply {
                this.text = text; textSize = 11f
                setTextColor(0xFF8892B0.toInt())
                gravity = Gravity.CENTER
                setLineSpacing(0f, 1.4f)
            })
        }
    }
}
