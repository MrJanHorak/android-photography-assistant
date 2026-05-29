package com.janhorak.shutterdeck.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

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
        GearKitEntity::class,
        GearKitItemEntity::class,
        GearMaintenanceEntryEntity::class,
    ],
    version = 9,
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
    abstract fun gearKitDao(): GearKitDao
    abstract fun gearMaintenanceDao(): GearMaintenanceDao
}
