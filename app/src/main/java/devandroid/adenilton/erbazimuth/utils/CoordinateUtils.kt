package devandroid.adenilton.erbazimuth.utils

import java.math.RoundingMode
import java.util.Locale

object CoordinateUtils {

    /**
     * Analisa uma string de coordenada e a converte para Graus Decimais,
     * arredondando para 5 casas decimais.
     */
    fun parseCoordinate(coordinateString: String): Double? {
        val trimmedString = coordinateString.trim()

        val dmsRegex = """(-?)(\d+)[°\s-:]+(\d+)[`'\s-:]+(\d+[\.,]?\d*)?""".toRegex()
        val matchResult = dmsRegex.find(trimmedString)

        val result: Double? = if (matchResult != null) {
            try {
                val sign = if (matchResult.groups[1]?.value == "-") -1.0 else 1.0
                val degrees = matchResult.groups[2]?.value?.toDouble() ?: 0.0
                val minutes = matchResult.groups[3]?.value?.toDouble() ?: 0.0
                val seconds = matchResult.groups[4]?.value?.replace(',', '.')?.toDouble() ?: 0.0
                sign * (degrees + (minutes / 60.0) + (seconds / 3600.0))
            } catch (e: NumberFormatException) {
                null
            }
        } else {
            try {
                trimmedString.replace(',', '.').toDouble()
            } catch (e: NumberFormatException) {
                null
            }
        }

        // ATUALIZAÇÃO: Arredonda o resultado para 5 casas decimais
        return result?.toBigDecimal()?.setScale(5, RoundingMode.HALF_UP)?.toDouble()
    }
}

