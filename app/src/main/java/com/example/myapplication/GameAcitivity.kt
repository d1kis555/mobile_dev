package com.example.myapplication

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.random.Random
import android.animation.Animator
import android.animation.AnimatorListenerAdapter

class GameActivity : ComponentActivity() {

    private lateinit var tvScore: TextView
    private lateinit var tvTimer: TextView
    private lateinit var gameField: FrameLayout
    private var score = 0
    private var roundRemainingMs: Long = 0

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var spawnRunnable: Runnable
    private lateinit var timerRunnable: Runnable

    private var gameSpeed: Float = 3.0f
    private var maxBugs: Int = 10
    private var spawnIntervalMs: Long = 1200L
    private var roundDurationMs: Long = 60000L
    private val bugSizeDp: Int = 64

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        tvScore = findViewById(R.id.tvScore)
        tvTimer = findViewById(R.id.tvTimer)
        gameField = findViewById(R.id.gameField)

        val btnMenu: Button = findViewById(R.id.btnMenu)
        btnMenu.setOnClickListener { finish() }

        loadSettings()
        roundRemainingMs = roundDurationMs

        gameField.post { startSpawning() }
        startTimer()

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

    private fun loadSettings() {
        val p = getSharedPreferences("game_prefs", MODE_PRIVATE)
        gameSpeed = p.getInt("speed", 5).toFloat()
        maxBugs = p.getInt("max_bugs", 10)
        spawnIntervalMs = (p.getInt("bonus_interval", 15) * 100L).coerceAtLeast(350L)
        roundDurationMs = (p.getInt("round_duration", 120) * 1000L)
    }

    private fun startSpawning() {
        spawnRunnable = object : Runnable {
            override fun run() {
                val toSpawn = (maxBugs - gameField.childCount).coerceAtLeast(0)
                repeat(toSpawn) { spawnBug() }
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
        val distance = sqrt(dx * dx + dy * dy)
        val speed = 50f * gameSpeed
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

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (roundRemainingMs <= 0) {
                    endGame()
                    return
                }
                val seconds = (roundRemainingMs / 1000) % 60
                val minutes = (roundRemainingMs / 1000) / 60
                tvTimer.text = String.format("%02d:%02d", minutes, seconds)
                roundRemainingMs -= 1000
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable)
    }

    private fun endGame() {
        handler.removeCallbacks(spawnRunnable)
        handler.removeCallbacks(timerRunnable)
        roundRemainingMs = 0

        val childCount = gameField.childCount
        for (i in childCount - 1 downTo 0) {
            val child = gameField.getChildAt(i)
            child.animate().cancel()
            gameField.removeView(child)
        }

        gameField.setOnTouchListener(null)

        tvTimer.text = "00:00"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Игра окончена!")
            .setMessage("Ваш счёт: $score")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }


    override fun onDestroy() {
        handler.removeCallbacks(spawnRunnable)
        handler.removeCallbacks(timerRunnable)
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
