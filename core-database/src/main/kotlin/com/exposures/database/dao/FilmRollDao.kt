package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.exposures.database.entity.FilmRollEntity
import com.exposures.model.RollStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FilmRollDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rolls: List<FilmRollEntity>)

    @Query("DELETE FROM film_rolls")
    suspend fun deleteAll()

    /** Full replace — phone is authoritative, so a sync should look exactly like what it just sent. */
    @Transaction
    suspend fun replaceAll(rolls: List<FilmRollEntity>) {
        deleteAll()
        upsertAll(rolls)
    }

    @Query("SELECT * FROM film_rolls ORDER BY name")
    fun getAll(): Flow<List<FilmRollEntity>>

    @Query("SELECT * FROM film_rolls WHERE status = :status ORDER BY name")
    fun getByStatus(status: RollStatus = RollStatus.AVAILABLE): Flow<List<FilmRollEntity>>

    @Query("SELECT * FROM film_rolls WHERE id = :id")
    fun observeById(id: String): Flow<FilmRollEntity?>

    @Query("SELECT * FROM film_rolls WHERE id = :id")
    suspend fun getById(id: String): FilmRollEntity?

    @Query("SELECT COUNT(*) FROM film_rolls")
    suspend fun count(): Int
}
