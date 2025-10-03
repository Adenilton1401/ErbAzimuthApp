package devandroid.adenilton.erbazimuth.data.database

import androidx.room.*
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.Erb
import devandroid.adenilton.erbazimuth.data.model.ErbWithAzimutes
import kotlinx.coroutines.flow.Flow

@Dao
interface ErbAzimuthDao {

    @Transaction
    @Query("SELECT * FROM erbs")
    fun getAllErbsWithAzimutes(): Flow<List<ErbWithAzimutes>>

    @Query("SELECT * FROM erbs WHERE identificacao = :identificacao LIMIT 1")
    suspend fun getErbByIdentificacao(identificacao: String): Erb?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErb(erb: Erb): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAzimute(azimute: Azimute)

    // --- NOVOS MÉTODOS PARA EXCLUSÃO ---
    @Delete
    suspend fun deleteErb(erb: Erb)

    @Delete
    suspend fun deleteAzimute(azimute: Azimute)
}

