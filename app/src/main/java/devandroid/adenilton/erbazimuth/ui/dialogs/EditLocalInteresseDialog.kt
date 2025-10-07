package devandroid.adenilton.erbazimuth.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import devandroid.adenilton.erbazimuth.data.model.LocalInteresse

@Composable
fun EditLocalInteresseDialog(
    localToEdit: LocalInteresse,
    onDismiss: () -> Unit,
    onConfirm: (LocalInteresse) -> Unit
) {
    var nome by rememberSaveable { mutableStateOf(localToEdit.nome) }
    // A edição do endereço não é permitida para manter a consistência com as coordenadas.
    // O usuário deve excluir e criar um novo se o endereço estiver errado.
    val endereco = localToEdit.endereco

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Local de Interesse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do Local") }
                )
                OutlinedTextField(
                    value = endereco,
                    onValueChange = {},
                    label = { Text("Endereço") },
                    enabled = false // O endereço não pode ser alterado
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nome.isNotBlank()) {
                        onConfirm(localToEdit.copy(nome = nome))
                    }
                }
            ) { Text("Salvar") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
