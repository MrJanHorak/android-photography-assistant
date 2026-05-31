package com.janhorak.shutterdeck.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.janhorak.shutterdeck.core.data.db.AppDatabase
import com.janhorak.shutterdeck.core.data.db.FilmRollDao
import com.janhorak.shutterdeck.core.data.db.FilmStockDao
import com.janhorak.shutterdeck.core.data.db.GearBatteryDao
import com.janhorak.shutterdeck.core.data.db.GearFilterDao
import com.janhorak.shutterdeck.core.data.db.GearKitDao
import com.janhorak.shutterdeck.core.data.db.GearItemDao
import com.janhorak.shutterdeck.core.data.db.GearLoanDao
import com.janhorak.shutterdeck.core.data.db.GearMaintenanceDao
import com.janhorak.shutterdeck.core.data.db.GearMemoryCardDao
import com.janhorak.shutterdeck.core.data.db.LightingSetupDao
import com.janhorak.shutterdeck.core.data.db.LocationDao
import com.janhorak.shutterdeck.core.data.db.ScenePresetDao
import com.janhorak.shutterdeck.core.data.db.ShotNoteDao
import com.janhorak.shutterdeck.core.data.db.ShootDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreDataModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("settings") },
        )

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "shutterdeck.db")
            .addMigrations(
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideScenePresetDao(database: AppDatabase): ScenePresetDao = database.scenePresetDao()

    @Provides
    fun provideLocationDao(database: AppDatabase): LocationDao = database.locationDao()

    @Provides
    fun provideShootDao(database: AppDatabase): ShootDao = database.shootDao()

    @Provides
    fun provideGearItemDao(database: AppDatabase): GearItemDao = database.gearItemDao()

    @Provides
    fun provideGearFilterDao(database: AppDatabase): GearFilterDao = database.gearFilterDao()

    @Provides
    fun provideGearBatteryDao(database: AppDatabase): GearBatteryDao = database.gearBatteryDao()

    @Provides
    fun provideGearMemoryCardDao(database: AppDatabase): GearMemoryCardDao = database.gearMemoryCardDao()

    @Provides
    fun provideGearLoanDao(database: AppDatabase): GearLoanDao = database.gearLoanDao()

    @Provides
    fun provideGearKitDao(database: AppDatabase): GearKitDao = database.gearKitDao()

    @Provides
    fun provideGearMaintenanceDao(database: AppDatabase): GearMaintenanceDao = database.gearMaintenanceDao()

    @Provides
    fun provideFilmStockDao(database: AppDatabase): FilmStockDao = database.filmStockDao()

    @Provides
    fun provideFilmRollDao(database: AppDatabase): FilmRollDao = database.filmRollDao()

    @Provides
    fun provideShotNoteDao(database: AppDatabase): ShotNoteDao = database.shotNoteDao()

    @Provides
    fun provideLightingSetupDao(database: AppDatabase): LightingSetupDao = database.lightingSetupDao()
}
