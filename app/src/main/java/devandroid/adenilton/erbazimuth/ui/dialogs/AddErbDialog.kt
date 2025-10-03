package devandroid.adenilton.erbazimuth.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.Erb

@Composable
fun AddErbDialog(
    erbToUpdate: Erb? = null,
    onDismiss: () -> Unit,
    onConfirm: (Erb, Azimute) -> Unit
) {
    var erbId by rememberSaveable { mutableStateOf(erbToUpdate?.identificacao ?: "") }
    var latitude by rememberSaveable { mutableStateOf(erbToUpdate?.latitude?.toString() ?: "") }
    var longitude by rememberSaveable { mutableStateOf(erbToUpdate?.longitude?.toString() ?: "") }
    var azimuteDesc by rememberSaveable { mutableStateOf("") }
    var azimuteValor by rememberSaveable { mutableStateOf("") }
    var raio by rememberSaveable { mutableStateOf("") }

    var erbIdError by remember { mutableStateOf<String?>(null) }
    var latitudeError by remember { mutableStateOf<String?>(null) }
    var longitudeError by remember { mutableStateOf<String?>(null) }
    var azimuteError by remember { mutableStateOf<String?>(null) }
    var raioError by remember { mutableStateOf<String?>(null) }

    val erbIdFocusRequester = remember { FocusRequester() }
    val latitudeFocusRequester = remember { FocusRequester() }
    val longitudeFocusRequester = remember { FocusRequester() }
    val azimuteFocusRequester = remember { FocusRequester() }
    val raioFocusRequester = remember { FocusRequester() }

    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta, Color.Cyan)
    val ColorSaver = Saver<Color, Long>(save = { it.value.toLong() }, restore = { Color(it.toULong()) })
    var selectedColor by rememberSaveable(stateSaver = ColorSaver) { mutableStateOf(colors.first()) }

    fun validateAllFields(): Boolean {
        // Valida os campos da ERB apenas se for uma nova ERB
        if (erbToUpdate == null) {
            if (erbId.isBlank()) { erbIdError = "Campo obrigatório"; erbIdFocusRequester.requestFocus(); return false }
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
        }

        // Valida os campos do azimute em ambos os casos
        val raioDouble = raio.replace(',', '.').toDoubleOrNull()
        when {
            raio.isBlank() -> { raioError = "Campo obrigatório"; raioFocusRequester.requestFocus(); return false }
            raioDouble == null -> { raioError = "Valor inválido"; raioFocusRequester.requestFocus(); return false }
            raioDouble <= 0 -> { raioError = "Deve ser maior que zero"; raioFocusRequester.requestFocus(); return false }
        }

        val azimuteDouble = azimuteValor.replace(',', '.').toDoubleOrNull()
        when {
            azimuteValor.isBlank() -> { azimuteError = "Campo obrigatório"; azimuteFocusRequester.requestFocus(); return false }
            azimuteDouble == null -> { azimuteError = "Valor inválido"; azimuteFocusRequester.requestFocus(); return false }
            azimuteDouble !in 0.0..360.0 -> { azimuteError = "Intervalo: 0 a 360"; azimuteFocusRequester.requestFocus(); return false }
        }
        return true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (erbToUpdate == null) "Adicionar ERB e Azimute" else "Adicionar Novo Azimute") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = erbId,
                        onValueChange = { erbId = it; erbIdError = null },
                        label = { Text("Identificação da ERB") },
                        isError = erbIdError != null,
                        supportingText = { erbIdError?.let { Text(it) } },
                        singleLine = true,
                        enabled = erbToUpdate == null,
                        modifier = Modifier.focusRequester(erbIdFocusRequester)
                    )

                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it; latitudeError = null },
                        label = { Text("Latitude (-90 a 90)") },
                        isError = latitudeError != null,
                        supportingText = { latitudeError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = erbToUpdate == null,
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
                        singleLine = true,
                        enabled = erbToUpdate == null,
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

                    OutlinedTextField(
                        value = azimuteDesc,
                        onValueChange = { azimuteDesc = it },
                        label = { Text("Descrição do Azimute") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = azimuteValor,
                        onValueChange = { azimuteValor = it; azimuteError = null },
                        label = { Text("Azimute (0-360°)") },
                        isError = azimuteError != null,
                        supportingText = { azimuteError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .focusRequester(azimuteFocusRequester)
                            .onFocusChanged {
                                if (!it.isFocused && azimuteValor.isNotBlank()) {
                                    val az = azimuteValor.replace(',', '.').toDoubleOrNull()
                                    azimuteError = when {
                                        az == null -> "Valor inválido"
                                        az !in 0.0..360.0 -> "Intervalo: 0 a 360"
                                        else -> null
                                    }
                                }
                            }
                    )

                    OutlinedTextField(
                        value = raio,
                        onValueChange = { raio = it; raioError = null },
                        label = { Text("Raio (metros)") },
                        isError = raioError != null,
                        supportingText = { raioError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .focusRequester(raioFocusRequester)
                            .onFocusChanged {
                                if (!it.isFocused && raio.isNotBlank()) {
                                    val r = raio.replace(',', '.').toDoubleOrNull()
                                    raioError = when {
                                        r == null -> "Valor inválido"
                                        r <= 0 -> "Deve ser maior que zero"
                                        else -> null
                                    }
                                }
                            }
                    )

                    Text("Cor do Setor")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { color ->
                            Box(modifier = Modifier.size(40.dp).background(color, CircleShape).border(width = 2.dp, color = if (selectedColor == color) Color.Black else Color.Transparent, shape = CircleShape).clickable { selectedColor = color })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateAllFields()) {
                        val erb = Erb(
                            identificacao = erbId,
                            latitude = latitude.replace(',', '.').toDouble(),
                            longitude = longitude.replace(',', '.').toDouble()
                        )
                        val azimute = Azimute(
                            descricao = azimuteDesc,
                            azimute = azimuteValor.replace(',', '.').toDouble(),
                            raio = raio.replace(',', '.').toDouble(),
                            cor = selectedColor.value.toLong()
                        )
                        onConfirm(erb, azimute)
                    }
                }
            ) { Text("Salvar") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

