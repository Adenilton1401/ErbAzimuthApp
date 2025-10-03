package devandroid.adenilton.erbazimuth.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import devandroid.adenilton.erbazimuth.ui.dialogs.AddErbDialog
import devandroid.adenilton.erbazimuth.ui.viewmodel.MapViewModel
import devandroid.adenilton.erbazimuth.utils.MapUtils
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MapScreen(viewModel: MapViewModel) {
    val erbsWithAzimutes by viewModel.erbsWithAzimutes.collectAsState()
    val showDialog by viewModel.showAddErbDialog.collectAsState()

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

    // --- LÓGICA PARA FOCAR NO MAPA ---
    // 1. Usamos LaunchedEffect para executar um código que sobrevive a recomposições
    //    e pode chamar funções 'suspend' (como a animação da câmera).
    LaunchedEffect(Unit) {
        // 2. Coletamos os eventos do ViewModel. 'collectLatest' garante que se
        //    muitos eventos chegarem rápido, só o último será processado.
        viewModel.cameraUpdateEvent.collectLatest { latLng ->
            // 3. Anima a câmera para a nova posição com zoom.
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(latLng, 15f), // 15f é um bom zoom para nível de rua
                durationMs = 1000 // Animação dura 1 segundo
            )
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onShowAddErbDialog() }) {
                Icon(Icons.Filled.Add, contentDescription = "Adicionar ERB")
            }
        }, floatingActionButtonPosition = FabPosition.Start

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
                        snippet = "Lat: ${erb.latitude}, Lng: ${erb.longitude}"
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
                                fillColor = Color(azimute.cor.toULong()).copy(alpha = 0.3f),
                                strokeColor = Color(azimute.cor.toULong()),
                                strokeWidth = 3f
                            )
                        }
                    }
                }
            }
        }
    }
}

