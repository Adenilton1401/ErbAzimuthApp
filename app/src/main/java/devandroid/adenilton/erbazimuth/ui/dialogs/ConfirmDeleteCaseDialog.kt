package devandroid.adenilton.erbazimuth.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import devandroid.adenilton.erbazimuth.data.model.Caso

@Composable
fun ConfirmDeleteCaseDialog(
    casoToDelete: Caso,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Exclusão") },
        text = {
            Text("Você tem certeza que deseja excluir o caso '${casoToDelete.nome}'? Todos os dados associados (ERBs, azimutes e locais de interesse) serão permanentemente removidos.")
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
