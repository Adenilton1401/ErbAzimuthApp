package devandroid.adenilton.erbazimuth.data.repository

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import devandroid.adenilton.erbazimuth.data.database.ErbAzimuthDao
import devandroid.adenilton.erbazimuth.data.model.Azimute
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

    fun getAllErbsWithAzimutes(): Flow<List<ErbWithAzimutes>> {
        return dao.getAllErbsWithAzimutes()
    }

    suspend fun insertErbAndAzimuth(erb: Erb, azimute: Azimute): Long {
        return withContext(Dispatchers.IO) {
            val existingErb = dao.getErbByIdentificacao(erb.identificacao)

            if (existingErb == null) {
                Log.d("ErbAzimuthApp", "Repository: ERB ${erb.identificacao} não existe. Buscando endereço...")
                val endereco = getAddressFromCoordinates(erb.latitude, erb.longitude)
                val erbComEndereco = erb.copy(endereco = endereco)
                Log.d("ErbAzimuthApp", "Repository: Endereço encontrado: $endereco. Inserindo nova ERB.")

                val newErbId = dao.insertErb(erbComEndereco)
                val newAzimute = azimute.copy(erbOwnerId = newErbId)
                dao.insertAzimute(newAzimute)
                newErbId
            } else {
                Log.d("ErbAzimuthApp", "Repository: ERB ${erb.identificacao} já existe. Adicionando apenas azimute.")
                val existingErbId = existingErb.id
                val newAzimute = azimute.copy(erbOwnerId = existingErbId)
                dao.insertAzimute(newAzimute)
                -1L
            }
        }
    }

    // --- MÉTODO CORRIGIDO ---
    private suspend fun getAddressFromCoordinates(lat: Double, lon: Double): String {
        if (!Geocoder.isPresent()) {
            Log.e("ErbAzimuthApp", "Geocoder não está disponível neste dispositivo.")
            return "Geocoder indisponível"
        }
        val geocoder = Geocoder(context, Locale.getDefault())

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // A nova API é assíncrona, usamos uma corrotina para esperar o resultado.
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        val addressText = addresses.firstOrNull()?.let {
                            listOfNotNull(it.thoroughfare, it.subThoroughfare, it.subLocality, it.subAdminArea)
                                .filter { s -> s.isNotBlank() }
                                .joinToString(", ")
                        }?.ifBlank { "Endereço não encontrado" } ?: "Endereço não encontrado"

                        // Garante que a corrotina ainda está ativa antes de retornar
                        if (continuation.isActive) {
                            continuation.resume(addressText)
                        }
                    }
                }
            } else {
                // A API antiga é síncrona e pode ser chamada diretamente.
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val addressText = addresses?.firstOrNull()?.let {
                    listOfNotNull(it.thoroughfare, it.subThoroughfare, it.subLocality, it.locality)
                        .filter { s -> s.isNotBlank() }
                        .joinToString(", ")
                }?.ifBlank { "Endereço não encontrado" } ?: "Endereço não encontrado"
                addressText
            }
        } catch (e: IOException) {
            Log.e("ErbAzimuthApp", "Erro de rede ao buscar endereço", e)
            "Erro de conexão ao buscar endereço"
        } catch (e: Exception) {
            Log.e("ErbAzimuthApp", "Erro inesperado no Geocoder", e)
            "Erro ao buscar endereço"
        }
    }
}

