package com.janhorak.shutterdeck.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Root Room database for ShutterDeck. Add new entities and DAOs here as features land. */
@Database(
    entities = [
        ScenePresetEntity::class,
        LocationEntity::class,
        ShootEntity::class,
        ShotItemEntity::class,
        GearItemEntity::class,
        GearFilterEntity::class,
        GearBatteryEntity::class,
        GearMemoryCardEntity::class,
        GearLoanEntity::class,
        GearKitEntity::class,
        GearKitItemEntity::class,
        GearMaintenanceEntryEntity::class,
        FilmStockEntity::class,
        FilmRollEntity::class,
        FilmFrameEntity::class,
    ],
    version = 15,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scenePresetDao(): ScenePresetDao
    abstract fun locationDao(): LocationDao
    abstract fun shootDao(): ShootDao
    abstract fun gearItemDao(): GearItemDao
    abstract fun gearFilterDao(): GearFilterDao
    abstract fun gearBatteryDao(): GearBatteryDao
    abstract fun gearMemoryCardDao(): GearMemoryCardDao
    abstract fun gearLoanDao(): GearLoanDao
    abstract fun gearKitDao(): GearKitDao
    abstract fun gearMaintenanceDao(): GearMaintenanceDao
    abstract fun filmStockDao(): FilmStockDao
    abstract fun filmRollDao(): FilmRollDao

    companion object {
        val MIGRATION_14_15: Migration = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE scout_locations ADD COLUMN referencePhotoUri TEXT NOT NULL DEFAULT ''",
                )
            }
        }
    }
}
