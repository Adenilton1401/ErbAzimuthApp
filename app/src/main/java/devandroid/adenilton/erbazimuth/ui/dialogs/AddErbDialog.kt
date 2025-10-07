package devandroid.adenilton.erbazimuth.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
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
    itemToProcess: Any? = null,
    erbForContext: Erb? = null,
    onDismiss: () -> Unit,
    onConfirm: (Erb, Azimute, Azimute?) -> Unit
) {
    val azimuteToEdit = itemToProcess as? Azimute
    val isErbEditable = itemToProcess == null

    val sourceErb = erbForContext ?: (itemToProcess as? Erb)
    var erbId by rememberSaveable { mutableStateOf(sourceErb?.identificacao ?: "") }
    var latitude by rememberSaveable { mutableStateOf(sourceErb?.latitude?.toString() ?: "") }
    var longitude by rememberSaveable { mutableStateOf(sourceErb?.longitude?.toString() ?: "") }

    var azimuteDesc by rememberSaveable { mutableStateOf(azimuteToEdit?.descricao ?: "") }
    var azimuteValor by rememberSaveable { mutableStateOf(azimuteToEdit?.azimute?.toString() ?: "") }
    var raio by rememberSaveable { mutableStateOf(azimuteToEdit?.raio?.toString() ?: "") }

    val colors = remember {
        listOf(
            Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta, Color.Cyan,
            Color(0xFFFFA500), // Laranja
            Color(0xFF800080), // Roxo
            Color(0xFF008080), // Teal
            Color(0xFFD2691E), // Chocolate
            Color(0xFF4682B4), // SteelBlue
            Color(0xFFDC143C)  // Crimson
        )
    }

    val ColorSaver = Saver<Color, Long>(save = { it.value.toLong() }, restore = { Color(it.toULong()) })
    var selectedColor by rememberSaveable(stateSaver = ColorSaver) { mutableStateOf(azimuteToEdit?.cor?.let { Color(it.toULong()) } ?: colors.first()) }

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

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val middleIndex = Int.MAX_VALUE / 2
        val initialColorIndex = colors.indexOf(selectedColor)
        val targetIndex = if (initialColorIndex != -1) {
            middleIndex - (middleIndex % colors.size) + initialColorIndex
        } else {
            middleIndex
        }
        listState.scrollToItem(targetIndex)
    }

    fun validateAllFields(): Boolean {
        if (isErbEditable) {
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
        title = {
            Text(
                when {
                    azimuteToEdit != null -> "Editar Azimute"
                    sourceErb != null -> "Adicionar Novo Azimute"
                    else -> "Adicionar ERB e Azimute"
                }
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = erbId, onValueChange = { erbId = it; erbIdError = null }, label = { Text("Identificação da ERB") }, enabled = isErbEditable, isError = erbIdError != null, supportingText = { erbIdError?.let { Text(it) } }, modifier = Modifier.focusRequester(erbIdFocusRequester))
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it; latitudeError = null },
                        label = { Text("Latitude (-90 a 90)") },
                        enabled = isErbEditable,
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
                        enabled = isErbEditable,
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
                    OutlinedTextField(value = azimuteDesc, onValueChange = { azimuteDesc = it }, label = { Text("Descrição do Azimute") })
                    OutlinedTextField(
                        value = azimuteValor,
                        onValueChange = { azimuteValor = it; azimuteError = null },
                        label = { Text("Azimute (0-360°)") },
                        isError = azimuteError != null,
                        supportingText = { azimuteError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    LazyRow(
                        state = listState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(Int.MAX_VALUE) { index ->
                            val color = colors[index % colors.size]
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        width = 3.dp,
                                        color = if (selectedColor == color) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateAllFields()) {
                        // ATUALIZADO: Usa a ERB de contexto se disponível, senão cria uma nova com um casoId temporário.
                        val finalErb = erbForContext ?: Erb(
                            casoId = 0L, // ID temporário, será substituído pelo ViewModel
                            identificacao = erbId,
                            latitude = latitude.replace(',', '.').toDouble(),
                            longitude = longitude.replace(',', '.').toDouble()
                        )
                        val finalAzimute = azimuteToEdit?.copy(
                            descricao = azimuteDesc,
                            azimute = azimuteValor.replace(',', '.').toDouble(),
                            raio = raio.replace(',', '.').toDouble(),
                            cor = selectedColor.value.toLong()
                        ) ?: Azimute(
                            descricao = azimuteDesc,
                            azimute = azimuteValor.replace(',', '.').toDouble(),
                            raio = raio.replace(',', '.').toDouble(),
                            cor = selectedColor.value.toLong()
                        )
                        onConfirm(finalErb, finalAzimute, azimuteToEdit)
                    }
                }
            ) { Text("Salvar") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

