package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.exposures.database.entity.LightMeterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LightMeterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(lightMeters: List<LightMeterEntity>)

    @Query("DELETE FROM light_meters")
    suspend fun deleteAll()

    /** Full replace — phone is authoritative, so a sync should look exactly like what it just sent. */
    @Transaction
    suspend fun replaceAll(lightMeters: List<LightMeterEntity>) {
        deleteAll()
        upsertAll(lightMeters)
    }

    @Query("SELECT * FROM light_meters ORDER BY name")
    fun getAll(): Flow<List<LightMeterEntity>>

    @Query("SELECT * FROM light_meters WHERE id = :id")
    suspend fun getById(id: String): LightMeterEntity?

    @Query("SELECT COUNT(*) FROM light_meters")
    suspend fun count(): Int
}
