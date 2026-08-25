package com.dangerkhan.weatherstation

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [TelemetryRecord::class, Module::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao
    abstract fun moduleDao(): ModuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "weather_station_db"
                )
                .fallbackToDestructiveMigration() // Reset DB for schema change
                .addCallback(DatabaseCallback(context))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(database.moduleDao())
                    }
                }
            }

            suspend fun seedDatabase(moduleDao: ModuleDao) {
                val defaultModules = listOf(
                    Module(name = "TEMPERATURE", unit = "°C", jsonKey = "temperature", color = 0xFFF97316, type = "metric"),
                    Module(name = "HUMIDITY", unit = "%", jsonKey = "humidity", color = 0xFF38BDF8, type = "metric"),
                    Module(name = "RAIN METER", unit = "", jsonKey = "rain", color = 0xFF60A5FA, type = "status"),
                    Module(name = "BATTERY", unit = "%", jsonKey = "battery", color = 0xFF10B981, type = "metric")
                )
                moduleDao.insertAll(defaultModules)
            }
        }
    }
}
