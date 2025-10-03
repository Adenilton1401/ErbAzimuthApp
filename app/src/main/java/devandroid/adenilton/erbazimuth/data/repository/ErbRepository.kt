package devandroid.adenilton.erbazimuth.data.repository

import android.util.Log
import devandroid.adenilton.erbazimuth.data.database.ErbAzimuthDao
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.Erb
import devandroid.adenilton.erbazimuth.data.model.ErbWithAzimutes
import kotlinx.coroutines.flow.Flow

class ErbRepository(private val dao: ErbAzimuthDao) {

    fun getAllErbsWithAzimutes(): Flow<List<ErbWithAzimutes>> {
        return dao.getAllErbsWithAzimutes()
    }

    suspend fun insertErbAndAzimuth(erb: Erb, azimute: Azimute) {
        // Adiciona um log para rastrear a chamada ao repositório
        Log.d("ErbAzimuthApp", "Repository: Inserindo ERB e Azimute. ERB ID: ${erb.identificacao}")

        // Verifica se uma ERB com a mesma identificação já existe
        val existingErb = dao.getErbByIdentificacao(erb.identificacao)

        if (existingErb != null) {
            // A ERB já existe, então apenas adiciona o novo azimute a ela
            val newAzimute = azimute.copy(erbOwnerId = existingErb.id)
            dao.insertAzimute(newAzimute)
            Log.d("ErbAzimuthApp", "Repository: ERB existente encontrada. Adicionando novo azimute a ERB ID: ${existingErb.id}")
        } else {
            // A ERB é nova, insere a ERB primeiro para obter seu ID
            val newErbId = dao.insertErb(erb)
            // Agora, cria uma cópia do azimute com o ID da ERB recém-criada
            val newAzimute = azimute.copy(erbOwnerId = newErbId)
            dao.insertAzimute(newAzimute)
            Log.d("ErbAzimuthApp", "Repository: Nova ERB inserida com ID: $newErbId. Inserindo azimute associado.")
        }
    }
}

