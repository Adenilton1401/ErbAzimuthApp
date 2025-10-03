package devandroid.adenilton.erbazimuth.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
//import androidx.compose.material.icons.filled.Navigation
//import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.Erb

@Composable
fun ItemDetailsSheet(
    item: Any,
    onDismiss: () -> Unit,
    onEditClick: (Any) -> Unit,
    onDeleteClick: (Any) -> Unit,
    // NOVO PARÂMETRO: para a ação de navegação
    onNavigateClick: (Erb) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Cabeçalho com o título e os botões de ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (item is Erb) "Detalhes da ERB" else "Detalhes do Azimute",
                    style = MaterialTheme.typography.titleLarge
                )
                Row {
                    // NOVO BOTÃO: Só aparece se o item for uma ERB
                    if (item is Erb) {
                        IconButton(onClick = { onNavigateClick(item) }) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Criar Rota")
                        }
                    }
                    IconButton(onClick = { onEditClick(item) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { onDeleteClick(item) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Corpo com os detalhes específicos do item
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Usamos 'when' para mostrar os detalhes corretos para cada tipo de item
                when (item) {
                    is Erb -> {
                        DetailItem("Identificação:", item.identificacao)
                        DetailItem("Latitude:", item.latitude.toString())
                        DetailItem("Longitude:", item.longitude.toString())
                        // NOVO CAMPO: Exibe o endereço
                        DetailItem("Endereço:", item.endereco ?: "Buscando...")
                    }
                    is Azimute -> {
                        DetailItem("Descrição:", item.descricao)
                        DetailItem("Azimute:", "${item.azimute}°")
                        DetailItem("Raio:", "${item.raio} metros")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Cor:", fontWeight = FontWeight.Bold, modifier = Modifier.width(120.dp))
                            Box(modifier = Modifier.size(20.dp).background(Color(item.cor.toULong())))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    }
}

// Componente auxiliar para mostrar um item de detalhe (Rótulo + Valor)
@Composable
private fun DetailItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(120.dp)
        )
        Text(text = value)
    }
}

