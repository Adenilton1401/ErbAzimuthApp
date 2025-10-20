package devandroid.adenilton.erbazimuth

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import devandroid.adenilton.erbazimuth.data.database.ErbAzimuthDatabase
import devandroid.adenilton.erbazimuth.data.repository.ErbRepository
import devandroid.adenilton.erbazimuth.ui.dialogs.ExitConfirmationDialog
import devandroid.adenilton.erbazimuth.ui.screens.CaseListScreen
import devandroid.adenilton.erbazimuth.ui.screens.MapScreen
import devandroid.adenilton.erbazimuth.ui.theme.ErbAzimuthTheme
import devandroid.adenilton.erbazimuth.ui.viewmodel.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- CORREÇÃO AQUI ---
        // Passa o 'lifecycleScope' da Activity para a função getDatabase.
        // Isso garante que a corrotina de pré-carregamento tenha um ciclo de vida seguro.
        val database = ErbAzimuthDatabase.getDatabase(this)
        val repository = ErbRepository(database.erbAzimuthDao(), applicationContext)

        // Inicia a verificação de pré-carregamento em segundo plano
        lifecycleScope.launch {
            repository.prePopulateCacheIfNeeded()
        }

        enableEdgeToEdge()
        setContent {
            ErbAzimuthTheme {
                AppNavigator(repository = repository)
            }
        }
    }
}

@Composable
fun AppNavigator(repository: ErbRepository) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.CaseList) }
    val application = LocalContext.current.applicationContext as Application
    var showExitDialog by remember { mutableStateOf(false) }
    val activity = (LocalContext.current as? Activity)

    BackHandler {
        when (currentScreen) {
            is Screen.Map -> {
                currentScreen = Screen.CaseList
            }
            is Screen.CaseList -> {
                showExitDialog = true
            }
        }
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            onDismiss = { showExitDialog = false },
            onConfirm = {
                activity?.finish()
            }
        )
    }

    when (val screen = currentScreen) {
        is Screen.CaseList -> {
            val caseViewModel: CaseViewModel = viewModel(factory = CaseViewModelFactory(repository))
            CaseListScreen(
                viewModel = caseViewModel,
                onCaseSelected = { caseId -> currentScreen = Screen.Map(caseId) }
            )
        }
        is Screen.Map -> {
            val mapViewModel: MapViewModel = viewModel(
                key = screen.caseId.toString(),
                factory = MapViewModelFactory(application, repository, screen.caseId)
            )
            MapScreen(
                viewModel = mapViewModel,
                onNavigateBack = { currentScreen = Screen.CaseList }
            )
        }
    }
}

sealed class Screen {
    object CaseList : Screen()
    data class Map(val caseId: Long) : Screen()
}

