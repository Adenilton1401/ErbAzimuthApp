package devandroid.adenilton.erbazimuth.data.repository

import android.content.Context
import android.location.Geocoder
import android.os.Build
import devandroid.adenilton.erbazimuth.data.database.ErbAzimuthDao
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.Caso
import devandroid.adenilton.erbazimuth.data.model.Erb
import devandroid.adenilton.erbazimuth.data.model.ErbWithAzimutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

class ErbRepository(private val dao: ErbAzimuthDao, private val context: Context) {
    // --- Métodos para Casos ---
    fun getAllCasos(): Flow<List<Caso>> = dao.getAllCasos()
    suspend fun insertCaso(caso: Caso) = withContext(Dispatchers.IO) { dao.insertCaso(caso) }
    suspend fun deleteCaso(caso: Caso) = withContext(Dispatchers.IO) { dao.deleteCaso(caso) }
    // --- NOVO MÉTODO ---
    suspend fun updateCaso(caso: Caso) = withContext(Dispatchers.IO) { dao.updateCaso(caso) }

    // --- Métodos para ERB e Azimute ---
    fun getErbsForCase(caseId: Long): Flow<List<ErbWithAzimutes>> = dao.getErbsForCase(caseId)

    suspend fun insertErbAndAzimuth(erb: Erb, azimute: Azimute): Long {
        return withContext(Dispatchers.IO) {
            val existingErb = dao.getErbByIdentificacao(erb.identificacao, erb.casoId)
            if (existingErb == null) {
                val endereco = getAddressFromCoordinates(erb.latitude, erb.longitude)
                val erbComEndereco = erb.copy(endereco = endereco)
                val newErbId = dao.insertErb(erbComEndereco)
                val newAzimute = azimute.copy(erbOwnerId = newErbId)
                dao.insertAzimute(newAzimute)
                newErbId
            } else {
                val existingErbId = existingErb.id
                val newAzimute = azimute.copy(erbOwnerId = existingErbId)
                dao.insertAzimute(newAzimute)
                -1L
            }
        }
    }

    suspend fun updateErb(erb: Erb) {
        withContext(Dispatchers.IO) {
            val endereco = getAddressFromCoordinates(erb.latitude, erb.longitude)
            val erbComEndereco = erb.copy(endereco = endereco)
            dao.updateErb(erbComEndereco)
        }
    }

    suspend fun deleteErb(erb: Erb) { withContext(Dispatchers.IO) { dao.deleteErb(erb) } }
    suspend fun updateAzimute(azimute: Azimute) { withContext(Dispatchers.IO) { dao.updateAzimute(azimute) } }
    suspend fun deleteAzimute(azimute: Azimute) { withContext(Dispatchers.IO) { dao.deleteAzimute(azimute) } }

    private suspend fun getAddressFromCoordinates(lat: Double, lon: Double): String {
        if (!Geocoder.isPresent()) { return "Geocoder indisponível" }
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        val addressText = addresses.firstOrNull()?.let {
                            listOfNotNull(it.thoroughfare, it.subThoroughfare, it.subLocality, it.subAdminArea)
                                .filter { s -> s.isNotBlank() }
                                .joinToString(", ")
                        }?.ifBlank { "Endereço não encontrado" } ?: "Endereço não encontrado"
                        if (continuation.isActive) { continuation.resume(addressText) }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val addressText = addresses?.firstOrNull()?.let {
                    listOfNotNull(it.thoroughfare, it.subThoroughfare, it.subLocality, it.subAdminArea)
                        .filter { s -> s.isNotBlank() }
                        .joinToString(", ")
                }?.ifBlank { "Endereço não encontrado" } ?: "Endereço não encontrado"
                addressText
            }
        } catch (e: IOException) {
            "Erro de conexão ao buscar endereço"
        } catch (e: Exception) {
            "Erro ao buscar endereço"
        }
    }
}

