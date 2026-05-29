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
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scenePresetDao(): ScenePresetDao
    abstract fun locationDao(): LocationDao
    abstract fun shootDao(): ShootDao
    abstract fun gearItemDao(): GearItemDao
}
