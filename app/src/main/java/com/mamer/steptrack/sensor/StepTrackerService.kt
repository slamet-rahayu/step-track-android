package com.mamer.steptrack.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mamer.steptrack.MainActivity
import com.mamer.steptrack.data.StepDatabase
import com.mamer.steptrack.data.StepRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt
import androidx.core.content.edit

class StepTrackerService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private lateinit var sensorManager: SensorManager
    private lateinit var repository: StepRepository
    
    private var stepCounterSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    
    // Preferences for delta calculation of cumulative steps
    private val PREF_NAME = "step_counter_prefs"
    private val KEY_LAST_RAW_STEPS = "last_raw_steps"
    private val KEY_LAST_RAW_DATE = "last_raw_date"

    // Accelerometer variables
    private var gravity = 9.8f
    private val alpha = 0.9f
    private var lastStepTime = 0L
    private val stepDebounceDelay = 350L // ms
    private val thresholdUpper = 1.4f // m/s^2 deviation to register a step

    companion object {
        const val NOTIFICATION_ID = 8871
        const val CHANNEL_ID = "step_tracker_channel"
        private const val TAG = "StepTrackerService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate called")
        StepSensorState.updateServiceRunning(true)
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val database = StepDatabase.getDatabase(applicationContext)
        repository = StepRepository(database.stepDao)
        
        createNotificationChannel()
        startServiceInForeground()
        
        registerSensors()
        observeTodaySteps()
    }

    private fun registerSensors() {
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounterSensor != null) {
            Log.d(TAG, "Step Counter hardware sensor found!")
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
            StepSensorState.updateSensorType("Hardware Step Sensor")
        } else {
            Log.d(TAG, "Step Counter sensor not found, using accelerometer fallback")
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accelerometerSensor != null) {
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
                StepSensorState.updateSensorType("Accelerometer Fallback")
            } else {
                Log.e(TAG, "No suitable sensor found!")
                StepSensorState.updateSensorType("No Sensors Available")
            }
        }
    }

    private fun observeTodaySteps() {
        serviceScope.launch {
            repository.getStepsForDateFlow(getTodayDate()).collectLatest { dailySteps ->
                val steps = dailySteps?.steps ?: 0
                val target = dailySteps?.target ?: 10000
                val progressPercent = if (target > 0) (steps * 100) / target else 0
                
                updateNotification("Today: $steps / $target steps ($progressPercent%)")
            }
        }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy called")
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        StepSensorState.updateServiceRunning(false)
    }

    // SensorEventListener contract
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val rawValue = event.values[0].toInt()
                handleStepCounterEvent(rawValue)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                handleAccelerometerEvent(event.values)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun handleStepCounterEvent(rawValue: Int) {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lastRaw = prefs.getInt(KEY_LAST_RAW_STEPS, -1)
        val lastDate = prefs.getString(KEY_LAST_RAW_DATE, "")
        val today = getTodayDate()

        var delta = 0
        
        if (lastRaw == -1 || today != lastDate) {
            // First reading ever, or new day starting. We log and start delta from this point.
            // On a new day, we reset baseline to the current raw count.
            prefs.edit {
                putInt(KEY_LAST_RAW_STEPS, rawValue)
                    .putString(KEY_LAST_RAW_DATE, today)
            }
            
            // We can also assume that if a new day is starting, the delta counts from last event.
            // But to be safe, we reset the running diff baseline. 
            // Let's just consume the reading.
            return
        }

        if (rawValue < lastRaw) {
            // Device rebooted. Raw steps reset to 0+
            delta = rawValue
        } else {
            delta = rawValue - lastRaw
        }

        if (delta > 0) {
            prefs.edit {
                putInt(KEY_LAST_RAW_STEPS, rawValue)
                    .putString(KEY_LAST_RAW_DATE, today)
            }
            
            serviceScope.launch {
                repository.recordSteps(today, delta, 10000)
            }
        }
    }

    private fun handleAccelerometerEvent(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        
        // Low-pass filter to capture gravity
        gravity = alpha * gravity + (1 - alpha) * magnitude
        
        // Subtract gravity from total magnitude to isolate dynamic acceleration
        val dynamicMag = magnitude - gravity
        
        val currentTime = System.currentTimeMillis()
        if (dynamicMag > thresholdUpper && (currentTime - lastStepTime) > stepDebounceDelay) {
            lastStepTime = currentTime
            
            val today = getTodayDate()
            serviceScope.launch {
                repository.recordSteps(today, 1, 10000)
            }
        }
    }

    // Foreground service building & updates
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Tracker Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps step sensor tracking active in the background"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startServiceInForeground() {
        val notification = buildNotification("Step tracker is starting...")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification(contentText)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "dashboard")
        }
        
        // Handle API levels gracefully with FLAG_IMMUTABLE
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Step Track Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_compass) // simple system status icon
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
