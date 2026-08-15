package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exposures.database.entity.ExposureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExposureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(exposure: ExposureEntity)

    @Query("SELECT * FROM exposures WHERE filmRollId = :filmRollId ORDER BY frameNumber")
    fun getByRoll(filmRollId: String): Flow<List<ExposureEntity>>

    @Query("SELECT * FROM exposures WHERE id = :id")
    suspend fun getById(id: String): ExposureEntity?
}
