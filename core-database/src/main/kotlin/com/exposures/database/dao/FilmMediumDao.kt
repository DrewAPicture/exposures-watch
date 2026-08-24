package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.exposures.database.entity.FilmMediumEntity
import com.exposures.model.FilmMediumStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FilmMediumDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(filmMedia: List<FilmMediumEntity>)

    @Query("DELETE FROM film_media")
    suspend fun deleteAll()

    @Query("DELETE FROM film_media WHERE id NOT IN (:ids) AND id NOT IN (SELECT DISTINCT filmMediumId FROM exposures)")
    suspend fun deleteNotInPreservingReferenced(ids: List<String>)

    @Query("DELETE FROM film_media WHERE id NOT IN (SELECT DISTINCT filmMediumId FROM exposures)")
    suspend fun deleteUnreferenced()

    /** Full replace — phone is authoritative, so a sync should look exactly like what it just sent. */
    @Transaction
    suspend fun replaceAll(filmMedia: List<FilmMediumEntity>) {
        deleteAll()
        upsertAll(filmMedia)
    }

    @Query("SELECT * FROM film_media ORDER BY name COLLATE NOCASE")
    fun getAll(): Flow<List<FilmMediumEntity>>

    @Query("SELECT * FROM film_media WHERE status = :status ORDER BY name COLLATE NOCASE")
    fun getByStatus(status: FilmMediumStatus = FilmMediumStatus.AVAILABLE): Flow<List<FilmMediumEntity>>

    /** AVAILABLE + COMPLETED film media for the switcher, available ones first (string-sorts before "COMPLETED"), alphabetical within each. Excludes ARCHIVED. */
    @Query(
        "SELECT * FROM film_media WHERE status IN ('AVAILABLE', 'COMPLETED') " +
            "ORDER BY status, name COLLATE NOCASE",
    )
    fun getSwitcherFilmMedia(): Flow<List<FilmMediumEntity>>

    @Query("UPDATE film_media SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: FilmMediumStatus)

    @Query("SELECT * FROM film_media WHERE id = :id")
    fun observeById(id: String): Flow<FilmMediumEntity?>

    @Query("SELECT * FROM film_media WHERE id = :id")
    suspend fun getById(id: String): FilmMediumEntity?

    @Query("SELECT COUNT(*) FROM film_media")
    suspend fun count(): Int
}
