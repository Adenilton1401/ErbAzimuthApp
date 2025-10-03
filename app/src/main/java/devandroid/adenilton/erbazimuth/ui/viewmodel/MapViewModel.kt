package devandroid.adenilton.erbazimuth.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.Erb
import devandroid.adenilton.erbazimuth.data.model.ErbWithAzimutes
import devandroid.adenilton.erbazimuth.data.repository.ErbRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MapViewModel(private val repository: ErbRepository) : ViewModel() {

    private val _erbsWithAzimutes = MutableStateFlow<List<ErbWithAzimutes>>(emptyList())
    val erbsWithAzimutes = _erbsWithAzimutes.asStateFlow()

    private val _showAddErbDialog = MutableStateFlow(false)
    val showAddErbDialog = _showAddErbDialog.asStateFlow()

    // 1. Criamos um SharedFlow para eventos que acontecem uma única vez, como mover a câmera.
    private val _cameraUpdateEvent = MutableSharedFlow<LatLng>()
    val cameraUpdateEvent = _cameraUpdateEvent.asSharedFlow()


    init {
        loadErbs()
    }

    private fun loadErbs() {
        viewModelScope.launch {
            repository.getAllErbsWithAzimutes()
                .catch { exception ->
                    Log.e("ErbAzimuthApp", "Erro ao carregar ERBs", exception)
                }
                .collect { data ->
                    Log.d("ErbAzimuthApp", "ViewModel: Novos dados recebidos. Total de ERBs: ${data.size}")
                    _erbsWithAzimutes.value = data
                }
        }
    }

    fun onShowAddErbDialog() {
        _showAddErbDialog.value = true
    }

    fun onDismissAddErbDialog() {
        _showAddErbDialog.value = false
    }

    fun addErbAndAzimuth(erb: Erb, azimute: Azimute) {
        viewModelScope.launch {
            Log.d("ErbAzimuthApp", "ViewModel: Tentando adicionar ERB e Azimute...")
            repository.insertErbAndAzimuth(erb, azimute)
            // 2. Após salvar com sucesso, emitimos um evento com a localização da nova ERB.
            _cameraUpdateEvent.emit(LatLng(erb.latitude, erb.longitude))
            onDismissAddErbDialog() // Fecha o diálogo
        }
    }
}

// Factory para criar o ViewModel com o repositório
@Suppress("UNCHECKED_CAST")
class MapViewModelFactory(private val repository: ErbRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

