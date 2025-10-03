package devandroid.adenilton.erbazimuth.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import devandroid.adenilton.erbazimuth.data.model.Erb
import devandroid.adenilton.erbazimuth.ui.dialogs.AddErbDialog
import devandroid.adenilton.erbazimuth.ui.sheets.ItemDetailsSheet
import devandroid.adenilton.erbazimuth.ui.viewmodel.MapViewModel
import devandroid.adenilton.erbazimuth.utils.MapUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel) {
    val erbsWithAzimutes by viewModel.erbsWithAzimutes.collectAsState()
    val showDialog by viewModel.showAddErbDialog.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    // NOVO: Pega o contexto atual para usar na Intent de navegação
    val context = LocalContext.current


    if (showDialog) {
        AddErbDialog(
            onDismiss = { viewModel.onDismissAddErbDialog() },
            onConfirm = { erb, azimute ->
                viewModel.addErbAndAzimuth(erb, azimute)
            }
        )
    }

    val brazilCenter = LatLng(-14.2350, -51.9253)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(brazilCenter, 4f)
    }

    LaunchedEffect(Unit) {
        viewModel.newErbEvent.collectLatest { newErbLocation ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(newErbLocation, 15f),
                durationMs = 1000
            )
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onShowAddErbDialog() }) {
                Icon(Icons.Filled.Add, contentDescription = "Adicionar ERB")
            }
        },
        floatingActionButtonPosition = FabPosition.Start
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                erbsWithAzimutes.forEach { erbWithAzimutes ->
                    val erb = erbWithAzimutes.erb
                    val position = LatLng(erb.latitude, erb.longitude)

                    Marker(
                        state = MarkerState(position = position),
                        title = erb.identificacao,
                        // ATUALIZADO: Mostra o endereço no snippet do marcador
                        snippet = erb.endereco ?: "Clique para ver detalhes",
                        onClick = {
                            viewModel.onItemSelected(erb)
                            true
                        }
                    )

                    erbWithAzimutes.azimutes.forEach { azimute ->
                        val sectorPoints = MapUtils.calculateAzimuthSectorPoints(
                            center = position,
                            radius = azimute.raio,
                            azimuth = azimute.azimute.toFloat()
                        )

                        if (sectorPoints.isNotEmpty()) {
                            Polygon(
                                points = sectorPoints,
                                fillColor = Color(azimute.cor.toULong()).copy(alpha = 0.5f),
                                strokeColor = Color(azimute.cor.toULong()),
                                strokeWidth = 5f,
                                clickable = true,
                                onClick = {
                                    viewModel.onItemSelected(azimute)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedItem != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onDismissDetails() },
            sheetState = sheetState
        ) {
            ItemDetailsSheet(
                item = selectedItem!!,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            viewModel.onDismissDetails()
                        }
                    }
                },
                onEditClick = {
                    // TODO: Implementar lógica de edição
                },
                onDeleteClick = {
                    // TODO: Implementar lógica de exclusão
                },
                // NOVO: Implementação da lógica de navegação
                onNavigateClick = { item ->
                    if (item is Erb) {
                        // Cria a URI para a navegação do Google Maps
                        val gmmIntentUri = Uri.parse("google.navigation:q=${item.latitude},${item.longitude}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")

                        // Verifica se o Google Maps está instalado antes de tentar abrir
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        }
                    }
                }
            )
        }
    }
}

