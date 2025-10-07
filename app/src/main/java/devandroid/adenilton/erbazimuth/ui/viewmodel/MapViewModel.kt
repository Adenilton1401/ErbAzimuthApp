package devandroid.adenilton.erbazimuth.ui.viewmodel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.Erb
import devandroid.adenilton.erbazimuth.data.model.ErbWithAzimutes
import devandroid.adenilton.erbazimuth.data.repository.ErbRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream

class MapViewModel(
    private val repository: ErbRepository,
    private val casoId: Long
) : ViewModel() {
    private val _erbsWithAzimutes = MutableStateFlow<List<ErbWithAzimutes>>(emptyList())
    val erbsWithAzimutes: StateFlow<List<ErbWithAzimutes>> = _erbsWithAzimutes.asStateFlow()

    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    private val _newErbEvent = MutableSharedFlow<LatLng>()
    val newErbEvent: SharedFlow<LatLng> = _newErbEvent.asSharedFlow()

    private val _selectedItem = MutableStateFlow<Any?>(null)
    val selectedItem: StateFlow<Any?> = _selectedItem.asStateFlow()

    private val _itemToDelete = MutableStateFlow<Any?>(null)
    val itemToDelete: StateFlow<Any?> = _itemToDelete.asStateFlow()

    private var googleMap: GoogleMap? = null
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _initialCameraUpdate = MutableSharedFlow<CameraUpdate>(replay = 1)
    val initialCameraUpdate: SharedFlow<CameraUpdate> = _initialCameraUpdate.asSharedFlow()
    private var isInitialZoomDone = false


    init {
        viewModelScope.launch {
            repository.getErbsForCase(casoId)
                .onEach { erbs ->
                    if (erbs.isNotEmpty() && !isInitialZoomDone) {
                        val boundsBuilder = LatLngBounds.builder()
                        erbs.forEach { erbWithAzimutes ->
                            boundsBuilder.include(LatLng(erbWithAzimutes.erb.latitude, erbWithAzimutes.erb.longitude))
                        }
                        val bounds = boundsBuilder.build()
                        val cameraUpdate = if (erbs.size > 1) {
                            CameraUpdateFactory.newLatLngBounds(bounds, 150)
                        } else {
                            CameraUpdateFactory.newLatLngZoom(bounds.center, 14f)
                        }
                        _initialCameraUpdate.tryEmit(cameraUpdate)
                        isInitialZoomDone = true
                    }
                }
                .collect {
                    _erbsWithAzimutes.value = it
                }
        }
    }

    fun onMapLoaded(map: GoogleMap) {
        this.googleMap = map
    }

    fun captureMapSnapshot(context: Context) {
        googleMap?.snapshot { bitmap ->
            if (bitmap != null) {
                saveBitmapToGallery(context, bitmap)
            } else {
                Log.e("ErbAzimuthApp", "Bitmap do mapa é nulo.")
                viewModelScope.launch {
                    _snackbarMessage.emit("Falha ao capturar imagem do mapa.")
                }
            }
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
            val uri = try {
                val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                resolver.insert(contentUri, contentValues)
            } catch (e: Exception) {
                Log.e("ErbAzimuthApp", "Erro ao criar URI da imagem.", e)
                null
            }

            if (uri == null) {
                _snackbarMessage.emit("Erro ao preparar para salvar a imagem.")
                return@launch
            }

            try {
                stream = resolver.openOutputStream(uri)
                if (stream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    _snackbarMessage.emit("Imagem salva na galeria!")
                } else {
                    throw IOException("Não foi possível abrir o stream de saída.")
                }
            } catch (e: Exception) {
                Log.e("ErbAzimuthApp", "Falha ao salvar bitmap.", e)
                resolver.delete(uri, null, null)
                _snackbarMessage.emit("Erro ao salvar imagem.")
            } finally {
                stream?.close()
            }
        }
    }

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

    fun onConfirmAdd(erb: Erb, azimute: Azimute) {
        viewModelScope.launch {
            val erbForCase = erb.copy(casoId = casoId)
            val newErbId = repository.insertErbAndAzimuth(erbForCase, azimute)
            if(newErbId != -1L) { _newErbEvent.emit(LatLng(erb.latitude, erb.longitude)) }
            onDismissDialog()
        }
    }

    fun onConfirmEdit(item: Any) {
        viewModelScope.launch {
            when (item) {
                is Erb -> {
                    val updatedErb = item.copy(casoId = casoId)
                    repository.updateErb(updatedErb)
                    _newErbEvent.emit(LatLng(item.latitude, item.longitude))
                }
                is Azimute -> repository.updateAzimute(item)
            }
            onDismissDialog()
        }
    }

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


sealed class DialogState {
    object Hidden : DialogState()
    object AddErbAndAzimuth : DialogState()
    data class AddAzimuth(val erb: Erb) : DialogState()
    data class EditErb(val erb: Erb) : DialogState()
    data class EditAzimuth(val azimute: Azimute, val parentErb: Erb) : DialogState()
}

class MapViewModelFactory(
    private val repository: ErbRepository,
    private val casoId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(repository, casoId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

