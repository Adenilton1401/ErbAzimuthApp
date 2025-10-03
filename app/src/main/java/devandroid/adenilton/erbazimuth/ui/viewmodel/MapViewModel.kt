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

    private val _selectedItem = MutableStateFlow<Any?>(null)
    val selectedItem: StateFlow<Any?> = _selectedItem.asStateFlow()

    private val _itemToDelete = MutableStateFlow<Any?>(null)
    val itemToDelete: StateFlow<Any?> = _itemToDelete.asStateFlow()

    // NOVO: Guarda a ERB de contexto para adicionar um novo azimute
    private val _erbForNewAzimuth = MutableStateFlow<Erb?>(null)
    val erbForNewAzimuth: StateFlow<Erb?> = _erbForNewAzimuth.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllErbsWithAzimutes().collect { _erbsWithAzimutes.value = it }
        }
    }

    fun addErbAndAzimuth(erb: Erb, azimute: Azimute) {
        viewModelScope.launch {
            val newErbId = repository.insertErbAndAzimuth(erb, azimute)
            if(newErbId != -1L) {
                _newErbEvent.emit(LatLng(erb.latitude, erb.longitude))
            }
            onDismissAddErbDialog()
        }
    }

    fun onDeleteRequest(item: Any) {
        _itemToDelete.value = item
    }

    fun onConfirmDelete() {
        viewModelScope.launch {
            _itemToDelete.value?.let { item ->
                when (item) {
                    is Erb -> repository.deleteErb(item)
                    is Azimute -> repository.deleteAzimute(item)
                }
            }
            _itemToDelete.value = null
            _selectedItem.value = null
        }
    }

    // NOVO: Lógica para o novo botão
    fun onAddAzimuthRequest(erb: Erb) {
        // Fecha a folha de detalhes
        _selectedItem.value = null
        // Define a ERB para a qual estamos adicionando um azimute
        _erbForNewAzimuth.value = erb
        // Mostra o diálogo de adição
        _showAddErbDialog.value = true
    }

    fun onDismissDelete() { _itemToDelete.value = null }
    fun onShowAddErbDialog() { _showAddErbDialog.value = true }
    fun onItemSelected(item: Any) { _selectedItem.value = item }
    fun onDismissDetails() { _selectedItem.value = null }

    fun onDismissAddErbDialog() {
        _showAddErbDialog.value = false
        // Limpa a ERB de contexto quando o diálogo é fechado
        _erbForNewAzimuth.value = null
    }
}


class MapViewModelFactory(private val repository: ErbRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

