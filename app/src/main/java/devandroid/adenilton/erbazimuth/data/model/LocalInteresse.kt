package devandroid.adenilton.erbazimuth.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "locais_interesse",
    foreignKeys = [ForeignKey(
        entity = Caso::class,
        parentColumns = ["id"],
        childColumns = ["casoId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["casoId"])]
)
data class LocalInteresse(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val casoId: Long,
    val nome: String,
    val endereco: String,
    val latitude: Double,
    val longitude: Double
)

