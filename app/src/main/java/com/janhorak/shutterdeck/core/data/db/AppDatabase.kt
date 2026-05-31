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
        ShotNoteEntity::class,
        LightingSetupEntity::class,
        LightingSetupItemEntity::class,
    ],
    version = 17,
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
    abstract fun shotNoteDao(): ShotNoteDao
    abstract fun lightingSetupDao(): LightingSetupDao

    companion object {
        val MIGRATION_14_15: Migration = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE scout_locations ADD COLUMN referencePhotoUri TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        val MIGRATION_15_16: Migration = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shot_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        shotLabel TEXT NOT NULL,
                        noteText TEXT NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_16_17: Migration = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `lighting_setups` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `lighting_setup_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `setupId` INTEGER NOT NULL,
                        `itemType` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `xFraction` REAL NOT NULL,
                        `yFraction` REAL NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        FOREIGN KEY(`setupId`) REFERENCES `lighting_setups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_lighting_setup_items_setupId`
                    ON `lighting_setup_items` (`setupId`)
                    """.trimIndent(),
                )
            }
        }
    }
}
