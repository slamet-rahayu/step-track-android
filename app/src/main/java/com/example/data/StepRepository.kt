package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StepRepository(private val stepDao: StepDao) {

    fun getStepsForDateFlow(date: String): Flow<DailySteps?> = stepDao.getStepsForDateFlow(date)

    fun getAllStepsFlow(): Flow<List<DailySteps>> = stepDao.getAllStepsFlow()

    fun getRecentStepsFlow(limit: Int): Flow<List<DailySteps>> = stepDao.getRecentStepsFlow(limit)

    suspend fun insertOrUpdate(dailySteps: DailySteps) = withContext(Dispatchers.IO) {
        stepDao.insertOrUpdate(dailySteps)
    }

    suspend fun recordSteps(date: String, stepsToAdd: Int, target: Int) = withContext(Dispatchers.IO) {
        val existing = stepDao.getStepsForDate(date)
        val newSteps = (existing?.steps ?: 0) + stepsToAdd
        
        // Stride is approx 0.75m. Calories approx 0.04 kcal per step
        val distance = newSteps * 0.75
        val calories = newSteps * 0.04
        
        // Assume approx 120 steps per minute walking -> 1 step is 500ms active time
        val activeTime = (newSteps * 500L) 
        
        val record = DailySteps(
            date = date,
            steps = newSteps,
            target = existing?.target ?: target,
            distance = distance,
            calories = calories,
            activeTimeMillis = activeTime,
            lastUpdated = System.currentTimeMillis()
        )
        stepDao.insertOrUpdate(record)
    }

    suspend fun setStepGoal(date: String, newGoal: Int, defaultStepsIfNew: Int = 0) = withContext(Dispatchers.IO) {
        val existing = stepDao.getStepsForDate(date)
        if (existing != null) {
            stepDao.insertOrUpdate(existing.copy(target = newGoal, lastUpdated = System.currentTimeMillis()))
        } else {
            val distance = defaultStepsIfNew * 0.75
            val calories = defaultStepsIfNew * 0.04
            val activeTime = defaultStepsIfNew * 500L
            stepDao.insertOrUpdate(
                DailySteps(
                    date = date,
                    steps = defaultStepsIfNew,
                    target = newGoal,
                    distance = distance,
                    calories = calories,
                    activeTimeMillis = activeTime,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }
}
