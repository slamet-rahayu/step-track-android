package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DailySteps
import com.example.data.StepDatabase
import com.example.data.StepRepository
import com.example.sensor.StepSensorState
import com.example.sensor.StepTrackerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StepViewModel(
    application: Application,
    private val repository: StepRepository
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    val activeSensorType = StepSensorState.activeSensorType
    val isServiceRunning = StepSensorState.isServiceRunning

    // Selected date for viewing details, defaults to today
    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Observe step data for the selected date using dynamic state flatmapping
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedDaySteps: StateFlow<DailySteps?> = _selectedDate
        .flatMapLatest { date ->
            repository.getStepsForDateFlow(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    // Instead of nested mapping, let's just observe today's steps and the history flow directly
    val todaySteps: StateFlow<DailySteps?> = repository.getStepsForDateFlow(getTodayDateString())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val stepHistory: StateFlow<List<DailySteps>> = repository.getRecentStepsFlow(7)
        .map { list ->
            // Make sure the list is filled for the past 7 days so the chart looks fully complete!
            fillPast7Days(list)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Automatically start service when first launching
        startSensorService()
        seedTestDataIfNeeded()
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    fun startSensorService() {
        try {
            val intent = Intent(context, StepTrackerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopSensorService() {
        try {
            val intent = Intent(context, StepTrackerService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateStepGoal(goal: Int) {
        viewModelScope.launch {
            repository.setStepGoal(getTodayDateString(), goal, todaySteps.value?.steps ?: 0)
        }
    }

    // Manual step simulation for testing/running inside preview (optional helper)
    fun addManualSteps(amount: Int) {
        viewModelScope.launch {
            repository.recordSteps(getTodayDateString(), amount, todaySteps.value?.target ?: 10000)
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun fillPast7Days(dbList: List<DailySteps>): List<DailySteps> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val map = dbList.associateBy { it.date }
        val result = mutableListOf<DailySteps>()
        
        val cal = Calendar.getInstance()
        // Start 6 days ago and go up to today (total 7 days)
        cal.add(Calendar.DAY_OF_YEAR, -6)
        
        for (i in 0 until 7) {
            val dateStr = sdf.format(cal.time)
            val record = map[dateStr] ?: DailySteps(
                date = dateStr,
                steps = 0,
                target = 10000,
                calories = 0.0,
                distance = 0.0,
                activeTimeMillis = 0L,
                lastUpdated = System.currentTimeMillis()
            )
            result.add(record)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    private fun seedTestDataIfNeeded() {
        viewModelScope.launch {
            // Use first() to retrieve database items exactly once on start and check if we need to seed.
            // This prevents the infinite database update loop!
            try {
                val list = repository.getAllStepsFlow().first()
                if (list.isEmpty()) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val cal = Calendar.getInstance()
                    
                    // Seed past 6 days (excluding today)
                    val realisticSteps = listOf(8430, 11200, 6500, 9300, 12400, 7800)
                    for (i in 5 downTo 0) {
                        cal.time = Date()
                        cal.add(Calendar.DAY_OF_YEAR, -(i + 1))
                        val dateStr = sdf.format(cal.time)
                        val stepsCount = realisticSteps[i % realisticSteps.size]
                        
                        repository.insertOrUpdate(
                            DailySteps(
                                date = dateStr,
                                steps = stepsCount,
                                target = 10000,
                                distance = stepsCount * 0.75,
                                calories = stepsCount * 0.04,
                                activeTimeMillis = stepsCount * 500L,
                                lastUpdated = System.currentTimeMillis()
                            )
                        )
                    }
                    
                    // Seed today if empty
                    val todayStr = sdf.format(Date())
                    repository.setStepGoal(todayStr, 10000, 1250) // seed initial steps to show layout
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class StepViewModelFactory(
    private val application: Application,
    private val repository: StepRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StepViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StepViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
