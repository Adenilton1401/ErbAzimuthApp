package devandroid.adenilton.erbazimuth.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import devandroid.adenilton.erbazimuth.data.model.Erb

@Composable
fun EditErbDialog(
    erbToEdit: Erb,
    onDismiss: () -> Unit,
    onConfirm: (Erb) -> Unit
) {
    var identificacao by rememberSaveable { mutableStateOf(erbToEdit.identificacao) }
    var latitude by rememberSaveable { mutableStateOf(erbToEdit.latitude.toString()) }
    var longitude by rememberSaveable { mutableStateOf(erbToEdit.longitude.toString()) }

    // --- ESTADO PARA CONTROLE DE ERROS ---
    var identificacaoError by remember { mutableStateOf<String?>(null) }
    var latitudeError by remember { mutableStateOf<String?>(null) }
    var longitudeError by remember { mutableStateOf<String?>(null) }

    // --- CONTROLE DE FOCO ---
    val identificacaoFocusRequester = remember { FocusRequester() }
    val latitudeFocusRequester = remember { FocusRequester() }
    val longitudeFocusRequester = remember { FocusRequester() }

    // --- FUNÇÃO DE VALIDAÇÃO ---
    fun validateAllFields(): Boolean {
        if (identificacao.isBlank()) {
            identificacaoError = "Campo obrigatório"
            identificacaoFocusRequester.requestFocus()
            return false
        }
        val latDouble = latitude.replace(',', '.').toDoubleOrNull()
        when {
            latitude.isBlank() -> { latitudeError = "Campo obrigatório"; latitudeFocusRequester.requestFocus(); return false }
            latDouble == null -> { latitudeError = "Valor inválido"; latitudeFocusRequester.requestFocus(); return false }
            latDouble !in -90.0..90.0 -> { latitudeError = "Intervalo: -90 a 90"; latitudeFocusRequester.requestFocus(); return false }
        }
        val lonDouble = longitude.replace(',', '.').toDoubleOrNull()
        when {
            longitude.isBlank() -> { longitudeError = "Campo obrigatório"; longitudeFocusRequester.requestFocus(); return false }
            lonDouble == null -> { longitudeError = "Valor inválido"; longitudeFocusRequester.requestFocus(); return false }
            lonDouble !in -180.0..180.0 -> { longitudeError = "Intervalo: -180 a 180"; longitudeFocusRequester.requestFocus(); return false }
        }
        return true
    }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar ERB") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = identificacao,
                    onValueChange = { identificacao = it; identificacaoError = null },
                    label = { Text("Identificação da ERB") },
                    isError = identificacaoError != null,
                    supportingText = { identificacaoError?.let { Text(it) } },
                    modifier = Modifier.focusRequester(identificacaoFocusRequester)
                )
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it; latitudeError = null },
                    label = { Text("Latitude (-90 a 90)") },
                    isError = latitudeError != null,
                    supportingText = { latitudeError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .focusRequester(latitudeFocusRequester)
                        .onFocusChanged {
                            if (!it.isFocused && latitude.isNotBlank()) {
                                val lat = latitude.replace(',', '.').toDoubleOrNull()
                                latitudeError = when {
                                    lat == null -> "Valor inválido"
                                    lat !in -90.0..90.0 -> "Intervalo: -90 a 90"
                                    else -> null
                                }
                            }
                        }
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it; longitudeError = null },
                    label = { Text("Longitude (-180 a 180)") },
                    isError = longitudeError != null,
                    supportingText = { longitudeError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .focusRequester(longitudeFocusRequester)
                        .onFocusChanged {
                            if (!it.isFocused && longitude.isNotBlank()) {
                                val lon = longitude.replace(',', '.').toDoubleOrNull()
                                longitudeError = when {
                                    lon == null -> "Valor inválido"
                                    lon !in -180.0..180.0 -> "Intervalo: -180 a 180"
                                    else -> null
                                }
                            }
                        }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateAllFields()) {
                        val updatedErb = erbToEdit.copy(
                            identificacao = identificacao,
                            latitude = latitude.replace(',', '.').toDouble(),
                            longitude = longitude.replace(',', '.').toDouble()
                        )
                        onConfirm(updatedErb)
                    }
                }
            ) { Text("Salvar Alterações") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

