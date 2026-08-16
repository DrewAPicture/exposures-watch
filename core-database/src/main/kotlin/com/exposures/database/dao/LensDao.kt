package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.exposures.database.entity.LensEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LensDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(lenses: List<LensEntity>)

    @Query("DELETE FROM lenses")
    suspend fun deleteAll()

    @Query("DELETE FROM lenses WHERE id NOT IN (:ids) AND id NOT IN (SELECT DISTINCT lensId FROM exposures)")
    suspend fun deleteNotInPreservingReferenced(ids: List<String>)

    @Query("DELETE FROM lenses WHERE id NOT IN (SELECT DISTINCT lensId FROM exposures)")
    suspend fun deleteUnreferenced()

    /** Full replace — phone is authoritative, so a sync should look exactly like what it just sent. */
    @Transaction
    suspend fun replaceAll(lenses: List<LensEntity>) {
        deleteAll()
        upsertAll(lenses)
    }

    @Query("SELECT * FROM lenses ORDER BY name")
    fun getAll(): Flow<List<LensEntity>>

    @Query("SELECT * FROM lenses WHERE id = :id")
    suspend fun getById(id: String): LensEntity?

    @Query("SELECT COUNT(*) FROM lenses")
    suspend fun count(): Int
}
