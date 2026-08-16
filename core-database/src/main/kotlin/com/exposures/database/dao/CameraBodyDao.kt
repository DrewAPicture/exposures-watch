package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.exposures.database.entity.CameraBodyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraBodyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(bodies: List<CameraBodyEntity>)

    @Query("DELETE FROM camera_bodies")
    suspend fun deleteAll()

    @Query("DELETE FROM camera_bodies WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<String>)

    /** Full replace — phone is authoritative, so a sync should look exactly like what it just sent. */
    @Transaction
    suspend fun replaceAll(bodies: List<CameraBodyEntity>) {
        deleteAll()
        upsertAll(bodies)
    }

    @Query("SELECT * FROM camera_bodies ORDER BY name")
    fun getAll(): Flow<List<CameraBodyEntity>>

    @Query("SELECT * FROM camera_bodies WHERE id = :id")
    suspend fun getById(id: String): CameraBodyEntity?

    @Query("SELECT COUNT(*) FROM camera_bodies")
    suspend fun count(): Int
}
