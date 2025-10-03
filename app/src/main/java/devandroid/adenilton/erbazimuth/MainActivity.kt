package devandroid.adenilton.erbazimuth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import devandroid.adenilton.erbazimuth.data.database.ErbAzimuthDatabase
import devandroid.adenilton.erbazimuth.data.repository.ErbRepository
import devandroid.adenilton.erbazimuth.ui.screens.MapScreen
import devandroid.adenilton.erbazimuth.ui.theme.ErbAzimuthTheme
// A LINHA MAIS IMPORTANTE É ESTA, QUE RESOLVE O ERRO:
import devandroid.adenilton.erbazimuth.ui.viewmodel.MapViewModelFactory
import devandroid.adenilton.erbazimuth.ui.viewmodel.MapViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // --- Configuração da Injeção de Dependência Manual ---
        val database = ErbAzimuthDatabase.getDatabase(applicationContext)
        val repository = ErbRepository(database.erbAzimuthDao())
        val viewModelFactory = MapViewModelFactory(repository)
        val mapViewModel = ViewModelProvider(this, viewModelFactory)[MapViewModel::class.java]
        // --- Fim da Configuração ---

        setContent {
            ErbAzimuthTheme {
                // Passa o ViewModel para a tela
                MapScreen(viewModel = mapViewModel)
            }
        }
    }
}

