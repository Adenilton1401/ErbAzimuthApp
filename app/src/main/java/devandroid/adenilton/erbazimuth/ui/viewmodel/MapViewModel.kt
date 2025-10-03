package devandroid.adenilton.erbazimuth.ui.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.maps.model.LatLng
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.Erb
import devandroid.adenilton.erbazimuth.data.model.ErbWithAzimutes
import devandroid.adenilton.erbazimuth.data.repository.ErbRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel(private val repository: ErbRepository) : ViewModel() {
    private val _erbsWithAzimutes = MutableStateFlow<List<ErbWithAzimutes>>(emptyList())
    val erbsWithAzimutes: StateFlow<List<ErbWithAzimutes>> = _erbsWithAzimutes.asStateFlow()

    private val _showAddErbDialog = MutableStateFlow(false)
    val showAddErbDialog: StateFlow<Boolean> = _showAddErbDialog.asStateFlow()

    private val _newErbEvent = MutableSharedFlow<LatLng>()
    val newErbEvent: SharedFlow<LatLng> = _newErbEvent.asSharedFlow()

    // --- NOVO: ESTADO PARA O ITEM SELECIONADO ---
    private val _selectedItem = MutableStateFlow<Any?>(null)
    val selectedItem: StateFlow<Any?> = _selectedItem.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllErbsWithAzimutes().collect {
                _erbsWithAzimutes.value = it
                Log.d("ErbAzimuthApp", "ViewModel: Novos dados recebidos. Total de ERBs: ${it.size}")
            }
        }
    }

    fun addErbAndAzimuth(erb: Erb, azimute: Azimute) {
        viewModelScope.launch {
            Log.d("ErbAzimuthApp", "ViewModel: Tentando adicionar ERB e Azimute...")
            val newErbId = repository.insertErbAndAzimuth(erb, azimute)
            if(newErbId != -1L) { // Se uma nova ERB foi realmente inserida
                _newErbEvent.emit(LatLng(erb.latitude, erb.longitude))
            }
            onDismissAddErbDialog()
        }
    }

    fun onShowAddErbDialog() {
        _showAddErbDialog.value = true
    }

    fun onDismissAddErbDialog() {
        _showAddErbDialog.value = false
    }

    // --- NOVO: FUNÇÕES PARA CONTROLAR A SELEÇÃO ---
    fun onItemSelected(item: Any) {
        _selectedItem.value = item
    }

    fun onDismissDetails() {
        _selectedItem.value = null
    }
}


// Fábrica para o ViewModel (sem alterações)
class MapViewModelFactory(private val repository: ErbRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

