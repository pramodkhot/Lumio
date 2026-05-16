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
import kotlin.math.*

class CrazyKnifeActivity : BaseGameActivity() {

    private val STAGE_KNIVES = intArrayOf(5, 8, 10, 13, 15)
    private val STAGE_SPEED  = longArrayOf(3000, 2400, 1900, 1500, 1200)
    private val KNIFE_GAP_DEG = 22f

    private var stage = 0
    private var knivesThrown = 0
    private var score = 0
    private var gameOver = false
    private var rotation = 0f
    private val knivesOnTarget = mutableListOf<Float>() // angles of placed knives

    private lateinit var gameView: GameView
    private lateinit var tvStage: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvKnivesLeft: TextView
    private lateinit var overlayLayout: FrameLayout
    private lateinit var tvOverlayTitle: TextView
    private lateinit var tvOverlayMsg: TextView

    private var rotationAnimator: ValueAnimator? = null

    inner class GameView(ctx: android.content.Context) : View(ctx) {
        private val bgPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
        private val boardPaint= Paint(Paint.ANTI_ALIAS_FLAG)
        private val innerPaint= Paint(Paint.ANTI_ALIAS_FLAG)
        private val knifePaint= Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
        }
        private val handlePaint= Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
        }
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }

        var cx = 0f; var cy = 0f; var radius = 0f

        override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
            cx = w / 2f; cy = h * 0.42f
            radius = minOf(w, h) * 0.28f
        }

        override fun onDraw(canvas: Canvas) {
            // Background
            bgPaint.color = 0xFF080614.toInt()
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            canvas.save()
            canvas.rotate(rotation, cx, cy)

            // Outer ring glow
            glowPaint.color = 0xFF4A3000.toInt()
            glowPaint.strokeWidth = 12f
            canvas.drawCircle(cx, cy, radius + 6f, glowPaint)

            // Board
            boardPaint.color = 0xFF2A1A00.toInt()
            canvas.drawCircle(cx, cy, radius, boardPaint)

            // Ring border
            boardPaint.style = Paint.Style.STROKE
            boardPaint.strokeWidth = 6f
            boardPaint.color = 0xFFFFD700.toInt()
            canvas.drawCircle(cx, cy, radius, boardPaint)
            boardPaint.style = Paint.Style.FILL

            // Inner rings
            innerPaint.style = Paint.Style.STROKE
            innerPaint.strokeWidth = 2f
            for (i in 1..3) {
                innerPaint.color = if (i == 3) 0xFFFF4081.toInt() else 0xFF3A2A00.toInt()
                canvas.drawCircle(cx, cy, radius * (i / 3.5f), innerPaint)
            }

            // Bullseye
            innerPaint.style = Paint.Style.FILL
            innerPaint.color = 0xFFFF4081.toInt()
            canvas.drawCircle(cx, cy, radius * 0.12f, innerPaint)
            innerPaint.color = 0xFFFFFFFF.toInt()
            canvas.drawCircle(cx, cy, radius * 0.05f, innerPaint)

            // Stage text on board
            textPaint.color = 0x44FFFFFF.toInt()
            textPaint.textSize = radius * 0.22f
            canvas.drawText("S${stage+1}", cx, cy + radius * 0.5f, textPaint)

            // Knives ON target (rotate with board)
            knivesOnTarget.forEachIndexed { i, angle ->
                drawKnife(canvas, cx, cy, radius, angle,
                    if (i == knivesOnTarget.lastIndex) 0xFFFFD700.toInt() else 0xFFCCCCCC.toInt())
            }

            canvas.restore()

            // Throwing knife at bottom (fixed, not rotating)
            if (!gameOver) {
                drawThrowingKnife(canvas)
            }
        }

        private fun drawKnife(canvas: Canvas, cx: Float, cy: Float, r: Float, angleDeg: Float, color: Int) {
            val rad = Math.toRadians(angleDeg.toDouble())
            val tipX = cx + (r - 8f) * sin(rad).toFloat()
            val tipY = cy - (r - 8f) * cos(rad).toFloat()
            val baseX = cx + (r + r * 0.45f) * sin(rad).toFloat()
            val baseY = cy - (r + r * 0.45f) * cos(rad).toFloat()
            val handleX = cx + (r + r * 0.7f) * sin(rad).toFloat()
            val handleY = cy - (r + r * 0.7f) * cos(rad).toFloat()

            knifePaint.color = color
            knifePaint.strokeWidth = 4f
            canvas.drawLine(tipX, tipY, baseX, baseY, knifePaint)

            handlePaint.color = 0xFF8B4513.toInt()
            handlePaint.strokeWidth = 7f
            canvas.drawLine(baseX, baseY, handleX, handleY, handlePaint)
        }

        private fun drawThrowingKnife(canvas: Canvas) {
            val kx = cx; val ky = height * 0.88f
            knifePaint.color = 0xFF00E5FF.toInt(); knifePaint.strokeWidth = 5f
            canvas.drawLine(kx, ky - 60f, kx, ky + 20f, knifePaint)
            handlePaint.color = 0xFF8B4513.toInt(); handlePaint.strokeWidth = 9f
            canvas.drawLine(kx, ky + 20f, kx, ky + 50f, handlePaint)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "CRAZY KNIFE"

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
            setPadding(16, 12, 16, 12)
        }

        tvStage = TextView(this).apply {
            textSize = 18f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFD700.toInt()); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvScore = TextView(this).apply {
            textSize = 18f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF00E5FF.toInt()); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvKnivesLeft = TextView(this).apply {
            textSize = 14f; setTextColor(0xFF8892B0.toInt()); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(tvStage); header.addView(tvScore); header.addView(tvKnivesLeft)
        main.addView(header)

        // ── Status ────────────────────────────────────────────────────────────
        tvStatus = TextView(this).apply {
            textSize = 14f; setTextColor(0xFFFFD700.toInt()); gravity = Gravity.CENTER
            setPadding(16, 10, 16, 10)
            minHeight = 44.dp
            setBackgroundColor(0xFF050410.toInt())
        }
        main.addView(tvStatus)

        // ── Game view ─────────────────────────────────────────────────────────
        gameView = GameView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            setOnClickListener { throwKnife() }
        }
        main.addView(gameView)

        // ── Throw button ──────────────────────────────────────────────────────
        val btnThrow = Button(this).apply {
            text = "🔪  TAP TO THROW"; textSize = 16f
            setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFFFF4081.toInt())
            setPadding(0, 18, 0, 18)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(16, 0, 16, 8) }
            setOnClickListener { throwKnife() }
        }
        main.addView(btnThrow)

        // ── Help text ─────────────────────────────────────────────────────────
        main.addView(buildHelpBar("Tap anywhere or the button to throw  •  Don't hit existing knives  •  Fill each stage to advance  •  5 stages to win"))

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
        tvOverlayTitle = TextView(this).apply {
            textSize = 48f; gravity = Gravity.CENTER; setPadding(0,0,0,8)
        }
        tvOverlayMsg = TextView(this).apply {
            textSize = 20f; setTextColor(0xFFFFD700.toInt())
            gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
            setPadding(0,8,0,32)
        }
        val btnRestart = Button(this).apply {
            text = "▶  PLAY AGAIN"; textSize = 16f
            setTextColor(0xFF000000.toInt()); setBackgroundColor(0xFF00E5FF.toInt())
            setPadding(48, 16, 48, 16)
            setOnClickListener { hideOverlay(); startGame() }
        }
        overlayContent.addView(tvOverlayTitle)
        overlayContent.addView(tvOverlayMsg)
        overlayContent.addView(btnRestart)
        overlayLayout.addView(overlayContent)
        root.addView(overlayLayout)

        setContentView(root)
        startGame()
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()

    private fun startGame() {
        stage = 0; score = 0; gameOver = false
        knivesThrown = 0; knivesOnTarget.clear()
        startRotation(); updateHeader()
        tvStatus.text = "TAP TO THROW — DON'T HIT EXISTING KNIVES!"
        tvStatus.setTextColor(0xFFFFD700.toInt())
        gameView.invalidate()
    }

    private fun startRotation() {
        rotationAnimator?.cancel()
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = STAGE_SPEED[stage]
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener {
                rotation = it.animatedValue as Float
                gameView.rotation = 0f
                gameView.invalidate()
            }
            start()
        }
    }

    private fun throwKnife() {
        if (gameOver) return
        val currentAngle = rotation % 360

        // Check collision with existing knives
        val hit = knivesOnTarget.any { existingAngle ->
            val diff = abs(((currentAngle - existingAngle + 360) % 360).let {
                minOf(it, 360 - it)
            })
            diff < KNIFE_GAP_DEG
        }

        if (hit) {
            // CRASH!
            crashAnimation()
            return
        }

        // Place knife
        knivesOnTarget.add(currentAngle)
        knivesThrown++
        score += 10
        updateHeader()
        gameView.invalidate()

        // Pulse animation
        ValueAnimator.ofFloat(0.9f, 1.05f, 1f).apply {
            duration = 200
            addUpdateListener { gameView.scaleX = it.animatedValue as Float; gameView.scaleY = it.animatedValue as Float }
            start()
        }

        tvStatus.text = "✅ KNIFE ${knivesThrown}/${STAGE_KNIVES[stage]} — KEEP GOING!"
        tvStatus.setTextColor(0xFF00E676.toInt())

        // Check stage complete
        if (knivesThrown >= STAGE_KNIVES[stage]) {
            stageComplete()
        }
    }

    private fun stageComplete() {
        rotationAnimator?.cancel()
        score += 50 // stage bonus
        tvStatus.text = "🎉 STAGE ${stage+1} COMPLETE! +50 BONUS"
        tvStatus.setTextColor(0xFFFFD700.toInt())
        updateHeader()

        if (stage >= 4) {
            // Won all stages!
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                gameOver = true
                AdManager.onGameCompleted()
                tvOverlayTitle.text = "🏆"
                tvOverlayMsg.text = "YOU WIN!\nAll 5 stages complete!\nScore: $score"
                overlayLayout.visibility = View.VISIBLE
                overlayLayout.alpha = 0f
                overlayLayout.animate().alpha(1f).setDuration(400).start()
            }, 1000)
        } else {
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                stage++
                knivesThrown = 0; knivesOnTarget.clear()
                startRotation(); updateHeader()
                tvStatus.text = "STAGE ${stage+1} — FASTER! Throw ${STAGE_KNIVES[stage]} knives"
                tvStatus.setTextColor(0xFFFF9800.toInt())
            }, 1200)
        }
    }

    private fun crashAnimation() {
        rotationAnimator?.cancel()
        gameOver = true
        // Screen shake
        val shake = ObjectAnimator.ofFloat(gameView, "translationX", 0f, -20f, 20f, -15f, 15f, -8f, 8f, 0f).apply {
            duration = 400
        }
        shake.start()

        tvStatus.text = "💥 CRASHED! Knife hit another knife"
        tvStatus.setTextColor(0xFFFF5252.toInt())

        android.os.Handler(Looper.getMainLooper()).postDelayed({
            AdManager.onGameCompleted()
            tvOverlayTitle.text = "💥"
            tvOverlayMsg.text = "KNIFE HIT!\nStage ${stage+1} — Score: $score"
            tvOverlayTitle.setTextColor(0xFFFF5252.toInt())
            overlayLayout.visibility = View.VISIBLE
            overlayLayout.alpha = 0f
            overlayLayout.animate().alpha(1f).setDuration(400).start()
        }, 800)
    }

    private fun updateHeader() {
        tvStage.text = "STAGE ${stage+1}/5"
        tvScore.text = "⭐ $score"
        val left = STAGE_KNIVES[stage] - knivesThrown
        tvKnivesLeft.text = "🔪 $left left"
    }

    private fun hideOverlay() {
        rotationAnimator?.cancel()
        overlayLayout.animate().alpha(0f).setDuration(250).withEndAction {
            overlayLayout.visibility = View.GONE
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        rotationAnimator?.cancel()
    }

    private fun buildHelpBar(text: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF050410.toInt())
            setPadding(16, 8, 16, 12)
            addView(View(this@CrazyKnifeActivity).apply {
                setBackgroundColor(0xFF1A2A6E.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply { bottomMargin = 8 }
            })
            addView(TextView(this@CrazyKnifeActivity).apply {
                this.text = text; textSize = 11f
                setTextColor(0xFF8892B0.toInt())
                gravity = Gravity.CENTER; setLineSpacing(0f, 1.4f)
            })
        }
    }
}
