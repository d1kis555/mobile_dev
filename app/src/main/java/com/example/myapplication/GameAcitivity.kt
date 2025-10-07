package com.example.myapplication

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
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
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.Score
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameActivity : ComponentActivity() {

    private lateinit var tvScore: TextView
    private lateinit var tvTimer: TextView
    private lateinit var gameField: FrameLayout
    private lateinit var bonusView: ImageView

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

    // ===== бонус/наклон =====
    private val bonusHandler = Handler(Looper.getMainLooper())
    private lateinit var bonusLoopRunnable: Runnable
    private val bonusPeriodMs = 15_000L     // новые бонусы каждые 15с
    private val bonusLifetimeMs = 10_000L   // бонус исчезает через 10с, если не кликнули
    private val tiltDurationMs = 5_000L     // режим наклона активен 5с

    private var tiltActive = false
    private var tiltX = 0f   // -1..1 (вправо положительный)
    private var tiltY = 0f   // -1..1 (вниз положительный)
    private val tiltHandler = Handler(Looper.getMainLooper())
    private lateinit var tiltRunnable: Runnable
    private val bugAnimators = mutableMapOf<ImageView, ValueAnimator>()
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var sensorManager: SensorManager
    private var accel: Sensor? = null

    // pitch/roll из акселерометра — чтобы движение соответствовало экрану
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            if (!tiltActive) return
            val g = SensorManager.GRAVITY_EARTH
            val nx = e.values[0] / g // ось X устройства
            val ny = e.values[1] / g // ось Y устройства
            val nz = e.values[2] / g // ось Z (перпендикуляр к экрану)

            // pitch: вперёд/назад (вперёд => вниз по экрану)
            val pitch = atan2(
                -nx.toDouble(),
                sqrt((ny * ny + nz * nz).toDouble())
            ).toFloat()

            // roll: влево/вправо (вправо => вправо по экрану)
            val roll = atan2(
                ny.toDouble(),
                nz.toDouble()
            ).toFloat()

            tiltX = roll.coerceIn(-1f, 1f)
            tiltY = pitch.coerceIn(-1f, 1f)
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        tvScore = findViewById(R.id.tvScore)
        tvTimer = findViewById(R.id.tvTimer)
        gameField = findViewById(R.id.gameField)
        bonusView = findViewById(R.id.bonusView)

        val btnMenu: Button = findViewById(R.id.btnMenu)
        btnMenu.setOnClickListener { finish() }

        loadSettings()
        roundRemainingMs = roundDurationMs
        db = AppDatabase.getDatabase(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

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

        // клик по бонусу — активируем режим наклона
        bonusView.setOnClickListener { activateTiltBonus() }

        // периодический показ бонуса
        bonusLoopRunnable = object : Runnable {
            override fun run() {
                showBonusOnce()
                bonusHandler.postDelayed(this, bonusPeriodMs)
            }
        }
        bonusHandler.postDelayed(bonusLoopRunnable, bonusPeriodMs)
    }

    // ===== настройки =====
    private fun loadSettings() {
        val p = getSharedPreferences("game_prefs", MODE_PRIVATE)
        gameSpeed = p.getInt("speed", 5).toFloat()
        maxBugs = p.getInt("max_bugs", 10)
        spawnIntervalMs = (p.getInt("bonus_interval", 15) * 100L).coerceAtLeast(350L)
        roundDurationMs = (p.getInt("round_duration", 120) * 1000L)
    }

    // ===== спавн жуков =====
    private fun startSpawning() {
        spawnRunnable = object : Runnable {
            override fun run() {
                val toSpawn = (maxBugs - gameField.childCountExcludingBonus()).coerceAtLeast(0)
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
                bugAnimators.remove(it)?.cancel()
                if (it.parent != null) (it.parent as ViewGroup).removeView(it)
            }
            score += 10
            tvScore.text = "Очки: $score"
        }

        gameField.addView(bug)
        if (!tiltActive) {
            moveBugSmooth(bug, fieldW, fieldH, sizePx)
        }
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
        val duration = (distance / speed * 1000).toLong().coerceAtLeast(200L)

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            addUpdateListener { valueAnimator ->
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
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (bug.parent != null && !tiltActive) moveBugSmooth(bug, fieldW, fieldH, sizePx)
                }
            })
        }
        bugAnimators[bug] = animator
        animator.start()
    }

    // ===== бонус =====
    private fun showBonusOnce() {
        if (tiltActive) return // пока активен — новый не показываем
        val bw = bonusView.width.takeIf { it > 0 } ?: (48 * resources.displayMetrics.density).toInt()
        val bh = bonusView.height.takeIf { it > 0 } ?: (48 * resources.displayMetrics.density).toInt()
        val w = gameField.width
        val h = gameField.height
        if (w == 0 || h == 0) return

        val x = Random.nextInt((w - bw).coerceAtLeast(1))
        val y = Random.nextInt((h - bh).coerceAtLeast(1))
        bonusView.x = x.toFloat()
        bonusView.y = y.toFloat()
        bonusView.visibility = android.view.View.VISIBLE

        // автоскрытие через 10с, если не нажали
        bonusView.removeCallbacks(hideBonusRunnable)
        bonusView.postDelayed(hideBonusRunnable, bonusLifetimeMs)
    }

    private val hideBonusRunnable = Runnable {
        if (!tiltActive) bonusView.visibility = android.view.View.GONE
    }

    private fun activateTiltBonus() {
        if (tiltActive) return
        tiltActive = true
        bonusView.visibility = android.view.View.GONE

        // звук
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, R.raw.bug_scream).apply { start() }

        // остановить текущие анимации пути у всех жуков
        bugAnimators.values.forEach { it.cancel() }
        bugAnimators.clear()

        // включить сенсор
        accel?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME) }

        // дрейф 60 Гц по pitch/roll
        val tiltSpeedPxPerSec = 500f * gameSpeed
        tiltRunnable = object : Runnable {
            override fun run() {
                if (!tiltActive) return
                val dt = 0.016f
                val dx = tiltX * tiltSpeedPxPerSec * dt
                val dy = tiltY * tiltSpeedPxPerSec * dt
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

                    val vx = nx - prevX
                    val vy = ny - prevY
                    if (vx != 0f || vy != 0f) {
                        val angle = Math.toDegrees(atan2(vy.toDouble(), vx.toDouble())).toFloat() + 90f
                        child.rotation = angle
                    }
                }
                tiltHandler.postDelayed(this, 16L)
            }
        }
        tiltHandler.post(tiltRunnable)

        // выключим наклон через 5 секунд и вернём обычное движение
        handler.postDelayed({ deactivateTiltBonus() }, tiltDurationMs)
    }

    private fun deactivateTiltBonus() {
        if (!tiltActive) return
        tiltActive = false
        sensorManager.unregisterListener(sensorListener)
        tiltHandler.removeCallbacks(tiltRunnable)

        // запустить обычные анимации снова
        val sizePx = (bugSizeDp * resources.displayMetrics.density).toInt()
        val w = gameField.width
        val h = gameField.height
        for (i in 0 until gameField.childCount) {
            val child = gameField.getChildAt(i)
            if (child === bonusView || child !is ImageView) continue
            moveBugSmooth(child, w, h, sizePx)
        }
    }

    // ===== таймер/счёт =====
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

    private fun saveScore(userName: String, score: Int) {
        val prefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        val speed = prefs.getInt("speed", 5)
        val maxBugs = prefs.getInt("max_bugs", 10)
        val bonusInterval = prefs.getInt("bonus_interval", 15)
        val roundDuration = prefs.getInt("round_duration", 30)

        val record = Score(
            userName = userName,
            points = score,
            difficulty = speed,
            maxBugs = maxBugs,
            bonusInterval = bonusInterval,
            roundDuration = roundDuration
        )
        CoroutineScope(Dispatchers.IO).launch {
            db.scoreDao().insertScore(record)
        }
    }

    private fun getCurrentUserName(): String {
        val prefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
        return prefs.getString("current_user", "Гость") ?: "Гость"
    }

    private fun endGame() {
        handler.removeCallbacks(spawnRunnable)
        handler.removeCallbacks(timerRunnable)
        bonusHandler.removeCallbacks(bonusLoopRunnable)
        bonusView.removeCallbacks(hideBonusRunnable)
        tiltHandler.removeCallbacksAndMessages(null)
        sensorManager.unregisterListener(sensorListener)
        mediaPlayer?.release()
        mediaPlayer = null

        roundRemainingMs = 0

        val childCount = gameField.childCount
        for (i in childCount - 1 downTo 0) {
            val child = gameField.getChildAt(i)
            child.animate().cancel()
            gameField.removeView(child)
        }

        gameField.setOnTouchListener(null)
        tvTimer.text = "00:00"

        val userName = getCurrentUserName()
        saveScore(userName, score)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Игра окончена!")
            .setMessage("Ваш счёт: $score")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss(); finish() }
            .setCancelable(false)
            .show()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorListener)
    }

    override fun onDestroy() {
        handler.removeCallbacks(spawnRunnable)
        handler.removeCallbacks(timerRunnable)
        bonusHandler.removeCallbacks(bonusLoopRunnable)
        bonusView.removeCallbacks(hideBonusRunnable)
        tiltHandler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}

// ===== утилиты =====
private fun ViewGroup.findViewAt(x: Float, y: Float): ImageView? {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child is ImageView && child.visibility == android.view.View.VISIBLE) {
            val left = child.x
            val top = child.y
            val right = left + child.width
            val bottom = top + child.height
            if (x in left..right && y in top..bottom) return child
        }
    }
    return null
}

private fun ViewGroup.childCountExcludingBonus(): Int {
    var c = 0
    for (i in 0 until childCount) {
        val v = getChildAt(i)
        if (v is ImageView && v.id != R.id.bonusView) c++
    }
    return c
}
