package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exposures.database.entity.CaptureRequestOutboxEntity

@Dao
interface CaptureRequestOutboxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(request: CaptureRequestOutboxEntity)

    @Query("DELETE FROM capture_request_outbox WHERE exposureId = :exposureId")
    suspend fun remove(exposureId: String)

    @Query("SELECT * FROM capture_request_outbox ORDER BY createdAt")
    suspend fun getAll(): List<CaptureRequestOutboxEntity>
}
