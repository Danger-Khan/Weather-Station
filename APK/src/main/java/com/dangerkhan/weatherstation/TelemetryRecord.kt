package com.dangerkhan.weatherstation

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "telemetry_records")
data class TelemetryRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val valuesJson: String,
    val timestamp: String
)

@Entity(tableName = "modules")
data class Module(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val unit: String,
    val jsonKey: String,
    val color: Long,
    val type: String = "metric", // "metric" or "status"
    val isEnabled: Boolean = true
)

@Dao
interface TelemetryDao {
    @Insert
    suspend fun insert(record: TelemetryRecord)

    @Query("SELECT * FROM telemetry_records ORDER BY id DESC")
    fun getAllRecords(): Flow<List<TelemetryRecord>>

    @Delete
    suspend fun delete(record: TelemetryRecord)

    @Query("DELETE FROM telemetry_records")
    suspend fun deleteAll()
}

@Dao
interface ModuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(module: Module)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(modules: List<Module>)

    @Update
    suspend fun update(module: Module)

    @Delete
    suspend fun delete(module: Module)

    @Query("SELECT * FROM modules")
    fun getAllModules(): Flow<List<Module>>

    @Query("SELECT COUNT(*) FROM modules")
    suspend fun getCount(): Int
}
