package com.exposures.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.exposures.database.dao.AppStateDao
import com.exposures.database.dao.CameraBodyDao
import com.exposures.database.dao.ExposureDao
import com.exposures.database.dao.FilmBackDao
import com.exposures.database.dao.FilmMediumDao
import com.exposures.database.dao.LensDao
import com.exposures.database.dao.LightMeterDao
import com.exposures.database.entity.AppStateEntity
import com.exposures.database.entity.CameraBodyEntity
import com.exposures.database.entity.ExposureEntity
import com.exposures.database.entity.FilmBackEntity
import com.exposures.database.entity.FilmMediumEntity
import com.exposures.database.entity.LensEntity
import com.exposures.database.entity.LightMeterEntity

@Database(
    entities = [
        CameraBodyEntity::class,
        LensEntity::class,
        LightMeterEntity::class,
        FilmBackEntity::class,
        FilmMediumEntity::class,
        ExposureEntity::class,
        AppStateEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ExposuresDatabase : RoomDatabase() {
    abstract fun cameraBodyDao(): CameraBodyDao
    abstract fun lensDao(): LensDao
    abstract fun lightMeterDao(): LightMeterDao
    abstract fun filmBackDao(): FilmBackDao
    abstract fun filmMediumDao(): FilmMediumDao
    abstract fun exposureDao(): ExposureDao
    abstract fun appStateDao(): AppStateDao

    companion object {
        const val DATABASE_NAME = "exposures.db"
    }
}
