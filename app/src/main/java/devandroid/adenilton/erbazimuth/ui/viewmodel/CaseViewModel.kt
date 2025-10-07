package devandroid.adenilton.erbazimuth.ui.viewmodel

import androidx.lifecycle.*
import devandroid.adenilton.erbazimuth.data.model.Caso
import devandroid.adenilton.erbazimuth.data.repository.ErbRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CaseViewModel(private val repository: ErbRepository) : ViewModel() {
    private val _cases = MutableStateFlow<List<Caso>>(emptyList())
    val cases: StateFlow<List<Caso>> = _cases.asStateFlow()

    private val _showAddCaseDialog = MutableStateFlow(false)
    // CORREÇÃO AQUI: Removido o hífen
    val showAddCaseDialog: StateFlow<Boolean> = _showAddCaseDialog.asStateFlow()

    private val _caseToEdit = MutableStateFlow<Caso?>(null)
    val caseToEdit: StateFlow<Caso?> = _caseToEdit.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllCasos().collect {
                _cases.value = it
            }
        }
    }

    fun onShowAddCaseDialog() { _showAddCaseDialog.value = true }
    fun onDismissAddCaseDialog() { _showAddCaseDialog.value = false }

    fun onConfirmAddCase(caso: Caso) {
        viewModelScope.launch {
            repository.insertCaso(caso)
            onDismissAddCaseDialog()
        }
    }

    fun deleteCase(caso: Caso) {
        viewModelScope.launch {
            repository.deleteCaso(caso)
        }
    }

    fun onEditRequest(caso: Caso) {
        _caseToEdit.value = caso
    }

    fun onDismissEditDialog() {
        _caseToEdit.value = null
    }

    fun onConfirmEdit(caso: Caso) {
        viewModelScope.launch {
            repository.updateCaso(caso)
            onDismissEditDialog()
        }
    }
}

class CaseViewModelFactory(private val repository: ErbRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CaseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CaseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

