package com.janhorak.shutterdeck.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/** Root Room database for ShutterDeck. Add new entities and DAOs here as features land. */
@Database(
    entities = [ScenePresetEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scenePresetDao(): ScenePresetDao
}
