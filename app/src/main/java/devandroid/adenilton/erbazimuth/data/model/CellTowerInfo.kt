package devandroid.adenilton.erbazimuth.data.model

// Modelo de dados para guardar as informações da torre
data class CellTowerInfo(
    val mcc: Int, // Mobile Country Code
    val mnc: Int, // Mobile Network Code
    val lac: Int, // Location Area Code
    val cid: Int, // Cell ID
    val signalStrength: Int, // Intensidade do Sinal (dBm)
    val operatorName: String // Nome da operadora (Ex: "TIM", "Claro")
)

