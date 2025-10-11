package devandroid.adenilton.erbazimuth.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.CellTowerInfo
import devandroid.adenilton.erbazimuth.data.model.Erb
import devandroid.adenilton.erbazimuth.data.model.LocalInteresse

@Composable
fun ItemDetailsSheet(
    item: Any,
    onDismiss: () -> Unit,
    onEditClick: (Any) -> Unit,
    onDeleteClick: (Any) -> Unit,
    onNavigateClick: (Erb) -> Unit,
    onAddAzimuthClick: (Erb) -> Unit
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
            // Título dinâmico com base no tipo do item
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when(item) {
                        is Erb -> "Detalhes da ERB"
                        is Azimute -> "Detalhes do Azimute"
                        is LocalInteresse -> "Detalhes do Local"
                        is CellTowerInfo -> "Dados da Torre Conectada"
                        else -> "Detalhes"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(Modifier.height(8.dp))
            Divider()

            // Botões de ação com texto (agora condicionais)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (item is Erb) {
                    ActionIconButton(text = "Ad. Azimute", icon = Icons.Default.LocationOn, contentDescription = "Adicionar Azimute", onClick = { onAddAzimuthClick(item) })
                    ActionIconButton(text = "Rota", icon = Icons.Default.ArrowDropDown, contentDescription = "Criar Rota", onClick = { onNavigateClick(item) })
                }
                // Botões de editar e excluir não aparecem para CellTowerInfo
                if (item !is CellTowerInfo) {
                    ActionIconButton(text = "Editar", icon = Icons.Default.Edit, contentDescription = "Editar", onClick = { onEditClick(item) })
                    ActionIconButton(text = "Excluir", icon = Icons.Default.Delete, contentDescription = "Excluir", onClick = { onDeleteClick(item) }, tint = MaterialTheme.colorScheme.error)
                }
            }

            Divider()
            Spacer(Modifier.height(8.dp))

            // Corpo com os detalhes específicos do item
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (item) {
                    is Erb -> {
                        DetailItem("Identificação:", item.identificacao)
                        DetailItem("Latitude:", item.latitude.toString())
                        DetailItem("Longitude:", item.longitude.toString())
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
                    is LocalInteresse -> {
                        DetailItem("Nome:", item.nome)
                        DetailItem("Endereço:", item.endereco)
                        DetailItem("Latitude:", item.latitude.toString())
                        DetailItem("Longitude:", item.longitude.toString())
                    }
                    // ATUALIZADO: para exibir os detalhes da torre
                    is CellTowerInfo -> {
                        DetailItem("Operadora:", item.operatorName)
                        DetailItem("Sinal:", "${item.signalStrength} dBm")
                        DetailItem("Cell ID:", item.cid.toString())
                        DetailItem("LAC/TAC:", item.lac.toString())
                        DetailItem("MCC:", item.mcc.toString())
                        DetailItem("MNC:", item.mnc.toString())
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Fechar")
            }
        }
    }
}

@Composable
private fun ActionIconButton(
    text: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = LocalContentColor.current
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = text, fontSize = 12.sp, color = tint)
    }
}

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

