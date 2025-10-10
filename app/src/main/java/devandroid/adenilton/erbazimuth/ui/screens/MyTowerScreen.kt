package devandroid.adenilton.erbazimuth.ui.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import devandroid.adenilton.erbazimuth.ui.viewmodel.MyTowerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MyTowerScreen(
    viewModel: MyTowerViewModel,
    onNavigateBack: () -> Unit
) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
    )

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    val userLocation by viewModel.userLocation.collectAsState()
    val cellTowerInfoList by viewModel.cellTowerInfoList.collectAsState()
    val towerLocationList by viewModel.towerLocationList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-14.2350, -51.9253), 4f)
    }

    LaunchedEffect(userLocation, towerLocationList) {
        val userLatLng = userLocation?.let { LatLng(it.latitude, it.longitude) }
        val allPoints = towerLocationList + (userLatLng?.let { listOf(it) } ?: emptyList())

        if (allPoints.isNotEmpty()) {
            if (allPoints.size > 1) {
                val bounds = LatLngBounds.builder().apply {
                    allPoints.forEach { include(it) }
                }.build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 200))
            } else {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(allPoints.first(), 15f))
            }
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState()
    var animationDone by remember { mutableStateOf(false) }

    // --- NOVA ANIMAÇÃO ---
    // Efeito para a animação "peek-a-boo" da folha inferior
    LaunchedEffect(cellTowerInfoList) {
        // Roda a animação apenas na primeira vez que os dados chegam
        if (cellTowerInfoList.isNotEmpty() && !animationDone) {
            // Pequeno atraso para garantir que a UI esteja pronta
            delay(500)
            // Expande a folha
            scaffoldState.bottomSheetState.expand()
            // Atraso para o usuário ver
            delay(1200)
            // Recolhe de volta para a posição "peek"
            scaffoldState.bottomSheetState.partialExpand()
            // Marca a animação como concluída para não repetir
            animationDone = true
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text("Minha Torre") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        sheetContent = {
            LazyColumn(modifier = Modifier.padding(bottom = 16.dp)) {
                if (cellTowerInfoList.isEmpty() && !isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhuma torre registrada encontrada.")
                        }
                    }
                } else {
                    itemsIndexed(cellTowerInfoList) { index, info ->
                        InfoCard(title = "Torre Conectada ${index + 1}") {
                            InfoRow("Operadora:", info.operatorName)
                            InfoRow("Sinal:", "${info.signalStrength} dBm")
                            InfoRow("Cell ID:", info.cid.toString())
                            InfoRow("MCC:", info.mcc.toString())
                            InfoRow("MNC:", info.mnc.toString())
                            InfoRow("LAC/TAC:", info.lac.toString())
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        },
        sheetPeekHeight = 90.dp,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        // Conteúdo principal (atrás da folha)
        if (permissionsState.allPermissionsGranted) {
            LaunchedEffect(Unit) { viewModel.startDataCollection() }

            Box(modifier = Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                ) {
                    userLocation?.let {
                        Marker(state = MarkerState(position = LatLng(it.latitude, it.longitude)), title = "Sua Posição")
                    }

                    towerLocationList.forEach { towerLatLng ->
                        Marker(
                            state = MarkerState(position = towerLatLng),
                            title = "Torre Conectada",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                        userLocation?.let { userLoc ->
                            Polyline(
                                points = listOf(LatLng(userLoc.latitude, userLoc.longitude), towerLatLng),
                                color = Color.Red,
                                width = 10f
                            )
                        }
                    }
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("As permissões de Localização e Telefone são necessárias para esta funcionalidade.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                    Text("Conceder Permissões")
                }
            }
        }
    }
}

// Componentes auxiliares para a UI
@Composable
fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row {
        Text(text = label, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
        Text(text = value)
    }
}

