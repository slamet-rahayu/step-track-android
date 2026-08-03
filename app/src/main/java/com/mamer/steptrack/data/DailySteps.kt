package com.mamer.steptrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_steps")
data class DailySteps(
    @PrimaryKey
    val date: String, // format: "yyyy-MM-dd"
    val steps: Int,
    val target: Int = 10000,
    val calories: Double = 0.0, // kcal
    val distance: Double = 0.0, // meters
    val activeTimeMillis: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
)
