package com.janhorak.shutterdeck.core.storage

import android.content.Context
import com.janhorak.shutterdeck.core.data.db.GearItemDao
import com.janhorak.shutterdeck.core.data.db.LocationDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferencePhotoGrantManager @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val gearItemDao: GearItemDao,
    private val locationDao: LocationDao,
) {
    suspend fun updateGearReferencePhotoGrant(
        itemId: Long,
        previousUriString: String,
        nextUriString: String,
    ): Boolean = updateReferencePhotoGrant(
        previousUriString = previousUriString,
        nextUriString = nextUriString,
        excludingGearItemId = itemId,
    )

    suspend fun releaseGearReferencePhotoGrant(
        uriString: String,
        excludingItemId: Long,
    ) {
        releaseReferencePhotoGrant(
            uriString = uriString,
            excludingGearItemId = excludingItemId,
        )
    }

    suspend fun updateLocationReferencePhotoGrant(
        locationId: Long,
        previousUriString: String,
        nextUriString: String,
    ): Boolean = updateReferencePhotoGrant(
        previousUriString = previousUriString,
        nextUriString = nextUriString,
        excludingLocationId = locationId,
    )

    suspend fun releaseLocationReferencePhotoGrant(
        uriString: String,
        excludingLocationId: Long,
    ) {
        releaseReferencePhotoGrant(
            uriString = uriString,
            excludingLocationId = excludingLocationId,
        )
    }

    private suspend fun updateReferencePhotoGrant(
        previousUriString: String,
        nextUriString: String,
        excludingGearItemId: Long = Long.MIN_VALUE,
        excludingLocationId: Long = Long.MIN_VALUE,
    ): Boolean = withContext(Dispatchers.IO) {
        val previous = previousUriString.trim()
        val next = nextUriString.trim()
        if (previous == next) return@withContext true

        val resolver = appContext.contentResolver
        if (!resolver.persistReadPermissionIfNeeded(next)) return@withContext false

        if (!isReferencePhotoStillUsedAnywhere(previous, excludingGearItemId, excludingLocationId)) {
            resolver.releasePersistedReadPermissionIfHeld(previous)
        }
        true
    }

    private suspend fun releaseReferencePhotoGrant(
        uriString: String,
        excludingGearItemId: Long = Long.MIN_VALUE,
        excludingLocationId: Long = Long.MIN_VALUE,
    ) {
        withContext(Dispatchers.IO) {
            val trimmedUri = uriString.trim()
            if (trimmedUri.isBlank()) return@withContext

            if (!isReferencePhotoStillUsedAnywhere(trimmedUri, excludingGearItemId, excludingLocationId)) {
                appContext.contentResolver.releasePersistedReadPermissionIfHeld(trimmedUri)
            }
        }
    }

    private suspend fun isReferencePhotoStillUsedAnywhere(
        uriString: String,
        excludingGearItemId: Long,
        excludingLocationId: Long,
    ): Boolean {
        val trimmedUri = uriString.trim()
        if (trimmedUri.isBlank()) return false

        return gearItemDao.countByReferencePhotoUriExcludingId(trimmedUri, excludingGearItemId) > 0 ||
            locationDao.countByReferencePhotoUriExcludingId(trimmedUri, excludingLocationId) > 0
    }
}
