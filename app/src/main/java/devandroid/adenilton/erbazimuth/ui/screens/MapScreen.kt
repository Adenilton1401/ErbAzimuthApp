package devandroid.adenilton.erbazimuth.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import devandroid.adenilton.erbazimuth.R
import devandroid.adenilton.erbazimuth.data.model.Erb
import devandroid.adenilton.erbazimuth.data.model.LocalInteresse
import devandroid.adenilton.erbazimuth.ui.dialogs.*
import devandroid.adenilton.erbazimuth.ui.sheets.ItemDetailsSheet
import devandroid.adenilton.erbazimuth.ui.viewmodel.DialogState
import devandroid.adenilton.erbazimuth.ui.viewmodel.MapViewModel
import devandroid.adenilton.erbazimuth.utils.MapUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onNavigateBack: () -> Unit
) {
    val erbsWithAzimutes by viewModel.erbsWithAzimutes.collectAsState()
    val locaisInteresse by viewModel.locaisInteresse.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val itemToDelete by viewModel.itemToDelete.collectAsState()
    val isMyTowerLayerVisible by viewModel.isMyTowerLayerVisible.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val cellTowerInfoList by viewModel.cellTowerInfoList.collectAsState()
    val towerLocationList by viewModel.towerLocationList.collectAsState()
    val isMyTowerLoading by viewModel.isMyTowerLoading.collectAsState()

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val towerPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE)
    )
    val storagePermissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) Manifest.permission.WRITE_EXTERNAL_STORAGE else Manifest.permission.INTERNET
    )

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Lógica dos diálogos
    when (val state = dialogState) {
        is DialogState.AddErbAndAzimuth -> AddErbDialog(viewModel = viewModel, onDismiss = { viewModel.onDismissDialog() })
        is DialogState.AddAzimuth -> AddErbDialog(itemToProcess = state.erb, erbForContext = state.erb, viewModel = viewModel, onDismiss = { viewModel.onDismissDialog() })
        is DialogState.EditErb -> EditErbDialog(erbToEdit = state.erb, onConfirm = { viewModel.onConfirmEdit(it) }, onDismiss = { viewModel.onDismissDialog() })
        is DialogState.EditAzimuth -> AddErbDialog(itemToProcess = state.azimute, erbForContext = state.parentErb, viewModel = viewModel, onDismiss = { viewModel.onDismissDialog() })
        is DialogState.AddLocalInteresse -> AddLocalInteresseDialog(viewModel = viewModel, onDismiss = { viewModel.onDismissDialog() })
        is DialogState.EditLocalInteresse -> EditLocalInteresseDialog(localToEdit = state.local, onConfirm = { viewModel.onConfirmEdit(it) }, onDismiss = { viewModel.onDismissDialog() })
        DialogState.Hidden -> {}
    }

    if (itemToDelete != null) {
        ConfirmDeleteDialog(itemToDelete = itemToDelete!!, onDismiss = { viewModel.onDismissDelete() }, onConfirm = { viewModel.onConfirmDelete() })
    }

    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(-14.2350, -51.9253), 4f) }
    var isInitialZoomDone by remember(viewModel) { mutableStateOf(false) }

    // Efeito de animação para o zoom inicial do caso
    LaunchedEffect(erbsWithAzimutes, locaisInteresse) {
        val allPoints = erbsWithAzimutes.map { LatLng(it.erb.latitude, it.erb.longitude) } + locaisInteresse.map { LatLng(it.latitude, it.longitude) }
        if (allPoints.isNotEmpty() && !isInitialZoomDone) {
            val bounds = LatLngBounds.builder().apply { allPoints.forEach { include(it) } }.build()
            val cameraUpdate = if (allPoints.size > 1) CameraUpdateFactory.newLatLngBounds(bounds, 150) else CameraUpdateFactory.newLatLngZoom(bounds.center, 14f)
            cameraPositionState.animate(cameraUpdate)
            isInitialZoomDone = true
        }
    }

    // Efeito para focar em novos pontos adicionados (ERBs, Locais de Interesse)
    LaunchedEffect(viewModel) {
        viewModel.newPointEvent.collectLatest { newPointLocation ->
            cameraPositionState.animate(update = CameraUpdateFactory.newLatLngZoom(newPointLocation, 15f), durationMs = 1000)
        }
    }

    // EFEITO RESTAURADO: Foco automático para a camada "Minha Torre"
    LaunchedEffect(isMyTowerLayerVisible, userLocation, towerLocationList) {
        if (isMyTowerLayerVisible) {
            val userLatLng = userLocation?.let { LatLng(it.latitude, it.longitude) }
            val allMyTowerPoints = towerLocationList + (userLatLng?.let { listOf(it) } ?: emptyList())

            if (allMyTowerPoints.isNotEmpty()) {
                val boundsBuilder = LatLngBounds.builder()
                allMyTowerPoints.forEach { boundsBuilder.include(it) }
                val bounds = boundsBuilder.build()
                val cameraUpdate = if (allMyTowerPoints.size > 1) {
                    CameraUpdateFactory.newLatLngBounds(bounds, 200)
                } else {
                    CameraUpdateFactory.newLatLngZoom(bounds.center, 15f)
                }
                cameraPositionState.animate(cameraUpdate)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mapa") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar para Casos") } },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Minha Torre", fontWeight = FontWeight.Normal, modifier = Modifier.padding(end = 5.dp))
                        Switch(
                            checked = isMyTowerLayerVisible,
                            onCheckedChange = {
                                if (!isMyTowerLayerVisible) {
                                    if (towerPermissionsState.allPermissionsGranted) {
                                        viewModel.toggleMyTowerLayer()
                                    } else {
                                        towerPermissionsState.launchMultiplePermissionRequest()
                                    }
                                } else {
                                    viewModel.toggleMyTowerLayer()
                                }
                            },
                            thumbContent = if (isMyTowerLayerVisible) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Camada Ativa",
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomBarAction(
                        text = "Interesses",
                        onClick = { viewModel.onAddLocalInteresseRequest() }
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Adicionar Local de Interesse")
                    }
                    FloatingActionButton(
                        onClick = { viewModel.onAddErbRequest() },
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 4.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Adicionar ERB e Azimute")
                    }
                    BottomBarAction(
                        text = "Salvar",
                        onClick = {
                            if (storagePermissionState.status.isGranted) { viewModel.captureMapSnapshot(context) }
                            else { storagePermissionState.launchPermissionRequest() }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.btn_salvar),
                            contentDescription = "Salvar Imagem do Mapa",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false),
            ) {
                MapEffect(Unit) { map -> viewModel.onMapLoaded(map) }

                // Desenha os dados do CASO
                erbsWithAzimutes.forEach { (erb, azimutes) ->
                    val position = LatLng(erb.latitude, erb.longitude)
                    MarkerComposable(
                        state = MarkerState(position = position),
                        title = erb.identificacao,
                        snippet = erb.endereco ?: "Clique para ver detalhes",
                        onClick = { viewModel.onItemSelected(erb); true }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tower),
                            contentDescription = "ERB",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Unspecified
                        )
                    }
                    azimutes.forEach { azimute ->
                        val sectorPoints = MapUtils.calculateAzimuthSectorPoints(center = position, radius = azimute.raio, azimuth = azimute.azimute.toFloat())
                        if (sectorPoints.isNotEmpty()) {
                            Polygon(points = sectorPoints, fillColor = Color(azimute.cor.toULong()).copy(alpha = 0.5f), strokeColor = Color(azimute.cor.toULong()), strokeWidth = 5f, clickable = true, onClick = { viewModel.onItemSelected(azimute) })
                        }
                    }
                }

                locaisInteresse.forEach { local ->
                    MarkerComposable(
                        state = MarkerState(position = LatLng(local.latitude, local.longitude)),
                        title = local.nome,
                        snippet = "Clique para ver detalhes",
                        onClick = { viewModel.onItemSelected(local); true }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = local.nome,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .background(
                                        color = Color.White.copy(alpha = 0.7f),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Local de Interesse",
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFFC0392B) // Vermelho escuro
                            )
                        }
                    }
                }

                // Desenha a CAMADA "MINHA TORRE" se estiver ATIVA
                if (isMyTowerLayerVisible) {
                    userLocation?.let {
                        MarkerComposable(
                            state = MarkerState(position = LatLng(it.latitude, it.longitude)),
                            title = "Sua Posição",
                            zIndex = 1f // Garante que fique por cima
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Sua Posição",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    val towerData = cellTowerInfoList.zip(towerLocationList)

                    towerData.forEach { (info, location) ->
                        MarkerComposable(
                            state = MarkerState(position = location),
                            title = info.operatorName,
                            snippet = "Clique para ver detalhes",
                            onClick = { viewModel.onItemSelected(info); true }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_tower),
                                contentDescription = "Torre Conectada",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Unspecified
                            )
                        }
                        userLocation?.let { userLoc ->
                            Polyline(points = listOf(LatLng(userLoc.latitude, userLoc.longitude), location), color = Color.Red, width = 10f)
                        }
                    }
                }
            }
            if (isMyTowerLoading) { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) }
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
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxHeight()
    ) {
        icon()
        Text(text = text, fontSize = 12.sp)
    }
}

