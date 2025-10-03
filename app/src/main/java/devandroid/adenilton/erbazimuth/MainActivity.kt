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
import devandroid.adenilton.erbazimuth.ui.viewmodel.MapViewModel
import devandroid.adenilton.erbazimuth.ui.viewmodel.MapViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa as dependências necessárias para a arquitetura MVVM
        val database = ErbAzimuthDatabase.getDatabase(this)
        // ATUALIZADO: Passa o 'applicationContext' para o repositório
        val repository = ErbRepository(database.erbAzimuthDao(), applicationContext)
        val viewModelFactory = MapViewModelFactory(repository)
        val mapViewModel = ViewModelProvider(this, viewModelFactory)[MapViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            ErbAzimuthTheme {
                // Inicia a tela principal, passando o ViewModel
                MapScreen(viewModel = mapViewModel)
            }
        }
    }
}

