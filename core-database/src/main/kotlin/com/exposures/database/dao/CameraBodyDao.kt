package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exposures.database.entity.CameraBodyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraBodyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(bodies: List<CameraBodyEntity>)

    @Query("SELECT * FROM camera_bodies ORDER BY name")
    fun getAll(): Flow<List<CameraBodyEntity>>

    @Query("SELECT * FROM camera_bodies WHERE id = :id")
    suspend fun getById(id: String): CameraBodyEntity?

    @Query("SELECT COUNT(*) FROM camera_bodies")
    suspend fun count(): Int
}
