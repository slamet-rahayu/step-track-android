package com.mamer.steptrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DailySteps::class], version = 1, exportSchema = false)
abstract class StepDatabase : RoomDatabase() {
    abstract val stepDao: StepDao

    companion object {
        @Volatile
        private var INSTANCE: StepDatabase? = null

        fun getDatabase(context: Context): StepDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StepDatabase::class.java,
                    "step_counter_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
