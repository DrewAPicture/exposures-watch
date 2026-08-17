package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exposures.database.entity.ExposureEntity
import com.exposures.model.PhotoStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ExposureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(exposure: ExposureEntity)

    /** Most-recent-frame-first — the order Frame History wants to show. */
    @Query("SELECT * FROM exposures WHERE filmRollId = :filmRollId ORDER BY frameNumber DESC")
    fun getByRoll(filmRollId: String): Flow<List<ExposureEntity>>

    @Query("SELECT * FROM exposures")
    suspend fun getAllOnce(): List<ExposureEntity>

    @Query("SELECT * FROM exposures WHERE id = :id")
    suspend fun getById(id: String): ExposureEntity?

    /** Applied when the phone's photo-status sync or a capture-result ack arrives — phone owns this field. */
    @Query("UPDATE exposures SET referencePhotoStatus = :status, updatedAt = :updatedAt WHERE id = :exposureId")
    suspend fun updatePhotoStatus(exposureId: String, status: PhotoStatus, updatedAt: Long)
}
