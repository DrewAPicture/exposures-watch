package com.exposures.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Singleton row (always id = 0) holding watch-local state that isn't part of the synced domain model. */
@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val activeRollId: String?,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
