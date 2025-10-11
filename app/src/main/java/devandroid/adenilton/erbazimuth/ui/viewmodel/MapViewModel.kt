package devandroid.adenilton.erbazimuth.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Address
import android.location.Location
import android.os.Build
import android.provider.MediaStore
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import devandroid.adenilton.erbazimuth.data.model.*
import devandroid.adenilton.erbazimuth.data.repository.ErbRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.io.OutputStream

class MapViewModel(
    private val application: Application,
    private val repository: ErbRepository,
    private val casoId: Long
) : AndroidViewModel(application) {

    // --- Estados da Tela Principal ---
    private val _erbsWithAzimutes = MutableStateFlow<List<ErbWithAzimutes>>(emptyList())
    val erbsWithAzimutes: StateFlow<List<ErbWithAzimutes>> = _erbsWithAzimutes.asStateFlow()

    private val _locaisInteresse = MutableStateFlow<List<LocalInteresse>>(emptyList())
    val locaisInteresse: StateFlow<List<LocalInteresse>> = _locaisInteresse.asStateFlow()

    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    private val _newPointEvent = MutableSharedFlow<LatLng>()
    val newPointEvent: SharedFlow<LatLng> = _newPointEvent.asSharedFlow()

    private val _selectedItem = MutableStateFlow<Any?>(null)
    val selectedItem: StateFlow<Any?> = _selectedItem.asStateFlow()

    private val _itemToDelete = MutableStateFlow<Any?>(null)
    val itemToDelete: StateFlow<Any?> = _itemToDelete.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _searchedAddress = MutableStateFlow<Address?>(null)
    val searchedAddress: StateFlow<Address?> = _searchedAddress.asStateFlow()

    private val _isSearchingAddress = MutableStateFlow(false)
    val isSearchingAddress: StateFlow<Boolean> = _isSearchingAddress.asStateFlow()

    private var googleMap: GoogleMap? = null

    // --- Estados da Camada "Minha Torre" ---
    private val _isMyTowerLayerVisible = MutableStateFlow(false)
    val isMyTowerLayerVisible: StateFlow<Boolean> = _isMyTowerLayerVisible.asStateFlow()

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private val _cellTowerInfoList = MutableStateFlow<List<CellTowerInfo>>(emptyList())
    val cellTowerInfoList: StateFlow<List<CellTowerInfo>> = _cellTowerInfoList.asStateFlow()

    private val _towerLocationList = MutableStateFlow<List<LatLng>>(emptyList())
    val towerLocationList: StateFlow<List<LatLng>> = _towerLocationList.asStateFlow()

    private val _isMyTowerLoading = MutableStateFlow(false)
    val isMyTowerLoading: StateFlow<Boolean> = _isMyTowerLoading.asStateFlow()

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    init {
        viewModelScope.launch { repository.getErbsForCase(casoId).collect { _erbsWithAzimutes.value = it } }
        viewModelScope.launch { repository.getLocaisInteresseForCase(casoId).collect { _locaisInteresse.value = it } }
    }

    // --- Lógica da Camada "Minha Torre" ---
    fun toggleMyTowerLayer() {
        _isMyTowerLayerVisible.value = !_isMyTowerLayerVisible.value
        if (_isMyTowerLayerVisible.value) {
            startDataCollection()
        }
    }

    private fun startDataCollection() {
        viewModelScope.launch {
            _isMyTowerLoading.value = true
            _cellTowerInfoList.value = emptyList()
            _towerLocationList.value = emptyList()
            fetchUserLocation()
            fetchCellTowerInfoAndLocation()
            _isMyTowerLoading.value = false
        }
    }

    private suspend fun fetchUserLocation() {
        if (ActivityCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                _userLocation.value = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
            } catch (e: SecurityException) { /* Ignorado */ }
        }
    }

    private fun mapMncToOperatorName(mcc: Int, mnc: Int): String {
        if (mcc == 724) {
            return when (mnc) {
                2, 3, 4 -> "TIM"; 5, 38 -> "Claro"; 6, 10, 11 -> "Vivo"; 16, 31 -> "Oi"; 8 -> "Nextel"; else -> "Outra ($mnc)"
            }
        }
        return "Desconhecido ($mnc)"
    }

    private suspend fun fetchCellTowerInfoAndLocation() {
        val telephonyManager = application.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (ActivityCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val cellInfoList = telephonyManager.allCellInfo
            val towerInfos = mutableListOf<CellTowerInfo>()
            val towerLocations = mutableListOf<LatLng>()
            for (cellInfo in cellInfoList) {
                if (cellInfo.isRegistered) {
                    val info: CellTowerInfo? = when (cellInfo) {
                        is CellInfoLte -> {
                            val id = cellInfo.cellIdentity
                            val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString?.toIntOrNull()?:0 else @Suppress("DEPRECATION") id.mcc
                            val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString?.toIntOrNull()?:0 else @Suppress("DEPRECATION") id.mnc
                            CellTowerInfo(mcc, mnc, id.tac, id.ci, cellInfo.cellSignalStrength.dbm, mapMncToOperatorName(mcc, mnc))
                        }
                        is CellInfoGsm -> {
                            val id = cellInfo.cellIdentity
                            val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString?.toIntOrNull()?:0 else @Suppress("DEPRECATION") id.mcc
                            val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString?.toIntOrNull()?:0 else @Suppress("DEPRECATION") id.mnc
                            CellTowerInfo(mcc, mnc, id.lac, id.cid, cellInfo.cellSignalStrength.dbm, mapMncToOperatorName(mcc, mnc))
                        }
                        is CellInfoWcdma -> {
                            val id = cellInfo.cellIdentity
                            val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString?.toIntOrNull()?:0 else @Suppress("DEPRECATION") id.mcc
                            val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString?.toIntOrNull()?:0 else @Suppress("DEPRECATION") id.mnc
                            CellTowerInfo(mcc, mnc, id.lac, id.cid, cellInfo.cellSignalStrength.dbm, mapMncToOperatorName(mcc, mnc))
                        }
                        else -> null
                    }
                    if (info != null && info.mcc != 0) {
                        towerInfos.add(info)
                        val towerData = repository.getTowerLocation(info)
                        if (towerData?.status == "ok" && towerData.lat != null && towerData.lon != null) {
                            towerLocations.add(LatLng(towerData.lat, towerData.lon))
                        }
                    }
                }
            }
            _cellTowerInfoList.value = towerInfos
            _towerLocationList.value = towerLocations
        }
    }

    fun onMapLoaded(map: GoogleMap) { this.googleMap = map }
    fun captureMapSnapshot(context: Context) {
        googleMap?.snapshot { bitmap ->
            if (bitmap != null) { saveBitmapToGallery(context, bitmap) }
            else { viewModelScope.launch { _snackbarMessage.emit("Falha ao capturar imagem do mapa.") } }
        }
    }
    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            val displayName = "map_snapshot_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ErbAzimuth")
                }
            }
            val resolver = context.contentResolver
            var stream: OutputStream? = null
            val uri = try { resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) } catch (e: Exception) { null }
            if (uri == null) { _snackbarMessage.emit("Erro ao preparar para salvar a imagem."); return@launch }
            try {
                stream = resolver.openOutputStream(uri)
                if (stream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream); _snackbarMessage.emit("Imagem salva na galeria!")
                } else { throw IOException("Não foi possível abrir o stream de saída.") }
            } catch (e: Exception) {
                resolver.delete(uri, null, null); _snackbarMessage.emit("Erro ao salvar imagem.")
            } finally { stream?.close() }
        }
    }
    fun onAddErbRequest() { _dialogState.value = DialogState.AddErbAndAzimuth }
    fun onAddAzimuthRequest(erb: Erb) { _selectedItem.value = null; _dialogState.value = DialogState.AddAzimuth(erb) }
    fun onAddLocalInteresseRequest() { _dialogState.value = DialogState.AddLocalInteresse }
    fun onEditRequest(item: Any) {
        _selectedItem.value = null
        _dialogState.value = when(item) {
            is Erb -> DialogState.EditErb(item)
            is Azimute -> {
                val parentErb = _erbsWithAzimutes.value.find { it.azimutes.any { az -> az.id == item.id } }?.erb
                if (parentErb != null) DialogState.EditAzimuth(item, parentErb) else DialogState.Hidden
            }
            is LocalInteresse -> DialogState.EditLocalInteresse(item)
            else -> DialogState.Hidden
        }
    }
    fun onDismissDialog() { _dialogState.value = DialogState.Hidden; _searchedAddress.value = null }
    fun onConfirmAdd(erb: Erb, azimute: Azimute) {
        viewModelScope.launch {
            val erbForCase = erb.copy(casoId = casoId)
            val newErbId = repository.insertErbAndAzimuth(erbForCase, azimute)
            if (newErbId != -1L) { _newPointEvent.emit(LatLng(erb.latitude, erb.longitude)) }
            onDismissDialog()
        }
    }
    fun onConfirmEdit(item: Any) {
        viewModelScope.launch {
            when (item) {
                is Erb -> {
                    val updatedErb = item.copy(casoId = casoId); repository.updateErb(updatedErb)
                    _newPointEvent.emit(LatLng(item.latitude, item.longitude))
                }
                is Azimute -> repository.updateAzimute(item)
                is LocalInteresse -> repository.updateLocalInteresse(item)
            }
            onDismissDialog()
        }
    }
    fun onSearchAddress(addressString: String) {
        viewModelScope.launch {
            _isSearchingAddress.value = true; _searchedAddress.value = null
            val result = repository.getCoordinatesFromAddress(addressString)
            _searchedAddress.value = result
            if (result == null) { _snackbarMessage.emit("Endereço não encontrado.") }
            _isSearchingAddress.value = false
        }
    }
    fun onConfirmAddLocal(nome: String) {
        viewModelScope.launch {
            _searchedAddress.value?.let { address ->
                val newLocal = LocalInteresse(
                    casoId = casoId, nome = nome, endereco = address.getAddressLine(0) ?: "",
                    latitude = address.latitude, longitude = address.longitude
                )
                repository.insertLocalInteresse(newLocal)
                _snackbarMessage.emit("Local de interesse adicionado!")
                _newPointEvent.emit(LatLng(newLocal.latitude, newLocal.longitude))
                onDismissDialog()
            }
        }
    }
    fun onDeleteRequest(item: Any) { _itemToDelete.value = item }
    fun onConfirmDelete() {
        viewModelScope.launch {
            _itemToDelete.value?.let { item ->
                when (item) {
                    is Erb -> repository.deleteErb(item)
                    is Azimute -> repository.deleteAzimute(item)
                    is LocalInteresse -> repository.deleteLocalInteresse(item)
                }
            }
            _itemToDelete.value = null; _selectedItem.value = null
        }
    }
    fun onDismissDelete() { _itemToDelete.value = null }
    fun onItemSelected(item: Any) { _selectedItem.value = item }
    fun onDismissDetails() { _selectedItem.value = null }
}


// --- CLASSE SELADA ADICIONADA AQUI ---
sealed class DialogState {
    object Hidden : DialogState()
    object AddErbAndAzimuth : DialogState()
    data class AddAzimuth(val erb: Erb) : DialogState()
    data class EditErb(val erb: Erb) : DialogState()
    data class EditAzimuth(val azimute: Azimute, val parentErb: Erb) : DialogState()
    object AddLocalInteresse : DialogState()
    data class EditLocalInteresse(val local: LocalInteresse) : DialogState()
}


class MapViewModelFactory(
    private val application: Application,
    private val repository: ErbRepository,
    private val casoId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(application, repository, casoId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

