package devandroid.adenilton.erbazimuth.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "erbs", indices = [Index(value = ["identificacao"], unique = true)])
data class Erb(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val identificacao: String,
    val latitude: Double,
    val longitude: Double
)
