package devandroid.adenilton.erbazimuth.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import devandroid.adenilton.erbazimuth.data.model.Caso
import devandroid.adenilton.erbazimuth.ui.dialogs.AddCaseDialog
import devandroid.adenilton.erbazimuth.ui.dialogs.EditCaseDialog
import devandroid.adenilton.erbazimuth.ui.viewmodel.CaseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseListScreen(
    viewModel: CaseViewModel,
    onCaseSelected: (Long) -> Unit
) {
    val cases by viewModel.cases.collectAsState()
    val showAddDialog by viewModel.showAddCaseDialog.collectAsState()
    val caseToEdit by viewModel.caseToEdit.collectAsState()

    if (showAddDialog) {
        AddCaseDialog(
            onDismiss = { viewModel.onDismissAddCaseDialog() },
            onConfirm = { viewModel.onConfirmAddCase(it) }
        )
    }

    if (caseToEdit != null) {
        EditCaseDialog(
            casoToEdit = caseToEdit!!,
            onDismiss = { viewModel.onDismissEditDialog() },
            onConfirm = { viewModel.onConfirmEdit(it) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Casos de Investigação") },
                // O botão de ação foi removido daqui
                actions = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onShowAddCaseDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Caso")
            }
        }
    ) { padding ->
        if (cases.isEmpty()) {
            Box(
                modifier = Modifier.Companion.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Companion.Center
            ) {
                Text("Nenhum caso encontrado. Crie um novo para começar.")
            }
        } else {
            LazyColumn(modifier = Modifier.Companion.padding(padding)) {
                items(cases) { caso ->
                    CaseListItem(
                        caso = caso,
                        onClick = { onCaseSelected(caso.id) },
                        onEditClick = { viewModel.onEditRequest(caso) },
                        onDeleteClick = { viewModel.deleteCase(caso) }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
fun CaseListItem(
    caso: Caso,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        Column(modifier = Modifier.Companion.weight(1f)) {
            Text(text = caso.nome, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Criado em: ${dateFormat.format(Date(caso.dataCriacao))}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row {
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Editar Caso")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir Caso",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}