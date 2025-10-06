package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScoreDao {
    @Insert
    suspend fun insertScore(score: Score)

    @Query("SELECT * FROM scores ORDER BY points DESC")
    suspend fun getAllScores(): List<Score>
}

