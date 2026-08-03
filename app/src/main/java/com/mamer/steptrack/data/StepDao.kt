package com.mamer.steptrack.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Query("SELECT * FROM daily_steps WHERE date = :date")
    fun getStepsForDateFlow(date: String): Flow<DailySteps?>

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun getStepsForDate(date: String): DailySteps?

    @Query("SELECT * FROM daily_steps ORDER BY date DESC")
    fun getAllStepsFlow(): Flow<List<DailySteps>>

    @Query("SELECT * FROM daily_steps ORDER BY date DESC LIMIT :limit")
    fun getRecentStepsFlow(limit: Int): Flow<List<DailySteps>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(dailySteps: DailySteps)

    @Query("DELETE FROM daily_steps WHERE date = :date")
    suspend fun deleteStepsForDate(date: String)
}
