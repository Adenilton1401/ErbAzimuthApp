package devandroid.adenilton.erbazimuth.utils

import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil.computeOffset

object MapUtils {
    /**
     * Calcula os pontos de um polígono que representa um setor circular.
     * @return Uma lista de LatLng que formam o polígono do setor.
     */
    fun calculateAzimuthSectorPoints(
        center: LatLng,
        radius: Double, // Raio em metros
        azimuth: Float, // Azimute em graus
        angle: Float = 20f // Abertura do setor em graus
    ): List<LatLng> {
        val points = mutableListOf<LatLng>()
        points.add(center) // Ponto central

        val halfAngle = angle / 2.0
        val startAngle = azimuth - halfAngle

        // Adiciona pontos ao longo do arco
        for (i in 0..angle.toInt()) {
            val currentAngle = startAngle + i
            val point = computeOffset(center, radius, currentAngle.toDouble())
            points.add(point)
        }

        points.add(center) // Fecha o polígono no centro
        return points
    }
}

