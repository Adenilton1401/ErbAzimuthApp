package devandroid.adenilton.erbazimuth.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import devandroid.adenilton.erbazimuth.data.model.Caso

@Composable
fun AddCaseDialog(
    onDismiss: () -> Unit,
    onConfirm: (Caso) -> Unit
) {
    var caseName by rememberSaveable { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Caso de Investigação") },
        text = {
            Column {
                Text("Digite um nome ou número para o novo caso.")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = caseName,
                    onValueChange = { caseName = it; nameError = null },
                    label = { Text("Nome do Caso") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it) } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (caseName.isNotBlank()) {
                        onConfirm(Caso(nome = caseName))
                    } else {
                        nameError = "O nome não pode ser vazio."
                    }
                }
            ) { Text("Criar") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
