package com.janhorak.shutterdeck.film.data

import android.content.Context
import com.janhorak.shutterdeck.core.data.db.FilmStockDao
import com.janhorak.shutterdeck.core.data.db.FilmStockEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilmStockRepository @Inject constructor(
    private val dao: FilmStockDao,
    @ApplicationContext private val context: Context,
) {
    private val seedMutex = Mutex()

    @Volatile
    private var hasSeededBundledStocks = false

    fun observeStocks(): Flow<List<FilmStockEntity>> {
        return dao.observeAll().onStart { seedBundledStocks() }
    }

    suspend fun saveCustomStock(stock: FilmStockEntity) {
        require(!stock.isBuiltIn) { "Built-in film stocks are read-only." }
        dao.upsert(stock.copy(isBuiltIn = false))
    }

    suspend fun deleteCustomStock(stock: FilmStockEntity) {
        require(!stock.isBuiltIn) { "Built-in film stocks are read-only." }
        dao.delete(stock)
    }

    private suspend fun seedBundledStocks() {
        if (hasSeededBundledStocks) return
        seedMutex.withLock {
            if (hasSeededBundledStocks) return
            val bundledStocks = FilmStockCatalogLoader.loadBundledStocks(context)
            if (bundledStocks.isNotEmpty()) {
                dao.upsertAll(bundledStocks)
            }
            hasSeededBundledStocks = true
        }
    }
}
