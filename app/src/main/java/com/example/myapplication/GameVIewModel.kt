package com.example.myapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class BugState(
    val id: Int,
    val isGolden: Boolean = false
)

class GameViewModel : ViewModel() {

    // Таймер и очки
    private var roundDurationMs: Long = 60_000L
    private val _timeLeft = MutableLiveData(roundDurationMs / 1000)
    val timeLeft: LiveData<Long> = _timeLeft

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score

    fun setTime(seconds: Long) { _timeLeft.value = seconds }
    fun setScore(points: Int) { _score.value = points }
    fun addScore(points: Int) { _score.value = (_score.value ?: 0) + points }

    // Спавн жуков
    private val _bugs = MutableLiveData<MutableList<BugState>>(mutableListOf())
    val bugs: LiveData<MutableList<BugState>> = _bugs

    fun spawnBug(isGolden: Boolean = false) {
        val list = _bugs.value ?: mutableListOf()
        val id = if (list.isEmpty()) 1 else list.maxOf { it.id } + 1
        list.add(BugState(id, isGolden))
        _bugs.value = list
    }

    fun removeBug(id: Int) {
        _bugs.value = _bugs.value?.filter { it.id != id }?.toMutableList()
    }

    fun clearBugs() { _bugs.value?.clear() }

    // Настройки
    var gameSpeed: Float = 3f
    var maxBugs: Int = 10
    var bonusIntervalSec: Int = 15
    var roundDurationSec: Int = 60
}
