package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.exposures.database.entity.FilmBackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FilmBackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(filmBacks: List<FilmBackEntity>)

    @Query("DELETE FROM film_backs")
    suspend fun deleteAll()

    /** Full replace — phone is authoritative, so a sync should look exactly like what it just sent. */
    @Transaction
    suspend fun replaceAll(filmBacks: List<FilmBackEntity>) {
        deleteAll()
        upsertAll(filmBacks)
    }

    @Query("SELECT * FROM film_backs ORDER BY name")
    fun getAll(): Flow<List<FilmBackEntity>>

    @Query("SELECT * FROM film_backs WHERE id = :id")
    suspend fun getById(id: String): FilmBackEntity?

    @Query("SELECT COUNT(*) FROM film_backs")
    suspend fun count(): Int
}
