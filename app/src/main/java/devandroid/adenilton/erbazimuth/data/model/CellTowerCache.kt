package devandroid.adenilton.erbazimuth.data.model

import androidx.room.Entity

@Entity(tableName = "cell_tower_cache", primaryKeys = ["mcc", "mnc", "lac", "cid"])
data class CellTowerCache(
    val mcc: Int, // Mobile Country Code
    val mnc: Int, // Mobile Network Code (net)
    val lac: Long, // Location Area Code (area)
    val cid: Long, // Cell ID (cell)
    val lat: Double, // Latitude
    val lon: Double  // Longitude
)
