package com.lumio.gamezone.games

import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.*
import com.lumio.gamezone.ui.BaseGameActivity

class WaterSortActivity : BaseGameActivity() {

    private val CAPACITY = 4
    private val COLORS = intArrayOf(
        0xFFFF5252.toInt(), 0xFF448AFF.toInt(), 0xFF00E676.toInt(), 0xFFFFD700.toInt(),
        0xFFFF4081.toInt(), 0xFF00E5FF.toInt(), 0xFF9C27B0.toInt(), 0xFFFF9800.toInt()
    )
    private var tubes = mutableListOf<MutableList<Int>>()
    private var selected = -1
    private var level = 1
    private lateinit var tubeViews: MutableList<TubeView>
    private lateinit var tvStatus: TextView

    inner class TubeView(context: android.content.Context) : View(context) {
        var tube: MutableList<Int> = mutableListOf()
        var isSelected = false
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            val segH = (h - 20) / CAPACITY
            // Border
            borderPaint.color = if (isSelected) 0xFFFFD700.toInt() else 0x8800E5FF.toInt()
            val rect = RectF(4f, 4f, w - 4f, h - 4f)
            canvas.drawRoundRect(rect, w / 2, 20f, borderPaint)
            // Fill segments from bottom
            tube.forEachIndexed { i, color ->
                paint.color = color
                val top = h - 4f - (i + 1) * segH
                val segRect = RectF(6f, top, w - 6f, h - 4f - i * segH)
                if (i == 0) {
                    val radii = floatArrayOf(0f,0f,0f,0f, w/2-6f,w/2-6f, w/2-6f,w/2-6f)
                    val path = Path().apply { addRoundRect(segRect, radii, Path.Direction.CW) }
                    canvas.drawPath(path, paint)
                } else canvas.drawRect(segRect, paint)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "WATER SORT"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF080614.toInt())
            setPadding(16, 16, 16, 16)
        }

        tvStatus = TextView(this).apply {
            textSize = 14f; setTextColor(0xFF00E5FF.toInt())
            gravity = Gravity.CENTER; setPadding(0, 0, 0, 16)
        }
        root.addView(tvStatus)

        val tubesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        tubeViews = mutableListOf()

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER; setPadding(0, 16, 0, 0)
        }
        val btnRestart = Button(this).apply {
            text = "RESTART"; setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFF00E5FF.toInt())
            setOnClickListener { startLevel() }
        }
        btnRow.addView(btnRestart)

        root.addView(tubesContainer)
        root.addView(btnRow)
        setContentView(root)

        startLevel()

        // Build tube views after layout
        root.post {
            tubesContainer.removeAllViews()
            tubeViews.clear()
            val tubeW = 52
            val tubeH = (resources.displayMetrics.heightPixels * 0.45).toInt()
            tubes.forEachIndexed { i, _ ->
                val tv = TubeView(this).apply {
                    tube = tubes[i]
                    layoutParams = LinearLayout.LayoutParams(tubeW, tubeH).apply { setMargins(6,0,6,0) }
                    setOnClickListener { onTubeClick(i) }
                }
                tubeViews.add(tv)
                tubesContainer.addView(tv)
            }
        }
    }

    private fun startLevel() {
        val numColors = minOf(4 + level - 1, 7)
        val all = mutableListOf<Int>()
        (0 until numColors).forEach { c -> repeat(CAPACITY) { all.add(COLORS[c]) } }
        all.shuffle()
        tubes = (0 until numColors).map { i -> all.subList(i * CAPACITY, (i+1) * CAPACITY).toMutableList() }.toMutableList()
        tubes.add(mutableListOf()); tubes.add(mutableListOf())
        selected = -1
        tvStatus.text = "Level $level — pour to sort!"
        tubeViews.forEachIndexed { i, tv -> if (i < tubes.size) { tv.tube = tubes[i]; tv.isSelected = false; tv.invalidate() } }
    }

    private fun onTubeClick(idx: Int) {
        if (selected == -1) {
            if (tubes[idx].isEmpty()) return
            selected = idx
            tubeViews[idx].isSelected = true; tubeViews[idx].invalidate()
        } else if (selected == idx) {
            tubeViews[idx].isSelected = false; tubeViews[idx].invalidate(); selected = -1
        } else {
            if (canPour(tubes[selected], tubes[idx])) {
                pour(selected, idx)
                tubeViews[selected].isSelected = false
                tubeViews[selected].invalidate(); tubeViews[idx].invalidate()
                selected = -1
                if (isSolved()) {
                    tvStatus.text = "✅ LEVEL $level SOLVED!"
                    level++
                    android.os.Handler(mainLooper).postDelayed({ startLevel() }, 1500)
                }
            } else {
                tubeViews[selected].isSelected = false; tubeViews[selected].invalidate()
                selected = idx
                tubeViews[idx].isSelected = true; tubeViews[idx].invalidate()
            }
        }
    }

    private fun canPour(from: MutableList<Int>, to: MutableList<Int>): Boolean {
        if (from.isEmpty()) return false
        if (to.size >= CAPACITY) return false
        if (to.isEmpty()) return true
        return from.last() == to.last()
    }

    private fun pour(fi: Int, ti: Int) {
        val color = tubes[fi].last()
        while (tubes[fi].isNotEmpty() && tubes[fi].last() == color && tubes[ti].size < CAPACITY) {
            tubes[ti].add(tubes[fi].removeLast())
        }
    }

    private fun isSolved() = tubes.all { t -> t.isEmpty() || (t.size == CAPACITY && t.all { it == t[0] }) }
}
