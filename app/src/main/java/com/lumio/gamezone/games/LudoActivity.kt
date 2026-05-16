package com.lumio.gamezone.games

import android.graphics.*
import android.os.Bundle
import android.os.Looper
import android.os.Handler
import android.view.*
import android.widget.*
import com.lumio.gamezone.ui.BaseGameActivity

class LudoActivity : BaseGameActivity() {

    // Standard Ludo board as a custom View
    inner class LudoBoardView(context: android.content.Context) : View(context) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.WHITE
        }

        // Colors
        private val RED    = 0xFFE53935.toInt()
        private val BLUE   = 0xFF1565C0.toInt()
        private val GREEN  = 0xFF2E7D32.toInt()
        private val YELLOW = 0xFFF9A825.toInt()
        private val WHITE  = 0xFFFFFFFF.toInt()
        private val LIGHT  = 0xFFEEEEEE.toInt()
        private val DARK   = 0xFF111111.toInt()

        var tokenPositions = Array(4) { IntArray(4) { -1 } }
        var currentPlayer = 0
        var selectedTokens = mutableSetOf<Int>() // token indices for current player that can move

        private var cellSize = 0f
        private var boardSize = 0f

        // 15x15 Ludo grid
        // Safe squares on standard path
        private val SAFE_SQUARES = setOf(1,9,14,22,27,35,40,48)

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            boardSize = minOf(w, h).toFloat()
            cellSize = boardSize / 15f
        }

        override fun onDraw(canvas: Canvas) {
            if (cellSize == 0f) return
            drawBoard(canvas)
            drawTokens(canvas)
        }

        private fun drawBoard(canvas: Canvas) {
            val cs = cellSize

            // Background
            paint.color = 0xFF1A1A2E.toInt()
            canvas.drawRect(0f, 0f, boardSize, boardSize, paint)

            // Home areas (corners - 6x6 each)
            // RED - top left
            paint.color = RED
            canvas.drawRect(0f, 0f, cs*6, cs*6, paint)
            paint.color = WHITE
            canvas.drawRect(cs*0.5f, cs*0.5f, cs*5.5f, cs*5.5f, paint)

            // BLUE - top right
            paint.color = BLUE
            canvas.drawRect(cs*9, 0f, cs*15, cs*6, paint)
            paint.color = WHITE
            canvas.drawRect(cs*9.5f, cs*0.5f, cs*14.5f, cs*5.5f, paint)

            // GREEN - bottom right
            paint.color = GREEN
            canvas.drawRect(cs*9, cs*9, cs*15, cs*15, paint)
            paint.color = WHITE
            canvas.drawRect(cs*9.5f, cs*9.5f, cs*14.5f, cs*14.5f, paint)

            // YELLOW - bottom left
            paint.color = YELLOW
            canvas.drawRect(0f, cs*9, cs*6, cs*15, paint)
            paint.color = WHITE
            canvas.drawRect(cs*0.5f, cs*9.5f, cs*5.5f, cs*14.5f, paint)

            // Home token circles in corners
            val homeColors = intArrayOf(RED, BLUE, GREEN, YELLOW)
            val homePositions = arrayOf(
                arrayOf(1 to 1, 4 to 1, 1 to 4, 4 to 4),  // RED
                arrayOf(10 to 1, 13 to 1, 10 to 4, 13 to 4), // BLUE
                arrayOf(10 to 10, 13 to 10, 10 to 13, 13 to 13), // GREEN
                arrayOf(1 to 10, 4 to 10, 1 to 13, 4 to 13)  // YELLOW
            )
            homePositions.forEachIndexed { p, positions ->
                positions.forEachIndexed { t, (col, row) ->
                    paint.color = homeColors[p]
                    canvas.drawCircle(col*cs+cs/2, row*cs+cs/2, cs*0.35f, paint)
                    paint.color = WHITE
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
                    canvas.drawCircle(col*cs+cs/2, row*cs+cs/2, cs*0.35f, paint)
                    paint.style = Paint.Style.FILL
                    // Show token number
                    textPaint.color = WHITE; textPaint.textSize = cs*0.28f
                    canvas.drawText("T${t+1}", col*cs+cs/2, row*cs+cs/2+textPaint.textSize/3, textPaint)
                }
            }

            // Draw path cells (the track)
            // Outer path - 52 cells arranged around the board
            val pathCells = getPathCells()
            pathCells.forEachIndexed { i, (col, row) ->
                val isSafe = SAFE_SQUARES.contains(i)
                paint.color = if (isSafe) 0xFFFFEB3B.toInt() else LIGHT
                canvas.drawRect(col*cs+1f, row*cs+1f, col*cs+cs-1f, row*cs+cs-1f, paint)
                paint.color = 0x33000000
                canvas.drawRect(col*cs, row*cs, col*cs+cs, row*cs+cs, strokePaint)
                if (isSafe) {
                    textPaint.color = DARK; textPaint.textSize = cs*0.22f
                    canvas.drawText("★", col*cs+cs/2, row*cs+cs/2+textPaint.textSize/3, textPaint)
                }
            }

            // Colored entry columns (home stretch)
            // RED goes up column 1 (col 1, rows 7-1)
            for (r in 1..5) {
                paint.color = Color.argb(180, 229, 57, 53)
                canvas.drawRect(cs*1+1f, r*cs+1f, cs*2-1f, (r+1)*cs-1f, paint)
            }
            // BLUE goes left on row 1 (cols 7-13)  - actually row 7
            for (c in 9..13) {
                paint.color = Color.argb(180, 21, 101, 192)
                canvas.drawRect(c*cs+1f, cs*1+1f, (c+1)*cs-1f, cs*2-1f, paint)
            }
            // GREEN goes down col 13 (rows 9-13)
            for (r in 9..13) {
                paint.color = Color.argb(180, 46, 125, 50)
                canvas.drawRect(cs*13+1f, r*cs+1f, cs*14-1f, (r+1)*cs-1f, paint)
            }
            // YELLOW goes right on row 13 (cols 1-5)
            for (c in 1..5) {
                paint.color = Color.argb(180, 249, 168, 37)
                canvas.drawRect(c*cs+1f, cs*13+1f, (c+1)*cs-1f, cs*14-1f, paint)
            }

            // Center home (finishing area)
            val centerPath = Path().apply {
                moveTo(cs*6, cs*6); lineTo(cs*9, cs*6)
                lineTo(cs*7.5f, cs*7.5f); close()
            }
            paint.color = RED; canvas.drawPath(centerPath, paint)
            val centerPath2 = Path().apply {
                moveTo(cs*9, cs*6); lineTo(cs*9, cs*9)
                lineTo(cs*7.5f, cs*7.5f); close()
            }
            paint.color = BLUE; canvas.drawPath(centerPath2, paint)
            val centerPath3 = Path().apply {
                moveTo(cs*9, cs*9); lineTo(cs*6, cs*9)
                lineTo(cs*7.5f, cs*7.5f); close()
            }
            paint.color = GREEN; canvas.drawPath(centerPath3, paint)
            val centerPath4 = Path().apply {
                moveTo(cs*6, cs*9); lineTo(cs*6, cs*6)
                lineTo(cs*7.5f, cs*7.5f); close()
            }
            paint.color = YELLOW; canvas.drawPath(centerPath4, paint)

            // HOME label in center
            textPaint.color = WHITE; textPaint.textSize = cs*0.4f; textPaint.isFakeBoldText = true
            canvas.drawText("🏠", cs*7.5f, cs*7.8f, textPaint)
        }

        private fun drawTokens(canvas: Canvas) {
            val cs = cellSize
            val colors = intArrayOf(0xFFE53935.toInt(), 0xFF1565C0.toInt(), 0xFF2E7D32.toInt(), 0xFFF9A825.toInt())
            val tokenLabels = arrayOf("R","B","G","Y")
            val pathCells = getPathCells()

            tokenPositions.forEachIndexed { p, tokens ->
                tokens.forEachIndexed { t, pos ->
                    if (pos == 57) return@forEachIndexed // finished
                    val (col, row) = if (pos == -1) {
                        // At home
                        val homePositions = arrayOf(
                            arrayOf(1 to 1, 4 to 1, 1 to 4, 4 to 4),
                            arrayOf(10 to 1, 13 to 1, 10 to 4, 13 to 4),
                            arrayOf(10 to 10, 13 to 10, 10 to 13, 13 to 13),
                            arrayOf(1 to 10, 4 to 10, 1 to 13, 4 to 13)
                        )
                        homePositions[p][t]
                    } else if (pos < pathCells.size) {
                        pathCells[pos]
                    } else return@forEachIndexed

                    val cx = col * cs + cs / 2
                    val cy = row * cs + cs / 2
                    val radius = cs * 0.38f

                    // Highlight movable tokens
                    val isMovable = p == currentPlayer && selectedTokens.contains(t)
                    if (isMovable) {
                        paint.color = 0xFFFFFF00.toInt()
                        canvas.drawCircle(cx, cy, radius + 4f, paint)
                    }

                    paint.color = colors[p]
                    canvas.drawCircle(cx, cy, radius, paint)
                    paint.color = WHITE; paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
                    canvas.drawCircle(cx, cy, radius, paint)
                    paint.style = Paint.Style.FILL

                    textPaint.color = WHITE
                    textPaint.textSize = cs * 0.3f
                    canvas.drawText(tokenLabels[p] + (t+1), cx, cy + textPaint.textSize/3, textPaint)
                }
            }
        }

        // Returns list of (col, row) for each of the 52 board path positions
        fun getPathCells(): List<Pair<Int,Int>> {
            return listOf(
                // Bottom of left column going up (RED start)
                6 to 14, 6 to 13, 6 to 12, 6 to 11, 6 to 10, 6 to 9,
                // Left row going left
                5 to 8, 4 to 8, 3 to 8, 2 to 8, 1 to 8, 0 to 8,
                // Top of left column going up
                0 to 7, 0 to 6,
                // Top row going right
                1 to 6, 2 to 6, 3 to 6, 4 to 6, 5 to 6,
                // Up right column
                6 to 5, 6 to 4, 6 to 3, 6 to 2, 6 to 1, 6 to 0,
                // Top right
                7 to 0, 8 to 0,
                // Right top column going down
                8 to 1, 8 to 2, 8 to 3, 8 to 4, 8 to 5,
                // Right row going right
                9 to 6, 10 to 6, 11 to 6, 12 to 6, 13 to 6, 14 to 6,
                // Right side
                14 to 7, 14 to 8,
                // Right bottom going left
                13 to 8, 12 to 8, 11 to 8, 10 to 8, 9 to 8,
                // Bottom right column going down
                8 to 9, 8 to 10, 8 to 11, 8 to 12, 8 to 13, 8 to 14,
                // Bottom
                7 to 14
            )
        }

        fun getTokenAt(touchX: Float, touchY: Float): Pair<Int,Int>? {
            val cs = cellSize
            val pathCells = getPathCells()
            val homePositions = arrayOf(
                arrayOf(1 to 1, 4 to 1, 1 to 4, 4 to 4),
                arrayOf(10 to 1, 13 to 1, 10 to 4, 13 to 4),
                arrayOf(10 to 10, 13 to 10, 10 to 13, 13 to 13),
                arrayOf(1 to 10, 4 to 10, 1 to 13, 4 to 13)
            )
            tokenPositions.forEachIndexed { p, tokens ->
                if (p != currentPlayer) return@forEachIndexed
                tokens.forEachIndexed { t, pos ->
                    if (pos == 57) return@forEachIndexed
                    val (col, row) = if (pos == -1) homePositions[p][t]
                    else if (pos < pathCells.size) pathCells[pos]
                    else return@forEachIndexed
                    val cx = col * cs + cs / 2
                    val cy = row * cs + cs / 2
                    if (Math.hypot((touchX - cx).toDouble(), (touchY - cy).toDouble()) < cs * 0.5) {
                        return Pair(p, t)
                    }
                }
            }
            return null
        }
    }

    private lateinit var boardView: LudoBoardView
    private lateinit var tvStatus: TextView
    private lateinit var tvDice: TextView
    private lateinit var btnRoll: Button

    private val COLORS = arrayOf("RED","BLUE","GREEN","YELLOW")
    private val COLORS_INT = intArrayOf(0xFFE53935.toInt(),0xFF1565C0.toInt(),0xFF2E7D32.toInt(),0xFFF9A825.toInt())
    private var numPlayers = 4
    private var tokens = Array(4) { IntArray(4) { -1 } }
    private var current = 0
    private var diceValue = 0
    private var rolled = false
    private var movableTokens = mutableSetOf<Int>()

    // Each player's start position on the 52-cell path
    private val PLAYER_START = intArrayOf(0, 13, 26, 39)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "LUDO"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF080614.toInt())
        }

        // Status bar
        val statusBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 12, 16, 8)
            setBackgroundColor(0xFF0D0B20.toInt())
        }

        tvStatus = TextView(this).apply {
            textSize = 16f; typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        statusBar.addView(tvStatus)

        tvDice = TextView(this).apply {
            textSize = 42f; setTextColor(0xFF00E5FF.toInt())
            gravity = Gravity.CENTER; minWidth = 80
            typeface = Typeface.DEFAULT_BOLD
        }
        statusBar.addView(tvDice)

        root.addView(statusBar)

        // Board
        boardView = LudoBoardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    onBoardTouch(event.x, event.y)
                }
                true
            }
        }
        root.addView(boardView)

        // Controls
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(16, 12, 16, 16)
            setBackgroundColor(0xFF0D0B20.toInt())
        }

        btnRoll = Button(this).apply {
            text = "🎲  ROLL DICE"
            textSize = 16f; setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFF00E5FF.toInt())
            setPadding(32, 16, 32, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 16 }
            setOnClickListener { rollDice() }
        }
        controls.addView(btnRoll)

        val btnNew = Button(this).apply {
            text = "NEW GAME"; textSize = 14f
            setTextColor(0xFF000000.toInt()); setBackgroundColor(0xFFFFD700.toInt())
            setPadding(24, 16, 24, 16)
            setOnClickListener { resetGame() }
        }
        controls.addView(btnNew)
        root.addView(controls)

        // Instructions
        val tvInstructions = TextView(this).apply {
            text = "① Roll dice  ② Tap your token to move  ③ Need 6 to enter board  ★ = Safe square"
            textSize = 10f; setTextColor(0xFF8892B0.toInt())
            gravity = Gravity.CENTER; setPadding(16, 4, 16, 8)
            setBackgroundColor(0xFF0D0B20.toInt())
        }
        root.addView(tvInstructions)

        setContentView(root)
        resetGame()
    }

    private fun resetGame() {
        tokens = Array(4) { IntArray(4) { -1 } }
        current = 0; diceValue = 0; rolled = false; movableTokens.clear()
        boardView.tokenPositions = tokens
        boardView.currentPlayer = 0
        boardView.selectedTokens = movableTokens
        updateStatus()
        boardView.invalidate()
    }

    private fun rollDice() {
        if (rolled) return
        diceValue = (1..6).random()
        rolled = true
        tvDice.text = getDiceFace(diceValue)
        btnRoll.isEnabled = false
        btnRoll.alpha = 0.5f

        // Find movable tokens for current player
        movableTokens.clear()
        for (t in 0..3) {
            val pos = tokens[current][t]
            if (pos == 57) continue
            if (pos == -1 && diceValue == 6) movableTokens.add(t)
            if (pos >= 0 && pos + diceValue <= 57) movableTokens.add(t)
        }

        boardView.selectedTokens = movableTokens
        boardView.invalidate()

        if (movableTokens.isEmpty()) {
            tvStatus.text = "${COLORS[current]}: No moves! Passing turn..."
            tvStatus.setTextColor(0xFF8892B0.toInt())
            android.os.Handler(mainLooper).postDelayed({ nextTurn() }, 1500)
        } else {
            tvStatus.text = "${COLORS[current]}: Tap a highlighted token!"
            tvStatus.setTextColor(COLORS_INT[current])
        }
    }

    private fun onBoardTouch(x: Float, y: Float) {
        if (!rolled || movableTokens.isEmpty()) return
        val hit = boardView.getTokenAt(x, y) ?: return
        val (p, t) = hit
        if (p != current || !movableTokens.contains(t)) return

        moveToken(t)
    }

    private fun moveToken(t: Int) {
        val pos = tokens[current][t]
        val newPos = if (pos == -1) PLAYER_START[current] else pos + diceValue

        // Capture: check if any opponent token is at newPos
        for (op in 0..3) {
            if (op == current) continue
            for (ot in 0..3) {
                if (tokens[op][ot] == newPos && !SAFE_SQUARES.contains(newPos)) {
                    tokens[op][ot] = -1 // send home
                }
            }
        }

        tokens[current][t] = minOf(newPos, 57)
        boardView.tokenPositions = tokens

        // Check win
        if (tokens[current].all { it == 57 }) {
            tvStatus.text = "🏆 ${COLORS[current]} WINS! 🎉"
            tvStatus.setTextColor(COLORS_INT[current])
            tvDice.text = "🏆"
            boardView.invalidate()
            return
        }

        if (diceValue == 6) {
            // Roll again
            rolled = false; movableTokens.clear()
            boardView.selectedTokens = movableTokens
            tvStatus.text = "${COLORS[current]}: Got 6! Roll again!"
            tvStatus.setTextColor(COLORS_INT[current])
            btnRoll.isEnabled = true; btnRoll.alpha = 1f
            tvDice.text = ""
        } else {
            nextTurn()
        }
        boardView.invalidate()
    }

    private fun nextTurn() {
        current = (current + 1) % numPlayers
        rolled = false; movableTokens.clear()
        boardView.currentPlayer = current
        boardView.selectedTokens = movableTokens
        btnRoll.isEnabled = true; btnRoll.alpha = 1f
        tvDice.text = ""
        updateStatus()
        boardView.invalidate()
    }

    private fun updateStatus() {
        tvStatus.text = "${COLORS[current]}'S TURN — Roll the dice!"
        tvStatus.setTextColor(COLORS_INT[current])
    }

    private fun getDiceFace(n: Int) = when(n) {
        1 -> "⚀"; 2 -> "⚁"; 3 -> "⚂"; 4 -> "⚃"; 5 -> "⚄"; else -> "⚅"
    }

    private val SAFE_SQUARES = setOf(0, 8, 13, 21, 26, 34, 39, 47)
}
