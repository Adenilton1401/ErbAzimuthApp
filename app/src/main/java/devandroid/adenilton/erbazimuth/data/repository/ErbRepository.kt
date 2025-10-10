package devandroid.adenilton.erbazimuth.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import devandroid.adenilton.erbazimuth.BuildConfig
import devandroid.adenilton.erbazimuth.data.database.ErbAzimuthDao
import devandroid.adenilton.erbazimuth.data.model.*
import devandroid.adenilton.erbazimuth.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume

// --- NOVAS CLASSES PARA O FORMATO DA API V2 (POST) ---
data class CellLocationRequest(
    val token: String,
    val radio: String,
    val mcc: Int,
    val mnc: Int,
    val cells: List<CellTower>,
    val address: Int = 1
)

data class CellTower(
    val lac: Int,
    val cid: Int
)

data class CellLocationResponse(
    val status: String,
    val message: String?,
    val lat: Double?,
    val lon: Double?
)

// --- INTERFACE RETROFIT ATUALIZADA ---
interface UnwiredLabsService {
    @POST("v2/process.php")
    suspend fun getCellLocation(@Body request: CellLocationRequest): Response<CellLocationResponse>
}


class ErbRepository(private val dao: ErbAzimuthDao, private val context: Context) {

    private val unwiredLabsService: UnwiredLabsService = Retrofit.Builder()
        // ATUALIZADO: URL base correta conforme a documentação v2
        .baseUrl("https://us1.unwiredlabs.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(UnwiredLabsService::class.java)

    // --- MÉTODO ATUALIZADO PARA USAR A NOVA API ---
    suspend fun getTowerLocation(info: CellTowerInfo): CellLocationResponse? {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.e("ErbAzimuthApp", "Repository: Sem conexão com a internet.")
            return CellLocationResponse("error", "Sem conexão com a internet", null, null)
        }

        Log.d("ErbAzimuthApp", "Repository: Buscando localização para a torre via API v2: MCC=${info.mcc}, MNC=${info.mnc}, LAC=${info.lac}, CID=${info.cid}")

        // Constrói o corpo da requisição POST
        val request = CellLocationRequest(
            token = BuildConfig.OPENCELLID_API_KEY,
            radio = "gsm", // A API aceita "gsm", "cdma", "umts", "lte". "gsm" é um bom padrão.
            mcc = info.mcc,
            mnc = info.mnc,
            cells = listOf(CellTower(lac = info.lac, cid = info.cid))
        )

        return try {
            val response = unwiredLabsService.getCellLocation(request)
            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.d("ErbAzimuthApp", "Repository: Resposta da API com sucesso: $responseBody")
                responseBody
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ErbAzimuthApp", "Repository: Erro na API: Código=${response.code()}, Mensagem=${errorBody}")
                null
            }
        } catch (e: Exception) {
            Log.e("ErbAzimuthApp", "Repository: Falha na chamada de rede. Exceção: ${e.javaClass.simpleName}, Mensagem: ${e.message}", e)
            null
        }
    }

    // --- MÉTODOS EXISTENTES ---
    fun getAllCasos(): Flow<List<Caso>> = dao.getAllCasos()
    suspend fun insertCaso(caso: Caso) = withContext(Dispatchers.IO) { dao.insertCaso(caso) }
    suspend fun deleteCaso(caso: Caso) = withContext(Dispatchers.IO) { dao.deleteCaso(caso) }
    suspend fun updateCaso(caso: Caso) = withContext(Dispatchers.IO) { dao.updateCaso(caso) }
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
    fun getLocaisInteresseForCase(caseId: Long): Flow<List<LocalInteresse>> = dao.getLocaisInteresseForCase(caseId)
    suspend fun insertLocalInteresse(local: LocalInteresse) = withContext(Dispatchers.IO) { dao.insertLocalInteresse(local) }
    suspend fun updateLocalInteresse(local: LocalInteresse) = withContext(Dispatchers.IO) { dao.updateLocalInteresse(local) }
    suspend fun deleteLocalInteresse(local: LocalInteresse) = withContext(Dispatchers.IO) { dao.deleteLocalInteresse(local) }

    suspend fun getCoordinatesFromAddress(addressString: String): Address? {
        if (!NetworkUtils.isNetworkAvailable(context)) { return null }
        if (!Geocoder.isPresent()) { return null }
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

    private suspend fun getAddressFromCoordinates(lat: Double, lon: Double): String {
        if (!NetworkUtils.isNetworkAvailable(context)) { return "Sem conexão" }
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

