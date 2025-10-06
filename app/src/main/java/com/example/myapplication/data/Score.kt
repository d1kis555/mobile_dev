package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class Score(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userName: String,
    val points: Int,
    val difficulty: Int,
    val maxBugs: Int,
    val bonusInterval: Int,
    val roundDuration: Int
)
