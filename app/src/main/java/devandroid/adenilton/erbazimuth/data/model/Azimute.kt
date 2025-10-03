package devandroid.adenilton.erbazimuth.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "azimutes",
    foreignKeys = [ForeignKey(
        entity = Erb::class,
        parentColumns = ["id"],
        childColumns = ["erbOwnerId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Azimute(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // Este é o campo que armazena a chave estrangeira para a ERB.
    // O valor padrão 0L é um placeholder temporário.
    val erbOwnerId: Long = 0L,
    val descricao: String,
    val azimute: Double,
    val raio: Double,
    // Cor armazenada como Long para compatibilidade
    val cor: Long
)

