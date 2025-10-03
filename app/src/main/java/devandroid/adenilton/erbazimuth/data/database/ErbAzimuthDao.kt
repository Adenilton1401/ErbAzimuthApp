package devandroid.adenilton.erbazimuth.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.Erb
import devandroid.adenilton.erbazimuth.data.model.ErbWithAzimutes
import kotlinx.coroutines.flow.Flow

@Dao
interface ErbAzimuthDao {

    // A anotação @Transaction garante a consistência ao buscar a ERB e seus azimutes.
    @Transaction
    @Query("SELECT * FROM erbs")
    fun getAllErbsWithAzimutes(): Flow<List<ErbWithAzimutes>>

    @Query("SELECT * FROM erbs WHERE identificacao = :identificacao LIMIT 1")
    suspend fun getErbByIdentificacao(identificacao: String): Erb?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErb(erb: Erb): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAzimute(azimute: Azimute)
}

