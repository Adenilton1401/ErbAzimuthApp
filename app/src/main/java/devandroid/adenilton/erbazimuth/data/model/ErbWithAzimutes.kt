package devandroid.adenilton.erbazimuth.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class ErbWithAzimutes(
    @Embedded val erb: Erb,
    @Relation(
        parentColumn = "id",
        entityColumn = "erbOwnerId"
    )
    val azimutes: List<Azimute>
)

