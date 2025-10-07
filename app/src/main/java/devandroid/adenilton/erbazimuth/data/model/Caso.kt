package devandroid.adenilton.erbazimuth.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "casos")
data class Caso(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val dataCriacao: Long = System.currentTimeMillis()
)
