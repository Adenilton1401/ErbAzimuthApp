package devandroid.adenilton.erbazimuth.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import devandroid.adenilton.erbazimuth.R
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
            // Título dinâmico
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

            // Botões de ação atualizados para usar ícones customizados
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (item is Erb) {
                    ActionIconButton(text = "Ad. Azimute", onClick = { onAddAzimuthClick(item) }) {
                        Icon(Icons.Default.AddCircle,
                            contentDescription = "Adicionar Azimute",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    ActionIconButton(text = "Rota", onClick = { onNavigateClick(item) }) {
                        Icon(painterResource(id = R.drawable.ic_navigation), contentDescription = "Criar Rota")
                    }
                }
                if (item !is CellTowerInfo) {
                    ActionIconButton(text = "Editar", onClick = { onEditClick(item) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    ActionIconButton(
                        text = "Excluir",
                        onClick = { onDeleteClick(item) },
                        contentColor = MaterialTheme.colorScheme.error
                    ) {
                        Icon(Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp))
                    }
                }
            }

            Divider()
            Spacer(Modifier.height(8.dp))

            // Detalhes do item
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

// --- ATUALIZADO: Componente reutilizável para um botão de ação ---
@Composable
private fun ActionIconButton(
    text: String,
    onClick: () -> Unit,
    contentColor: Color = LocalContentColor.current,
    icon: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            icon()
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = contentColor
        )
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

