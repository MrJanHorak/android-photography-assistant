package com.janhorak.shutterdeck.film.data

import android.content.Context
import com.janhorak.shutterdeck.core.data.db.FilmStockEntity
import org.json.JSONArray
import org.json.JSONObject

internal data class FilmStockCatalogEntry(
    val id: String,
    val brand: String,
    val name: String,
    val format: String,
    val stockType: String,
    val iso: Int,
    val reciprocityExponent: Double?,
    val reciprocityStartsAtSeconds: Double?,
    val processingType: String,
    val developerNotes: String,
    val description: String,
    val maxPushStops: Int?,
    val maxPullStops: Int?,
) {
    fun toEntity(): FilmStockEntity {
        return FilmStockEntity(
            id = id,
            brand = brand,
            name = name,
            format = format,
            stockType = stockType,
            iso = iso,
            reciprocityExponent = reciprocityExponent,
            reciprocityStartsAtSeconds = reciprocityStartsAtSeconds,
            processingType = processingType,
            developerNotes = developerNotes,
            description = description,
            maxPushStops = maxPushStops,
            maxPullStops = maxPullStops,
            isBuiltIn = true,
            createdAt = 0L,
        )
    }
}

internal object FilmStockCatalogLoader {
    private const val ASSET_FILE_NAME = "film_stock_catalog.json"

    fun loadBundledStocks(context: Context): List<FilmStockEntity> {
        return runCatching {
            context.assets.open(ASSET_FILE_NAME).bufferedReader().use { reader ->
                parse(reader.readText())
            }
        }.getOrElse { fallbackFilmStockCatalog }
            .map(FilmStockCatalogEntry::toEntity)
    }

    internal fun parse(json: String): List<FilmStockCatalogEntry> {
        val root = runCatching { JSONObject(json) }
            .getOrElse { throwable ->
                throw IllegalArgumentException("Film stock catalog is not valid JSON.", throwable)
            }
        val stockArray = root.optJSONArray("stocks")
            ?: throw IllegalArgumentException("Film stock catalog JSON must include a stocks array.")

        if (stockArray.length() == 0) {
            throw IllegalArgumentException("Film stock catalog JSON must include at least one stock entry.")
        }

        val parsedStocks = stockArray.toFilmStockCatalogEntries()
        validateUniqueIds(parsedStocks.map(FilmStockCatalogEntry::id))
        return parsedStocks
    }

    private fun JSONArray.toFilmStockCatalogEntries(): List<FilmStockCatalogEntry> {
        return List(length()) { index ->
            runCatching { getJSONObject(index).toFilmStockCatalogEntry() }
                .getOrElse { throwable ->
                    throw IllegalArgumentException("stocks[$index] is invalid: ${throwable.message}", throwable)
                }
        }
    }

    private fun JSONObject.toFilmStockCatalogEntry(): FilmStockCatalogEntry {
        val id = getString("id").trim()
        val brand = getString("brand").trim()
        val name = getString("name").trim()
        val format = getString("format").trim()
        val stockType = getString("stockType").trim()
        val iso = getInt("iso")
        val reciprocityExponent = optNullableDouble("reciprocityExponent")
        val reciprocityStartsAtSeconds = optNullableDouble("reciprocityStartsAtSeconds")
        val processingType = getString("processingType").trim()
        val developerNotes = optString("developerNotes", "").trim()
        val description = optString("description", "").trim()
        val maxPushStops = optNullableInt("maxPushStops")
        val maxPullStops = optNullableInt("maxPullStops")

        require(id.isNotBlank()) { "id must not be blank." }
        require(brand.isNotBlank()) { "brand must not be blank." }
        require(name.isNotBlank()) { "name must not be blank." }
        require(format.isNotBlank()) { "format must not be blank." }
        require(stockType.isNotBlank()) { "stockType must not be blank." }
        require(iso > 0) { "iso must be greater than 0." }
        require(processingType.isNotBlank()) { "processingType must not be blank." }

        if (reciprocityExponent != null || reciprocityStartsAtSeconds != null) {
            require(reciprocityExponent != null) { "reciprocityExponent must be provided when reciprocityStartsAtSeconds is set." }
            require(reciprocityStartsAtSeconds != null) { "reciprocityStartsAtSeconds must be provided when reciprocityExponent is set." }
            require(reciprocityExponent > 0.0 && reciprocityExponent.isFinite()) {
                "reciprocityExponent must be greater than 0."
            }
            require(reciprocityStartsAtSeconds > 0.0 && reciprocityStartsAtSeconds.isFinite()) {
                "reciprocityStartsAtSeconds must be greater than 0."
            }
        }

        require(maxPushStops == null || maxPushStops >= 0) { "maxPushStops must be 0 or greater when set." }
        require(maxPullStops == null || maxPullStops >= 0) { "maxPullStops must be 0 or greater when set." }

        return FilmStockCatalogEntry(
            id = id,
            brand = brand,
            name = name,
            format = format,
            stockType = stockType,
            iso = iso,
            reciprocityExponent = reciprocityExponent,
            reciprocityStartsAtSeconds = reciprocityStartsAtSeconds,
            processingType = processingType,
            developerNotes = developerNotes,
            description = description,
            maxPushStops = maxPushStops,
            maxPullStops = maxPullStops,
        )
    }

    private fun JSONObject.optNullableDouble(name: String): Double? {
        if (!has(name) || isNull(name)) return null
        return getDouble(name)
    }

    private fun JSONObject.optNullableInt(name: String): Int? {
        if (!has(name) || isNull(name)) return null
        return getInt(name)
    }

    private fun validateUniqueIds(ids: List<String>) {
        val duplicates = ids.groupingBy { it }.eachCount().filterValues { count -> count > 1 }.keys
        require(duplicates.isEmpty()) { "Film stock catalog IDs must be unique. Duplicate IDs: ${duplicates.joinToString()}" }
    }

    private val fallbackFilmStockCatalog = listOf(
        FilmStockCatalogEntry(
            id = "kodak_portra_400",
            brand = "Kodak",
            name = "Portra 400",
            format = "Multi-format",
            stockType = "Color negative",
            iso = 400,
            reciprocityExponent = 1.11,
            reciprocityStartsAtSeconds = 1.0,
            processingType = "C-41",
            developerNotes = "C-41 reference: start around 3:15 at 38 C. Use your lab or chemistry sheet as the final source of truth.",
            description = "Flexible daylight-balanced color negative stock with wide latitude for portraits and travel.",
            maxPushStops = 2,
            maxPullStops = 1,
        ),
        FilmStockCatalogEntry(
            id = "ilford_hp5_plus_400",
            brand = "Ilford",
            name = "HP5 Plus 400",
            format = "Multi-format",
            stockType = "B&W negative",
            iso = 400,
            reciprocityExponent = 1.31,
            reciprocityStartsAtSeconds = 1.0,
            processingType = "B&W",
            developerNotes = "HP5+ is commonly pushed. Add your chosen developer, dilution and agitation pattern for each push level.",
            description = "Versatile black-and-white stock that tolerates mixed light and routine pushing well.",
            maxPushStops = 3,
            maxPullStops = 1,
        ),
        FilmStockCatalogEntry(
            id = "fujifilm_velvia_50",
            brand = "Fujifilm",
            name = "Velvia 50",
            format = "Multi-format",
            stockType = "Slide",
            iso = 50,
            reciprocityExponent = 1.42,
            reciprocityStartsAtSeconds = 1.0,
            processingType = "E-6",
            developerNotes = "Slide stock with tight latitude. Record any filter, reciprocity and scanning adjustments with care.",
            description = "Highly saturated slide film favored for landscape work and careful tripod exposures.",
            maxPushStops = 0,
            maxPullStops = 0,
        ),
        FilmStockCatalogEntry(
            id = "polaroid_color_i_type",
            brand = "Polaroid",
            name = "Color i-Type",
            format = "Instant",
            stockType = "Instant",
            iso = 640,
            reciprocityExponent = null,
            reciprocityStartsAtSeconds = null,
            processingType = "Instant",
            developerNotes = "Instant chemistry varies with temperature. Save your warm-up and shielding notes rather than a strict timer.",
            description = "Integral instant film with built-in chemistry and an exposure workflow that depends heavily on temperature.",
            maxPushStops = null,
            maxPullStops = null,
        ),
    )
}
