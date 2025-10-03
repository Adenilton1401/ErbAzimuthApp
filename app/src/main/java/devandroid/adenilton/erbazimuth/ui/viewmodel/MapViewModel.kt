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

    private val _itemToProcess = MutableStateFlow<Any?>(null)
    val itemToProcess: StateFlow<Any?> = _itemToProcess.asStateFlow()

    private val _isDialogVisible = MutableStateFlow(false)
    val isDialogVisible: StateFlow<Boolean> = _isDialogVisible.asStateFlow()

    private val _erbForDialogContext = MutableStateFlow<Erb?>(null)
    val erbForDialogContext: StateFlow<Erb?> = _erbForDialogContext.asStateFlow()

    private val _newErbEvent = MutableSharedFlow<LatLng>()
    val newErbEvent: SharedFlow<LatLng> = _newErbEvent.asSharedFlow()

    private val _selectedItem = MutableStateFlow<Any?>(null)
    val selectedItem: StateFlow<Any?> = _selectedItem.asStateFlow()

    private val _itemToDelete = MutableStateFlow<Any?>(null)
    val itemToDelete: StateFlow<Any?> = _itemToDelete.asStateFlow()

    init {
        viewModelScope.launch { repository.getAllErbsWithAzimutes().collect { _erbsWithAzimutes.value = it } }
    }

    fun onSave(erb: Erb, azimute: Azimute, azimuteToEdit: Azimute?) {
        viewModelScope.launch {
            if (azimuteToEdit != null) {
                repository.updateAzimute(azimute)
            } else {
                val newErbId = repository.insertErbAndAzimuth(erb, azimute)
                if (newErbId != -1L) {
                    _newErbEvent.emit(LatLng(erb.latitude, erb.longitude))
                }
            }
            onDismissDialog()
        }
    }

    fun onAddErbRequest() {
        _itemToProcess.value = null
        _erbForDialogContext.value = null
        _isDialogVisible.value = true
    }

    fun onAddAzimuthRequest(erb: Erb) {
        _selectedItem.value = null
        _itemToProcess.value = erb
        _erbForDialogContext.value = erb
        _isDialogVisible.value = true
    }

    fun onEditRequest(item: Any) {
        _selectedItem.value = null
        if (item is Azimute) {
            val parentErb = _erbsWithAzimutes.value.find { erbWithAzimutes ->
                erbWithAzimutes.azimutes.any { it.id == item.id }
            }?.erb
            _erbForDialogContext.value = parentErb
        }
        _itemToProcess.value = item
        _isDialogVisible.value = true
    }

    fun onDismissDialog() {
        _isDialogVisible.value = false
        _itemToProcess.value = null
        _erbForDialogContext.value = null
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

    fun onDismissDelete() {
        _itemToDelete.value = null
    }

    fun onItemSelected(item: Any) {
        _selectedItem.value = item
    }

    fun onDismissDetails() {
        _selectedItem.value = null
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

