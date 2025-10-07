package devandroid.adenilton.erbazimuth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import devandroid.adenilton.erbazimuth.data.database.ErbAzimuthDatabase
import devandroid.adenilton.erbazimuth.data.repository.ErbRepository
import devandroid.adenilton.erbazimuth.ui.screens.CaseListScreen
import devandroid.adenilton.erbazimuth.ui.screens.MapScreen
import devandroid.adenilton.erbazimuth.ui.theme.ErbAzimuthTheme
import devandroid.adenilton.erbazimuth.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa as dependências uma única vez
        val database = ErbAzimuthDatabase.getDatabase(this)
        val repository = ErbRepository(database.erbAzimuthDao(), applicationContext)

        enableEdgeToEdge()
        setContent {
            ErbAzimuthTheme {
                // Controlador de Navegação Principal
                AppNavigator(repository = repository)
            }
        }
    }
}

// NOVO: Componente que gerencia a navegação entre telas
@Composable
fun AppNavigator(repository: ErbRepository) {
    // Estado que controla qual tela está visível. Inicia na tela de lista.
    var currentScreen by remember { mutableStateOf<Screen>(Screen.CaseList) }

    when (val screen = currentScreen) {
        is Screen.CaseList -> {
            val caseViewModel: CaseViewModel = viewModel(
                factory = CaseViewModelFactory(repository)
            )
            CaseListScreen(
                viewModel = caseViewModel,
                onCaseSelected = { caseId ->
                    // Ao selecionar um caso, muda para a tela do mapa com o ID do caso
                    currentScreen = Screen.Map(caseId)
                }
            )
        }
        is Screen.Map -> {
            val mapViewModel: MapViewModel = viewModel(
                key = screen.caseId.toString(),
                factory = MapViewModelFactory(repository, screen.caseId)
            )
            MapScreen(
                viewModel = mapViewModel,
                onNavigateBack = {
                    // O botão de voltar na tela do mapa retorna para a lista
                    currentScreen = Screen.CaseList
                }
            )
        }
    }
}

// Classe selada para representar as telas e seus parâmetros
sealed class Screen {
    object CaseList : Screen()
    data class Map(val caseId: Long) : Screen()
}

