package com.example.myapplication

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    private val gameViewModel: GameViewModel by viewModel()

    private lateinit var tvScore: TextView
    private lateinit var tvTimer: TextView
    private lateinit var gameField: FrameLayout
    private lateinit var bonusView: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private val bugAnimators = mutableMapOf<ImageView, ValueAnimator>()

    private var tiltActive = false
    private var tiltX = 0f
    private var tiltY = 0f
    private lateinit var sensorManager: SensorManager
    private var accel: Sensor? = null
    private val tiltHandler = Handler(Looper.getMainLooper())
    private lateinit var tiltRunnable: Runnable

    private val bonusHandler = Handler(Looper.getMainLooper())
    private lateinit var bonusLoopRunnable: Runnable
    private val bonusLifetimeMs = 10_000L
    private val tiltDurationMs = 5_000L
    private var roundRemainingMs: Long = 60_000L
    private lateinit var spawnRunnable: Runnable
    private lateinit var timerRunnable: Runnable

    private val bugSizeDp = 64

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        tvScore = findViewById(R.id.tvScore)
        tvTimer = findViewById(R.id.tvTimer)
        gameField = findViewById(R.id.gameField)
        bonusView = findViewById(R.id.bonusView)
        val btnMenu: Button = findViewById(R.id.btnMenu)
        btnMenu.setOnClickListener { finish() }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Загружаем настройки из SharedPreferences
        val prefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        gameViewModel.gameSpeed = prefs.getInt("speed", 5).toFloat()
        gameViewModel.maxBugs = prefs.getInt("max_bugs", 10)
        gameViewModel.bonusIntervalSec = prefs.getInt("bonus_interval", 15)
        gameViewModel.roundDurationSec = prefs.getInt("round_duration", 60)
        roundRemainingMs = gameViewModel.roundDurationSec * 1000L

        // LiveData наблюдатели
        gameViewModel.score.observe(this) { tvScore.text = "Очки: $it" }
        gameViewModel.timeLeft.observe(this) {
            tvTimer.text = String.format("%02d:%02d", it / 60, it % 60)
            if (it <= 0) endGame()
        }
        gameViewModel.bugs.observe(this) { drawBugs() }

        // Жуки спавнятся после измерения поля
        gameField.post {
            repeat(gameViewModel.maxBugs) { spawnRandomBug() }
            startTimer()
            startSpawning()
            startBonusLoop()
        }
    }

    private fun spawnRandomBug() {
        val isGolden = Random.nextInt(100) < 10 // 10% шанс золотого жука
        gameViewModel.spawnBug(isGolden)
    }

    private fun drawBugs() {
        val existingViews = gameField.childrenMap()
        gameViewModel.bugs.value?.forEach { bug ->
            if (!existingViews.containsKey(bug.id)) {
                spawnBugView(bug)
            }
        }
    }

    private fun spawnBugView(bug: BugState) {
        val sizePx = (bugSizeDp * resources.displayMetrics.density).toInt()
        val img = ImageView(this)

        val resId = if (bug.isGolden) {
            resources.getIdentifier("juk_gold", "drawable", packageName).takeIf { it != 0 } ?: R.drawable.juk
        } else R.drawable.juk
        img.setImageResource(resId)

        val fieldW = gameField.width
        val fieldH = gameField.height
        if (fieldW <= sizePx || fieldH <= sizePx) return

        img.x = Random.nextInt(0, fieldW - sizePx).toFloat()
        img.y = Random.nextInt(0, fieldH - sizePx).toFloat()
        img.layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
        img.setOnClickListener {
            gameViewModel.addScore(if (bug.isGolden) 50 else 10)
            gameViewModel.removeBug(bug.id)
            img.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(200)
                .withEndAction { gameField.removeView(img) }
        }

        gameField.addView(img)
        moveBugSmooth(img, fieldW, fieldH, sizePx)
    }

    private fun moveBugSmooth(bug: ImageView, fieldW: Int, fieldH: Int, sizePx: Int) {
        if (fieldW <= sizePx || fieldH <= sizePx) return

        val endX = Random.nextInt(0, fieldW - sizePx).toFloat()
        val endY = Random.nextInt(0, fieldH - sizePx).toFloat()
        val startX = bug.x
        val startY = bug.y
        val dx = endX - startX
        val dy = endY - startY
        val distance = sqrt(dx * dx + dy * dy)
        val speed = 50f * gameViewModel.gameSpeed
        val duration = (distance / speed * 1000).toLong().coerceAtLeast(200L)

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            addUpdateListener { v ->
                val fraction = v.animatedValue as Float
                val prevX = bug.x
                val prevY = bug.y
                bug.x = startX + dx * fraction
                bug.y = startY + dy * fraction
                val angle = Math.toDegrees(atan2((bug.y - prevY).toDouble(), (bug.x - prevX).toDouble())).toFloat() + 90f
                bug.rotation = angle
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!tiltActive) moveBugSmooth(bug, fieldW, fieldH, sizePx)
                }
            })
        }
        bugAnimators[bug] = animator
        animator.start()
    }

    private fun startSpawning() {
        spawnRunnable = object : Runnable {
            override fun run() {
                val toSpawn = (gameViewModel.maxBugs - gameField.childCountExcludingBonus()).coerceAtLeast(0)
                repeat(toSpawn) { spawnRandomBug() }
                handler.postDelayed(this, (1200L / gameViewModel.gameSpeed).toLong())
            }
        }
        handler.post(spawnRunnable)
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (roundRemainingMs <= 0) {
                    gameViewModel.setTime(0)
                    return
                }
                roundRemainingMs -= 1000
                gameViewModel.setTime(roundRemainingMs / 1000)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable)
    }

    // ===== бонус =====
    private fun startBonusLoop() {
        bonusLoopRunnable = object : Runnable {
            override fun run() {
                showBonusOnce()
                bonusHandler.postDelayed(this, (gameViewModel.bonusIntervalSec * 1000L))
            }
        }
        bonusHandler.postDelayed(bonusLoopRunnable, (gameViewModel.bonusIntervalSec * 1000L))
    }

    private fun showBonusOnce() {
        if (tiltActive) return
        val bw = bonusView.width.takeIf { it > 0 } ?: (48 * resources.displayMetrics.density).toInt()
        val bh = bonusView.height.takeIf { it > 0 } ?: (48 * resources.displayMetrics.density).toInt()
        val w = gameField.width
        val h = gameField.height
        if (w <= bw || h <= bh) return
        val x = Random.nextInt(0, w - bw)
        val y = Random.nextInt(0, h - bh)
        bonusView.x = x.toFloat()
        bonusView.y = y.toFloat()
        bonusView.visibility = View.VISIBLE
        bonusView.removeCallbacks(hideBonusRunnable)
        bonusView.postDelayed(hideBonusRunnable, bonusLifetimeMs)
        bonusView.setOnClickListener { activateTiltBonus() }
    }

    private val hideBonusRunnable = Runnable { if (!tiltActive) bonusView.visibility = View.GONE }

    private fun activateTiltBonus() {
        if (tiltActive) return
        tiltActive = true
        bonusView.visibility = View.GONE

        // Остановить все анимации жуков
        bugAnimators.values.forEach { it.cancel() }
        bugAnimators.clear()

        accel?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME) }

        tiltRunnable = object : Runnable {
            override fun run() {
                if (!tiltActive) return
                val dt = 0.016f
                val dx = tiltX * 500f * gameViewModel.gameSpeed * dt
                val dy = tiltY * 500f * gameViewModel.gameSpeed * dt
                val w = gameField.width
                val h = gameField.height
                for (i in 0 until gameField.childCount) {
                    val child = gameField.getChildAt(i)
                    if (child === bonusView || child !is ImageView) continue
                    val prevX = child.x
                    val prevY = child.y
                    var nx = child.x + dx
                    var ny = child.y + dy
                    nx = nx.coerceIn(0f, (w - child.width).toFloat())
                    ny = ny.coerceIn(0f, (h - child.height).toFloat())
                    child.x = nx
                    child.y = ny
                    val angle = Math.toDegrees(atan2((ny - prevY).toDouble(), (nx - prevX).toDouble())).toFloat() + 90f
                    child.rotation = angle
                }
                tiltHandler.postDelayed(this, 16L)
            }
        }
        tiltHandler.post(tiltRunnable)
        handler.postDelayed({ deactivateTiltBonus() }, tiltDurationMs)
    }

    private fun deactivateTiltBonus() {
        if (!tiltActive) return
        tiltActive = false
        sensorManager.unregisterListener(sensorListener)
        tiltHandler.removeCallbacks(tiltRunnable)

        val sizePx = (bugSizeDp * resources.displayMetrics.density).toInt()
        val w = gameField.width
        val h = gameField.height
        for (i in 0 until gameField.childCount) {
            val child = gameField.getChildAt(i)
            if (child === bonusView || child !is ImageView) continue
            moveBugSmooth(child, w, h, sizePx)
        }
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            if (!tiltActive) return
            val g = SensorManager.GRAVITY_EARTH
            val nx = e.values[0] / g
            val ny = e.values[1] / g
            val nz = e.values[2] / g
            val pitch = atan2(-nx.toDouble(), sqrt((ny * ny + nz * nz).toDouble())).toFloat()
            val roll = atan2(ny.toDouble(), nz.toDouble()).toFloat()
            tiltX = roll.coerceIn(-1f, 1f)
            tiltY = pitch.coerceIn(-1f, 1f)
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun endGame() {
        handler.removeCallbacks(spawnRunnable)
        handler.removeCallbacks(timerRunnable)
        bonusHandler.removeCallbacks(bonusLoopRunnable)
        bonusView.removeCallbacks(hideBonusRunnable)
        tiltHandler.removeCallbacksAndMessages(null)
        sensorManager.unregisterListener(sensorListener)
        bugAnimators.values.forEach { it.cancel() }
        bugAnimators.clear()
        gameViewModel.clearBugs()
        finish()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(spawnRunnable)
        handler.removeCallbacks(timerRunnable)
        bonusHandler.removeCallbacks(bonusLoopRunnable)
        bonusView.removeCallbacks(hideBonusRunnable)
        tiltHandler.removeCallbacksAndMessages(null)
    }
}

// ===== утилиты =====
private fun ViewGroup.childCountExcludingBonus(): Int {
    var c = 0
    for (i in 0 until childCount) {
        val v = getChildAt(i)
        if (v is ImageView && v.id != R.id.bonusView) c++
    }
    return c
}

private fun ViewGroup.childrenMap(): Map<Int, ImageView> {
    val map = mutableMapOf<Int, ImageView>()
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child is ImageView) {
            map[child.hashCode()] = child
        }
    }
    return map
}
