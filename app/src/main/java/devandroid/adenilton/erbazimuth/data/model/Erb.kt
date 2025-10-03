package devandroid.adenilton.erbazimuth.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ATUALIZADO: Define o nome da tabela explicitamente para "erbs"
@Entity(tableName = "erbs")
data class Erb(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val identificacao: String,
    val latitude: Double,
    val longitude: Double,
    val endereco: String? = null
)

