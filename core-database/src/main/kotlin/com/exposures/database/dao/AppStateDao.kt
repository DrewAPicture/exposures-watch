package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exposures.database.entity.AppStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppStateDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureRowExists(row: AppStateEntity)

    @Query("SELECT activeRollId FROM app_state WHERE id = ${AppStateEntity.SINGLETON_ID}")
    fun observeActiveRollId(): Flow<String?>

    @Query("UPDATE app_state SET activeRollId = :rollId WHERE id = ${AppStateEntity.SINGLETON_ID}")
    suspend fun setActiveRollId(rollId: String?)
}
