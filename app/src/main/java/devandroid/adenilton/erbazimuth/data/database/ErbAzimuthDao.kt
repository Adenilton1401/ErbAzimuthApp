package devandroid.adenilton.erbazimuth.data.database

import androidx.room.*
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.Caso
import devandroid.adenilton.erbazimuth.data.model.Erb
import devandroid.adenilton.erbazimuth.data.model.ErbWithAzimutes
import kotlinx.coroutines.flow.Flow

@Dao
interface ErbAzimuthDao {
    // --- Métodos para Casos ---
    @Insert
    suspend fun insertCaso(caso: Caso)

    @Query("SELECT * FROM casos ORDER BY dataCriacao DESC")
    fun getAllCasos(): Flow<List<Caso>>

    @Delete
    suspend fun deleteCaso(caso: Caso)

    // --- NOVO MÉTOD ---
    @Update
    suspend fun updateCaso(caso: Caso)


    // --- Métodos para ERB e Azimute ---
    @Transaction
    @Query("SELECT * FROM erbs WHERE casoId = :casoId")
    fun getErbsForCase(casoId: Long): Flow<List<ErbWithAzimutes>>

    @Query("SELECT * FROM erbs WHERE identificacao = :identificacao AND casoId = :casoId LIMIT 1")
    suspend fun getErbByIdentificacao(identificacao: String, casoId: Long): Erb?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErb(erb: Erb): Long

    @Update
    suspend fun updateErb(erb: Erb)

    @Delete
    suspend fun deleteErb(erb: Erb)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAzimute(azimute: Azimute)

    @Update
    suspend fun updateAzimute(azimute: Azimute)

    @Delete
    suspend fun deleteAzimute(azimute: Azimute)
}

