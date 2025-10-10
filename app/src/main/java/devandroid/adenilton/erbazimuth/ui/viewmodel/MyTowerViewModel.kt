package devandroid.adenilton.erbazimuth.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.maps.model.LatLng
import devandroid.adenilton.erbazimuth.data.model.CellTowerInfo
import devandroid.adenilton.erbazimuth.data.repository.ErbRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyTowerViewModel(application: Application, private val repository: ErbRepository) : AndroidViewModel(application) {

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    // --- ATUALIZADO: Agora armazena uma lista de torres ---
    private val _cellTowerInfoList = MutableStateFlow<List<CellTowerInfo>>(emptyList())
    val cellTowerInfoList: StateFlow<List<CellTowerInfo>> = _cellTowerInfoList.asStateFlow()

    private val _towerLocationList = MutableStateFlow<List<LatLng>>(emptyList())
    val towerLocationList: StateFlow<List<LatLng>> = _towerLocationList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    fun startDataCollection() {
        viewModelScope.launch {
            _isLoading.value = true
            // Limpa os dados anteriores
            _cellTowerInfoList.value = emptyList()
            _towerLocationList.value = emptyList()
            fetchUserLocation()
            fetchCellTowerInfoAndLocation()
            _isLoading.value = false
        }
    }

    private suspend fun fetchUserLocation() {
        val context = getApplication<Application>().applicationContext
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
                _userLocation.value = location
            } catch (e: SecurityException) { /* ... */ }
        }
    }

    // Mapeia o código da operadora (MNC) para o nome
    private fun mapMncToOperatorName(mcc: Int, mnc: Int): String {
        // Mapeamento para operadoras brasileiras (MCC = 724)
        if (mcc == 724) {
            return when (mnc) {
                2, 3, 4 -> "TIM"
                5, 38 -> "Claro"
                6, 10, 11 -> "Vivo"
                16, 31 -> "Oi"
                8 -> "Nextel"
                else -> "Outra ($mnc)"
            }
        }
        return "Desconhecido ($mnc)"
    }

    // --- ATUALIZADO: Busca todas as torres registradas ---
    private suspend fun fetchCellTowerInfoAndLocation() {
        val context = getApplication<Application>().applicationContext
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val cellInfoList = telephonyManager.allCellInfo
            val towerInfos = mutableListOf<CellTowerInfo>()
            val towerLocations = mutableListOf<LatLng>()

            for (cellInfo in cellInfoList) {
                if (cellInfo.isRegistered) {
                    val info: CellTowerInfo? = when (cellInfo) {
                        is CellInfoLte -> {
                            val identity = cellInfo.cellIdentity
                            val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) identity.mccString?.toIntOrNull() ?: 0 else @Suppress("DEPRECATION") identity.mcc
                            val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) identity.mncString?.toIntOrNull() ?: 0 else @Suppress("DEPRECATION") identity.mnc
                            CellTowerInfo(mcc, mnc, identity.tac, identity.ci, cellInfo.cellSignalStrength.dbm, mapMncToOperatorName(mcc, mnc))
                        }
                        is CellInfoGsm -> {
                            val identity = cellInfo.cellIdentity
                            val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) identity.mccString?.toIntOrNull() ?: 0 else @Suppress("DEPRECATION") identity.mcc
                            val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) identity.mncString?.toIntOrNull() ?: 0 else @Suppress("DEPRECATION") identity.mnc
                            CellTowerInfo(mcc, mnc, identity.lac, identity.cid, cellInfo.cellSignalStrength.dbm, mapMncToOperatorName(mcc, mnc))
                        }
                        is CellInfoWcdma -> {
                            val identity = cellInfo.cellIdentity
                            val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) identity.mccString?.toIntOrNull() ?: 0 else @Suppress("DEPRECATION") identity.mcc
                            val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) identity.mncString?.toIntOrNull() ?: 0 else @Suppress("DEPRECATION") identity.mnc
                            CellTowerInfo(mcc, mnc, identity.lac, identity.cid, cellInfo.cellSignalStrength.dbm, mapMncToOperatorName(mcc, mnc))
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
}

class MyTowerViewModelFactory(
    private val application: Application,
    private val repository: ErbRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyTowerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MyTowerViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

