package devandroid.adenilton.erbazimuth.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.Erb

@Composable
fun ConfirmDeleteDialog(
    itemToDelete: Any,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Exclusão") },
        text = {
            val itemName = when(itemToDelete) {
                is Erb -> "a ERB '${itemToDelete.identificacao}' e todos os seus azimutes"
                is Azimute -> "o azimute '${itemToDelete.descricao}'"
                else -> "este item"
            }
            Text("Você tem certeza que deseja excluir $itemName?")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Excluir")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
