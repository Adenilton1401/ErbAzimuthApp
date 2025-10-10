package devandroid.adenilton.erbazimuth.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import devandroid.adenilton.erbazimuth.data.database.ErbAzimuthDao
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.Caso
import devandroid.adenilton.erbazimuth.data.model.Erb
import devandroid.adenilton.erbazimuth.data.model.ErbWithAzimutes
import devandroid.adenilton.erbazimuth.data.model.LocalInteresse
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
    suspend fun updateCaso(caso: Caso) = withContext(Dispatchers.IO) { dao.updateCaso(caso) }

    // --- Métodos para ERB e Azimute ---
    fun getErbsForCase(caseId: Long): Flow<List<ErbWithAzimutes>> = dao.getErbsForCase(caseId)

    suspend fun insertErbAndAzimuth(erb: Erb, azimute: Azimute): Long {
        return withContext(Dispatchers.IO) {
            Log.d("ErbAzimuthApp", "Repository: Verificando se ERB '${erb.identificacao}' existe para o caso ${erb.casoId}.")
            val existingErb = dao.getErbByIdentificacao(erb.identificacao, erb.casoId)

            if (existingErb == null) {
                Log.d("ErbAzimuthApp", "Repository: ERB não encontrada. Inserindo nova.")
                val endereco = getAddressFromCoordinates(erb.latitude, erb.longitude)
                val erbComEndereco = erb.copy(endereco = endereco)
                val newErbId = dao.insertErb(erbComEndereco)
                Log.d("ErbAzimuthApp", "Repository: ERB inserida com novo ID: $newErbId.")

                val newAzimute = azimute.copy(erbOwnerId = newErbId)
                dao.insertAzimute(newAzimute)
                Log.d("ErbAzimuthApp", "Repository: Azimute inserido para a nova ERB.")
                newErbId
            } else {
                Log.d("ErbAzimuthApp", "Repository: ERB encontrada com ID: ${existingErb.id}. Inserindo apenas azimute.")
                val existingErbId = existingErb.id
                val newAzimute = azimute.copy(erbOwnerId = existingErbId)
                dao.insertAzimute(newAzimute)
                Log.d("ErbAzimuthApp", "Repository: Azimute inserido para ERB existente.")
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

    // --- Métodos para Locais de Interesse ---
    fun getLocaisInteresseForCase(caseId: Long): Flow<List<LocalInteresse>> = dao.getLocaisInteresseForCase(caseId)
    suspend fun insertLocalInteresse(local: LocalInteresse) = withContext(Dispatchers.IO) { dao.insertLocalInteresse(local) }
    suspend fun updateLocalInteresse(local: LocalInteresse) = withContext(Dispatchers.IO) { dao.updateLocalInteresse(local) }
    suspend fun deleteLocalInteresse(local: LocalInteresse) = withContext(Dispatchers.IO) { dao.deleteLocalInteresse(local) }

    // Geocodificação direta (Endereço -> Coordenadas)
    suspend fun getCoordinatesFromAddress(addressString: String): Address? {
        if (!Geocoder.isPresent()) {
            Log.e("ErbAzimuthApp", "Geocoder não está disponível.")
            return null
        }
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(addressString, 1) { addresses ->
                        if (continuation.isActive) {
                            continuation.resume(addresses.firstOrNull())
                        }
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(addressString, 1)?.firstOrNull()
                }
            }
        } catch (e: Exception) {
            Log.e("ErbAzimuthApp", "Erro na geocodificação direta", e)
            null
        }
    }

    // Geocodificação reversa (Coordenadas -> Endereço)
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

