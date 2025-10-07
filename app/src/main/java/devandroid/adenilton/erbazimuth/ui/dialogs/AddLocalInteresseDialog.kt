package devandroid.adenilton.erbazimuth.ui.dialogs

import android.location.Address
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import devandroid.adenilton.erbazimuth.ui.viewmodel.MapViewModel

@Composable
fun AddLocalInteresseDialog(
    viewModel: MapViewModel,
    onDismiss: () -> Unit
) {
    val searchedAddress by viewModel.searchedAddress.collectAsState()
    val isSearching by viewModel.isSearchingAddress.collectAsState()

    var nome by rememberSaveable { mutableStateOf("") }
    var endereco by rememberSaveable { mutableStateOf("") }
    var nomeError by remember { mutableStateOf<String?>(null) }
    var enderecoError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Local de Interesse") },
        text = {
            Column {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it; nomeError = null },
                    label = { Text("Nome do Local (Ex: Alvo, Crime)") },
                    isError = nomeError != null,
                    supportingText = { nomeError?.let { Text(it) } }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = endereco,
                    onValueChange = { endereco = it; enderecoError = null },
                    label = { Text("Endereço (Rua, N°, Cidade)") },
                    isError = enderecoError != null,
                    supportingText = { enderecoError?.let { Text(it) } }
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (endereco.isNotBlank()) {
                            viewModel.onSearchAddress(endereco)
                        } else {
                            enderecoError = "Digite um endereço para buscar."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Buscar Endereço")
                    }
                }

                // Área de confirmação do endereço encontrado
                searchedAddress?.let { address ->
                    Spacer(Modifier.height(16.dp))
                    Text("Endereço encontrado:", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = address.getAddressLine(0) ?: "Confirme se este é o local correto.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var hasError = false
                    if (nome.isBlank()) {
                        nomeError = "O nome é obrigatório."
                        hasError = true
                    }
                    if (searchedAddress == null) {
                        enderecoError = "Você precisa buscar e confirmar um endereço."
                        hasError = true
                    }
                    if (!hasError) {
                        viewModel.onConfirmAddLocal(nome)
                    }
                },
                // O botão só fica habilitado após uma busca bem-sucedida
                enabled = searchedAddress != null
            ) {
                Text("Salvar Local")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
