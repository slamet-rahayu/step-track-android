package com.example.sensor

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StepSensorState {
    private val _activeSensorType = MutableStateFlow("None")
    val activeSensorType: StateFlow<String> = _activeSensorType.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    fun updateSensorType(type: String) {
        _activeSensorType.value = type
    }

    fun updateServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }
}
