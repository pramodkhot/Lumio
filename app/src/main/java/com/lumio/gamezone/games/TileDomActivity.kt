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

class TileDomActivity : BaseGameActivity() {

    private val SYMBOLS = listOf("🌸","🎯","⭐","🔥","💎","🎮","🚀","🌊","🎲","🏆","⚡","🎸")
    private val COLS = 4; private val ROWS = 6

    data class Tile(val id: Int, val symbol: String, var faceUp: Boolean = false, var matched: Boolean = false)

    private var tiles = mutableListOf<Tile>()
    private var firstSel: Int = -1
    private var secondSel: Int = -1
    private var moves = 0
    private var matchCount = 0
    private var canFlip = true

    private lateinit var tileViews: Array<TextView>
    private lateinit var tvMoves: TextView
    private lateinit var tvMatches: TextView
    private lateinit var tvStatus: TextView
    private lateinit var overlayLayout: FrameLayout
    private lateinit var tvOverlayMsg: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "TILE DOM"

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
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 14, 16, 14)
        }

        fun statCol(label: String, isBlue: Boolean): Pair<LinearLayout, TextView> {
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tv = TextView(this).apply {
                text = "0"; textSize = 24f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isBlue) 0xFF00E5FF.toInt() else 0xFF00E676.toInt())
                gravity = Gravity.CENTER
            }
            val lbl = TextView(this).apply {
                text = label; textSize = 9f; setTextColor(0xFF8892B0.toInt())
                gravity = Gravity.CENTER
            }
            col.addView(tv); col.addView(lbl)
            return Pair(col, tv)
        }

        val (movesCol, movesTV) = statCol("MOVES", true)
        val (matchCol, matchTV) = statCol("MATCHED", false)
        tvMoves = movesTV; tvMatches = matchTV

        tvStatus = TextView(this).apply {
            textSize = 13f; setTextColor(0xFFFFD700.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            minHeight = 44.dp
        }

        val btnNew = Button(this).apply {
            text = "NEW"; textSize = 11f
            setTextColor(0xFF000000.toInt()); setBackgroundColor(0xFF00E5FF.toInt())
            setPadding(16, 8, 16, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { startNewGame() }
        }

        header.addView(movesCol); header.addView(tvStatus)
        header.addView(matchCol); header.addView(btnNew)
        main.addView(header)

        // ── Tile grid ─────────────────────────────────────────────────────────
        val screenW = resources.displayMetrics.widthPixels
        val tileSize = (screenW - (COLS + 1) * 8) / COLS
        val grid = GridLayout(this).apply {
            rowCount = ROWS; columnCount = COLS
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        tileViews = Array(ROWS * COLS) { idx ->
            TextView(this).apply {
                textSize = 28f; gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = tileSize; height = tileSize
                    setMargins(4, 4, 4, 4)
                }
                setBackgroundColor(0xFF0F1435.toInt())
                setOnClickListener { onTileTap(idx) }
                grid.addView(this)
            }
        }
        main.addView(grid)

        // ── Help text ─────────────────────────────────────────────────────────
        main.addView(buildHelpBar("Tap tiles to flip  •  Find matching pairs  •  Remember positions  •  Match all 12 pairs to win"))

        root.addView(main)

        // ── Overlay ───────────────────────────────────────────────────────────
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
        val tvOverlayIcon = TextView(this).apply { text = "🏆"; textSize = 72f; gravity = Gravity.CENTER }
        tvOverlayMsg = TextView(this).apply {
            textSize = 22f; setTextColor(0xFFFFD700.toInt())
            gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 8, 0, 32)
        }
        val btnRestart = Button(this).apply {
            text = "▶  PLAY AGAIN"; textSize = 16f
            setTextColor(0xFF000000.toInt()); setBackgroundColor(0xFF00E5FF.toInt())
            setPadding(48, 16, 48, 16)
            setOnClickListener { hideOverlay(); startNewGame() }
        }
        overlayContent.addView(tvOverlayIcon)
        overlayContent.addView(tvOverlayMsg)
        overlayContent.addView(btnRestart)
        overlayLayout.addView(overlayContent)
        root.addView(overlayLayout)

        setContentView(root)
        startNewGame()
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()

    private fun startNewGame() {
        tiles.clear()
        val pairs = (SYMBOLS + SYMBOLS).shuffled()
        pairs.forEachIndexed { i, sym -> tiles.add(Tile(i, sym)) }
        firstSel = -1; secondSel = -1; moves = 0; matchCount = 0; canFlip = true
        updateStats()
        renderTiles()
        tvStatus.text = "FIND THE MATCHING PAIRS!"
        tvStatus.setTextColor(0xFFFFD700.toInt())
        hideOverlay()
    }

    private fun onTileTap(idx: Int) {
        if (!canFlip) return
        val tile = tiles[idx]
        if (tile.matched || tile.faceUp) return
        if (firstSel == idx) return

        // Flip animation
        flipTile(idx, true) {
            tile.faceUp = true
            renderTile(idx)

            if (firstSel == -1) {
                firstSel = idx
                tvStatus.text = "GOOD! NOW FIND ITS MATCH"
                tvStatus.setTextColor(0xFF00E5FF.toInt())
            } else {
                secondSel = idx
                moves++
                updateStats()
                canFlip = false
                checkMatch()
            }
        }
    }

    private fun checkMatch() {
        val t1 = tiles[firstSel]; val t2 = tiles[secondSel]
        if (t1.symbol == t2.symbol) {
            // Match!
            Handler(Looper.getMainLooper()).postDelayed({
                t1.matched = true; t2.matched = true
                matchCount++
                // Flash match green
                tileViews[firstSel].setBackgroundColor(0xFF003300.toInt())
                tileViews[secondSel].setBackgroundColor(0xFF003300.toInt())
                tvStatus.text = "✅ MATCH! +1 — $matchCount/12 pairs found"
                tvStatus.setTextColor(0xFF00E676.toInt())
                updateStats()
                firstSel = -1; secondSel = -1; canFlip = true
                renderTiles()
                if (matchCount == 12) {
                    Handler(Looper.getMainLooper()).postDelayed({ showWin() }, 600)
                }
            }, 400)
        } else {
            // No match — flip back
            tvStatus.text = "❌ NOT A MATCH — TRY AGAIN"
            tvStatus.setTextColor(0xFFFF5252.toInt())
            Handler(Looper.getMainLooper()).postDelayed({
                t1.faceUp = false; t2.faceUp = false
                flipTile(firstSel, false) { renderTile(firstSel) }
                flipTile(secondSel, false) { renderTile(secondSel) }
                firstSel = -1; secondSel = -1; canFlip = true
                tvStatus.text = "KEEP TRYING — FIND THE PAIRS"
                tvStatus.setTextColor(0xFFFFD700.toInt())
            }, 1000)
        }
    }

    private fun flipTile(idx: Int, faceUp: Boolean, onEnd: () -> Unit) {
        val view = tileViews[idx]
        ObjectAnimator.ofFloat(view, "scaleX", 1f, 0f).apply {
            duration = 150
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) {
                    onEnd()
                    ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f).apply {
                        duration = 150; start()
                    }
                }
            })
            start()
        }
    }

    private fun renderTiles() {
        tiles.forEachIndexed { i, _ -> renderTile(i) }
    }

    private fun renderTile(idx: Int) {
        val tile = tiles[idx]
        val view = tileViews[idx]
        when {
            tile.matched -> {
                view.text = tile.symbol
                view.setBackgroundColor(0xFF003300.toInt())
                view.setTextColor(0xFF00E676.toInt())
                view.alpha = 0.6f
            }
            tile.faceUp -> {
                view.text = tile.symbol
                view.setBackgroundColor(0xFF001A3A.toInt())
                view.setTextColor(0xFFFFFFFF.toInt())
                view.alpha = 1f
                // Highlight selected
                if (idx == firstSel) {
                    view.setBackgroundColor(0xFF1A2A6E.toInt())
                }
            }
            else -> {
                view.text = "?"
                view.setBackgroundColor(0xFF0F1435.toInt())
                view.setTextColor(0xFF3A5A9E.toInt())
                view.alpha = 1f
            }
        }
    }

    private fun updateStats() {
        tvMoves.text = moves.toString()
        tvMatches.text = "$matchCount/12"
    }

    private fun showWin() {
        AdManager.onGameCompleted()
        tvOverlayMsg.text = "ALL PAIRS MATCHED! 🎉\nCompleted in $moves moves"
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
            addView(View(this@TileDomActivity).apply {
                setBackgroundColor(0xFF1A2A6E.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply { bottomMargin = 8 }
            })
            addView(TextView(this@TileDomActivity).apply {
                this.text = text; textSize = 11f
                setTextColor(0xFF8892B0.toInt())
                gravity = Gravity.CENTER; setLineSpacing(0f, 1.4f)
            })
        }
    }
}
