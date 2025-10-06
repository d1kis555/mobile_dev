package com.example.myapplication

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import kotlin.math.atan2
import kotlin.random.Random
import android.animation.Animator
import android.animation.AnimatorListenerAdapter

class GameActivity : ComponentActivity() {

    private lateinit var tvScore: TextView
    private lateinit var gameField: FrameLayout
    private var score = 0

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var spawnRunnable: Runnable

    // ================= Настройки игры =================
    private var gameSpeed: Float = 3.0f
    private var maxBugs: Int = 10
    private var spawnIntervalMs: Long = 1200L
    private var roundDurationMs: Long = 60000L
    private val bugSizeDp: Int = 64
    // ==================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        tvScore = findViewById(R.id.tvScore)
        gameField = findViewById(R.id.gameField)

        val difficulty = intent.getIntExtra("EXTRA_DIFFICULTY", 1)
        gameSpeed = 1f + (difficulty - 1) * 0.2f
        spawnIntervalMs = (1600L - (difficulty - 1) * 120L).coerceAtLeast(350L)

        gameField.post { startSpawning() }

        gameField.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val tappedView = gameField.findViewAt(event.x, event.y)
                if (tappedView == null) {
                    score -= 5
                    tvScore.text = "Очки: $score"
                }
            }
            false
        }
    }

    private fun startSpawning() {
        spawnRunnable = object : Runnable {
            override fun run() {
                val toSpawn = maxBugs - gameField.childCount
                repeat(toSpawn.coerceAtLeast(1)) { spawnBug() }
                handler.postDelayed(this, (spawnIntervalMs / gameSpeed).toLong())
            }
        }
        handler.post(spawnRunnable)
    }

    private fun spawnBug() {
        val sizePx = (bugSizeDp * resources.displayMetrics.density).toInt()
        val fieldW = gameField.width
        val fieldH = gameField.height
        if (fieldW <= sizePx || fieldH <= sizePx) return

        val bug = ImageView(this)
        val bugDrawable = resources.getIdentifier("juk", "drawable", packageName)
        if (bugDrawable != 0) bug.setImageResource(bugDrawable)
        else bug.setImageResource(R.drawable.ic_launcher_foreground)

        val startX = Random.nextInt(0, fieldW - sizePx).toFloat()
        val startY = Random.nextInt(0, fieldH - sizePx).toFloat()
        bug.x = startX
        bug.y = startY

        val params = FrameLayout.LayoutParams(sizePx, sizePx)
        bug.layoutParams = params

        bug.setOnClickListener {
            it.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(200).withEndAction {
                if (it.parent != null) (it.parent as ViewGroup).removeView(it)
            }
            score += 10
            tvScore.text = "Очки: $score"
        }

        gameField.addView(bug)
        moveBugSmooth(bug, fieldW, fieldH, sizePx)
    }

    private fun moveBugSmooth(bug: ImageView, fieldW: Int, fieldH: Int, sizePx: Int) {
        val endX = Random.nextInt(0, fieldW - sizePx).toFloat()
        val endY = Random.nextInt(0, fieldH - sizePx).toFloat()

        val startX = bug.x
        val startY = bug.y

        val dx = endX - startX
        val dy = endY - startY
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        val speed = 200f / gameSpeed
        val duration = (distance / speed * 1000).toLong()

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = duration
        animator.addUpdateListener { valueAnimator ->
            val fraction = valueAnimator.animatedValue as Float

            val prevX = bug.x
            val prevY = bug.y

            val newX = startX + dx * fraction
            val newY = startY + dy * fraction
            bug.x = newX
            bug.y = newY

            val vx = newX - prevX
            val vy = newY - prevY
            if (vx != 0f || vy != 0f) {
                val angle = Math.toDegrees(atan2(vy.toDouble(), vx.toDouble())).toFloat() + 90f
                bug.rotation = angle
            }
        }

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (bug.parent != null) moveBugSmooth(bug, fieldW, fieldH, sizePx)
            }
        })

        animator.start()
    }

    override fun onDestroy() {
        handler.removeCallbacks(spawnRunnable)
        super.onDestroy()
    }
}

private fun ViewGroup.findViewAt(x: Float, y: Float): ImageView? {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child is ImageView) {
            val left = child.x
            val top = child.y
            val right = left + child.width
            val bottom = top + child.height
            if (x in left..right && y in top..bottom) return child
        }
    }
    return null
}
