package devandroid.adenilton.erbazimuth.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import devandroid.adenilton.erbazimuth.data.model.Erb
import devandroid.adenilton.erbazimuth.ui.dialogs.AddErbDialog
import devandroid.adenilton.erbazimuth.ui.dialogs.ConfirmDeleteDialog
import devandroid.adenilton.erbazimuth.ui.dialogs.EditErbDialog
import devandroid.adenilton.erbazimuth.ui.sheets.ItemDetailsSheet
import devandroid.adenilton.erbazimuth.ui.viewmodel.DialogState
import devandroid.adenilton.erbazimuth.ui.viewmodel.MapViewModel
import devandroid.adenilton.erbazimuth.utils.MapUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onNavigateBack: () -> Unit
) {
    val erbsWithAzimutes by viewModel.erbsWithAzimutes.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val itemToDelete by viewModel.itemToDelete.collectAsState()

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val storagePermissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) Manifest.permission.WRITE_EXTERNAL_STORAGE
        else Manifest.permission.INTERNET
    )

    // Lógica para exibir o diálogo correto com base no estado do ViewModel
    when (val state = dialogState) {
        is DialogState.AddErbAndAzimuth -> AddErbDialog(onDismiss = { viewModel.onDismissDialog() }, onConfirm = { erb, azimute, _ -> viewModel.onConfirmAdd(erb, azimute) })
        is DialogState.AddAzimuth -> AddErbDialog(itemToProcess = state.erb, erbForContext = state.erb, onDismiss = { viewModel.onDismissDialog() }, onConfirm = { erb, azimute, _ -> viewModel.onConfirmAdd(erb, azimute) })
        is DialogState.EditErb -> EditErbDialog(erbToEdit = state.erb, onDismiss = { viewModel.onDismissDialog() }, onConfirm = { updatedErb -> viewModel.onConfirmEdit(updatedErb) })
        is DialogState.EditAzimuth -> AddErbDialog(itemToProcess = state.azimute, erbForContext = state.parentErb, onDismiss = { viewModel.onDismissDialog() }, onConfirm = { _, updatedAzimute, _ -> viewModel.onConfirmEdit(updatedAzimute) })
        DialogState.Hidden -> {}
    }
    if (itemToDelete != null) { ConfirmDeleteDialog(itemToDelete = itemToDelete!!, onDismiss = { viewModel.onDismissDelete() }, onConfirm = { viewModel.onConfirmDelete() }) }

    val brazilCenter = LatLng(-14.2350, -51.9253)
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(brazilCenter, 4f) }

    LaunchedEffect(key1 = viewModel) {
        // Escuta o evento de zoom inicial (apenas uma vez)
        launch {
            viewModel.initialCameraUpdate.firstOrNull()?.let { update ->
                cameraPositionState.animate(update)
            }
        }
        // Continua escutando eventos de novas ERBs adicionadas
        launch {
            viewModel.newErbEvent.collectLatest { newErbLocation ->
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(newErbLocation, 15f),
                    durationMs = 1000
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onAddErbRequest() }) {
                Icon(Icons.Filled.Add, contentDescription = "Adicionar ERB")
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            BottomAppBar(
                actions = {
                    BottomBarAction(
                        text = "Voltar",
                        icon = Icons.Default.ArrowBack,
                        contentDescription = "Voltar para Casos",
                        onClick = onNavigateBack
                    )
                    BottomBarAction(
                        text = "Salvar",
                        icon = Icons.Filled.AccountBox,
                        contentDescription = "Salvar Imagem do Mapa",
                        onClick = {
                            if (storagePermissionState.status.isGranted) {
                                viewModel.captureMapSnapshot(context)
                            } else {
                                storagePermissionState.launchPermissionRequest()
                            }
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                MapEffect(Unit) { map ->
                    viewModel.onMapLoaded(map)
                }

                erbsWithAzimutes.forEach { erbWithAzimutes ->
                    val erb = erbWithAzimutes.erb
                    val position = LatLng(erb.latitude, erb.longitude)
                    Marker(state = MarkerState(position = position), title = erb.identificacao, snippet = erb.endereco ?: "Clique para ver detalhes", onClick = { viewModel.onItemSelected(erb); true })
                    erbWithAzimutes.azimutes.forEach { azimute ->
                        val sectorPoints = MapUtils.calculateAzimuthSectorPoints(center = position, radius = azimute.raio, azimuth = azimute.azimute.toFloat())
                        if (sectorPoints.isNotEmpty()) {
                            Polygon(points = sectorPoints, fillColor = Color(azimute.cor.toULong()).copy(alpha = 0.5f), strokeColor = Color(azimute.cor.toULong()), strokeWidth = 5f, clickable = true, onClick = { viewModel.onItemSelected(azimute) })
                        }
                    }
                }
            }
        }
    }

    if (selectedItem != null) {
        ModalBottomSheet(onDismissRequest = { viewModel.onDismissDetails() }, sheetState = sheetState) {
            ItemDetailsSheet(
                item = selectedItem!!,
                onDismiss = { scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) { viewModel.onDismissDetails() } } },
                onEditClick = { item -> viewModel.onEditRequest(item) },
                onDeleteClick = { item -> viewModel.onDeleteRequest(item) },
                onNavigateClick = { item ->
                    if (item is Erb) {
                        val gmmIntentUri = Uri.parse("google.navigation:q=${item.latitude},${item.longitude}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply { setPackage("com.google.android.apps.maps") }
                        if (mapIntent.resolveActivity(context.packageManager) != null) { context.startActivity(mapIntent) }
                    }
                },
                onAddAzimuthClick = { erb -> if (erb is Erb) { viewModel.onAddAzimuthRequest(erb) } }
            )
        }
    }
}

@Composable
private fun BottomBarAction(
    text: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
        Text(text = text, fontSize = 12.sp)
    }
}

