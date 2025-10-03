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

    // --- ESTADOS REATORADOS PARA MAIOR CLAREZA ---
    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    private val _newErbEvent = MutableSharedFlow<LatLng>()
    val newErbEvent: SharedFlow<LatLng> = _newErbEvent.asSharedFlow()

    private val _selectedItem = MutableStateFlow<Any?>(null)
    val selectedItem: StateFlow<Any?> = _selectedItem.asStateFlow()

    private val _itemToDelete = MutableStateFlow<Any?>(null)
    val itemToDelete: StateFlow<Any?> = _itemToDelete.asStateFlow()

    init {
        viewModelScope.launch { repository.getAllErbsWithAzimutes().collect { _erbsWithAzimutes.value = it } }
    }

    // --- FUNÇÕES DE CONTROLE DOS DIÁLOGOS ---
    fun onAddErbRequest() { _dialogState.value = DialogState.AddErbAndAzimuth }
    fun onAddAzimuthRequest(erb: Erb) { _selectedItem.value = null; _dialogState.value = DialogState.AddAzimuth(erb) }
    fun onEditRequest(item: Any) {
        _selectedItem.value = null
        _dialogState.value = when(item) {
            is Erb -> DialogState.EditErb(item)
            is Azimute -> {
                val parentErb = _erbsWithAzimutes.value.find { it.azimutes.any { az -> az.id == item.id } }?.erb
                if (parentErb != null) DialogState.EditAzimuth(item, parentErb) else DialogState.Hidden
            }
            else -> DialogState.Hidden
        }
    }
    fun onDismissDialog() { _dialogState.value = DialogState.Hidden }

    // --- FUNÇÕES DE CONFIRMAÇÃO ---
    fun onConfirmAdd(erb: Erb, azimute: Azimute) {
        viewModelScope.launch {
            val newErbId = repository.insertErbAndAzimuth(erb, azimute)
            if(newErbId != -1L) { _newErbEvent.emit(LatLng(erb.latitude, erb.longitude)) }
            onDismissDialog()
        }
    }

    fun onConfirmEdit(item: Any) {
        viewModelScope.launch {
            when (item) {
                is Erb -> repository.updateErb(item)
                is Azimute -> repository.updateAzimute(item)
            }
            onDismissDialog()
        }
    }

    // --- LÓGICA DE EXCLUSÃO (sem alterações) ---
    fun onDeleteRequest(item: Any) { _itemToDelete.value = item }
    fun onConfirmDelete() {
        viewModelScope.launch {
            _itemToDelete.value?.let { item ->
                when (item) {
                    is Erb -> repository.deleteErb(item)
                    is Azimute -> repository.deleteAzimute(item)
                }
            }
            _itemToDelete.value = null; _selectedItem.value = null
        }
    }
    fun onDismissDelete() { _itemToDelete.value = null }
    fun onItemSelected(item: Any) { _selectedItem.value = item }
    fun onDismissDetails() { _selectedItem.value = null }
}

// NOVO: Classe selada para representar os diferentes estados do diálogo
sealed class DialogState {
    object Hidden : DialogState()
    object AddErbAndAzimuth : DialogState()
    data class AddAzimuth(val erb: Erb) : DialogState()
    data class EditErb(val erb: Erb) : DialogState()
    data class EditAzimuth(val azimute: Azimute, val parentErb: Erb) : DialogState()
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

