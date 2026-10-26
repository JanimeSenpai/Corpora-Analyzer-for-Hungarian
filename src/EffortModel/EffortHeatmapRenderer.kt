package EffortModel

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

object EffortHeatmapRenderer {
    fun renderFullKeyboard(
        leftHalf: KeyboardHalf,
        rightHalf: KeyboardHalf,
        leftEffort: List<List<Double>>,
        rightEffort: List<List<Double>>,
        outputPath: String = "full_keyboard_heatmap.png"
    ) {
        val keySize = 65
        val padding = 6
        val margin = 40
        val splitGap = 1.5 // Két kéz közötti távolság gomb-egységben (u)

        val leftCoords = (0 until leftHalf.numRows).flatMap { r ->
            (0 until leftHalf.numColumns).map { c -> leftHalf.getPhysicalCoordinate(r, c) }
        }
        val rightCoords = (0 until rightHalf.numRows).flatMap { r ->
            (0 until rightHalf.numColumns).map { c -> rightHalf.getPhysicalCoordinate(r, c) }
        }

        // Minimumok és maximumok meghatározása a lecsúszásmentes normalizáláshoz
        val leftMinX = leftCoords.minOfOrNull { it.x } ?: 0.0
        val leftMaxX = leftCoords.maxOfOrNull { it.x } ?: 0.0
        val rightMinX = rightCoords.minOfOrNull { it.x } ?: 0.0
        val rightMaxX = rightCoords.maxOfOrNull { it.x } ?: 0.0

        val globalMinY = (leftCoords + rightCoords).minOfOrNull { it.y } ?: 0.0
        val globalMaxY = (leftCoords + rightCoords).maxOfOrNull { it.y } ?: 0.0

        val allEfforts = leftEffort.flatten() + rightEffort.flatten()
        val minEffort = 1.0 // A Colemak-DH skála minimuma (mutatóujj alapsor)
        val maxEffort = max(allEfforts.maxOrNull() ?: 5.0, 4.5)

        val leftSpanX = leftMaxX - leftMinX
        val rightSpanX = rightMaxX - rightMinX
        val rightStartX = leftSpanX + 1.0 + splitGap

        val totalUnitsX = rightStartX + rightSpanX + 1.0
        val totalUnitsY = (globalMaxY - globalMinY) + 1.0

        val totalWidth = margin * 2 + ceil(totalUnitsX * keySize).toInt()
        val totalHeight = margin * 2 + ceil(totalUnitsY * keySize).toInt()

        val image = BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        g2d.color = Color(35, 35, 40)
        g2d.fillRect(0, 0, totalWidth, totalHeight)
        g2d.font = Font("SansSerif", Font.BOLD, 16)

        fun drawKey(normX: Double, normY: Double, effort: Double, isHomeKey: Boolean) {
            val pixelX = margin + (normX * keySize).roundToInt()
            val pixelY = margin + (normY * keySize).roundToInt()
            val boxSize = keySize - padding

            val ratio = ((effort - minEffort) / (maxEffort - minEffort)).toFloat().coerceIn(0f, 1f)
            val red = if (ratio < 0.5f) (255 * (ratio * 2)).toInt() else 255
            val green = if (ratio > 0.5f) (255 * (1f - (ratio - 0.5f) * 2)).toInt() else 255

            g2d.color = Color(red.coerceIn(0, 255), green.coerceIn(0, 255), 60)
            g2d.fillRoundRect(pixelX, pixelY, boxSize, boxSize, 14, 14)

            // Az alapsor 8 fő gombját vékony kerettel kiemeljük a könnyebb tájékozódásért
            if (isHomeKey) {
                g2d.color = Color(220, 40, 40)
                g2d.drawRoundRect(pixelX, pixelY, boxSize, boxSize, 14, 14)
            }

            g2d.color = Color(20, 20, 20)
            val text = String.format(Locale.US, "%.1f", effort)
            val fm = g2d.fontMetrics
            val textX = pixelX + (boxSize - fm.stringWidth(text)) / 2
            val textY = pixelY + ((boxSize - fm.height) / 2) + fm.ascent
            g2d.drawString(text, textX, textY)
        }

        // Bal kéz kirajzolása
        for (r in 0 until leftHalf.numRows) {
            for (c in 0 until leftHalf.numColumns) {
                val coord = leftHalf.getPhysicalCoordinate(r, c)
                val isHome = (r == leftHalf.homeRowIndex && c in 1..4)
                drawKey(coord.x - leftMinX, coord.y - globalMinY, leftEffort[r][c], isHome)
            }
        }

        // Jobb kéz kirajzolása
        for (r in 0 until rightHalf.numRows) {
            for (c in 0 until rightHalf.numColumns) {
                val coord = rightHalf.getPhysicalCoordinate(r, c)
                val isHome = (r == rightHalf.homeRowIndex && c in 1..4)
                drawKey(rightStartX + (coord.x - rightMinX), coord.y - globalMinY, rightEffort[r][c], isHome)
            }
        }

        g2d.dispose()
        ImageIO.write(image, "png", File(outputPath))
    }
}