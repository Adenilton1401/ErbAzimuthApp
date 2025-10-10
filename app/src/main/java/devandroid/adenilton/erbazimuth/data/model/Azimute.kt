package devandroid.adenilton.erbazimuth.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = Erb::class,
        parentColumns = ["id"],
        childColumns = ["erbOwnerId"],
        onDelete = ForeignKey.CASCADE
    )],
    // ATUALIZADO: Adiciona o índice para otimizar as consultas
    indices = [Index(value = ["erbOwnerId"])]
)
data class Azimute(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val erbOwnerId: Long = 0L,
    val descricao: String,
    val azimute: Double,
    val raio: Double,
    val cor: Long
)

