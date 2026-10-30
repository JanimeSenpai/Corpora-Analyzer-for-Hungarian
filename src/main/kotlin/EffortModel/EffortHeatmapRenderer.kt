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
        leftcolumnConfig: List<ColumnConfig>,
        rightcolumnConfig: List<ColumnConfig>,
        leftChars: Array<CharArray>? = null,   // <--- ÚJ: Opcionális bal karakterek
        rightChars: Array<CharArray>? = null,  // <--- ÚJ: Opcionális jobb karakterek
        outputPath: String = "full_keyboard_heatmap.png"
    ) {
        val keySize = 65
        val padding = 6
        val margin = 40
        val splitGap = 1.5

        val effortlimit = 99.0  //todo ez 99 volt eddig
        val colorlimit =10.0//ezt azért adtam hozzá, hogy ne 1..99 skálán normálja a piros színt, hanem max addig amíg tényleg vannak értékeink

        val leftCoords = (0 until leftHalf.numRows).flatMap { r ->
            (0 until leftHalf.numColumns).map { c -> leftHalf.getPhysicalCoordinate(r, c) }
        }
        val rightCoords = (0 until rightHalf.numRows).flatMap { r ->
            (0 until rightHalf.numColumns).map { c -> rightHalf.getPhysicalCoordinate(r, c) }
        }

        val leftMinX = leftCoords.minOfOrNull { it.x } ?: 0.0
        val leftMaxX = leftCoords.maxOfOrNull { it.x } ?: 0.0
        val rightMinX = rightCoords.minOfOrNull { it.x } ?: 0.0
        val rightMaxX = rightCoords.maxOfOrNull { it.x } ?: 0.0

        val globalMinY = (leftCoords + rightCoords).minOfOrNull { it.y } ?: 0.0
        val globalMaxY = (leftCoords + rightCoords).maxOfOrNull { it.y } ?: 0.0

        val allEfforts = leftEffort.flatten() + rightEffort.flatten()
        // Az effort < 99.0 szűrő biztosítja, hogy a dummy értékeket ne vegyük bele a színskálába
        val validEfforts = allEfforts.filter { it < colorlimit }//a colorlimit pedig a kiugró értékeket szűri ki
        val minEffort = 1.0
        val maxEffort = max(validEfforts.maxOrNull() ?: 5.0, 4.5)

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

        // Betűtípusok a kétféle állapothoz
        val fontCharBig = Font("SansSerif", Font.BOLD, 22)
        val fontEffortSmall = Font("SansSerif", Font.BOLD, 12)
        val fontEffortBig = Font("SansSerif", Font.BOLD, 16)

        // Bővített drawKey függvény, ami már elfogadja a karaktert is
        fun drawKey(normX: Double, normY: Double, effort: Double, isHomeKey: Boolean, keyChar: Char?) {
            val pixelX = margin + (normX * keySize).roundToInt()
            val pixelY = margin + (normY * keySize).roundToInt()
            val boxSize = keySize - padding

            val ratio = ((effort - minEffort) / (maxEffort - minEffort)).toFloat().coerceIn(0f, 1f)
            val red = if (ratio < 0.5f) (255 * (ratio * 2)).toInt() else 255
            val green = if (ratio > 0.5f) (255 * (1f - (ratio - 0.5f) * 2)).toInt() else 255

            g2d.color = Color(red.coerceIn(0, 255), green.coerceIn(0, 255), 60)
            g2d.fillRoundRect(pixelX, pixelY, boxSize, boxSize, 14, 14)

            if (isHomeKey) {
                g2d.color = Color(220, 40, 40)
                g2d.drawRoundRect(pixelX, pixelY, boxSize, boxSize, 14, 14)
            }

            val effortText = String.format(Locale.US, "%.1f", effort)

            // Ha van betűnk (és az nem alulvonás vagy dummy '#'), akkor a betűt tesszük középre
            if (keyChar != null && keyChar != '_' && keyChar != '#') {
                // 1. Karakter középre
                g2d.color = Color(20, 20, 20)
                g2d.font = fontCharBig
                val fmChar = g2d.fontMetrics
                val charStr = keyChar.toString().uppercase()
                val textX = pixelX + (boxSize - fmChar.stringWidth(charStr)) / 2
                val textY = pixelY + ((boxSize - fmChar.height) / 2) + fmChar.ascent
                g2d.drawString(charStr, textX, textY)

                // 2. Fáradtság (Effort) a jobb felső sarokba
                g2d.color = Color(20, 20, 20, 160) // Kicsit áttetszőbb fekete
                g2d.font = fontEffortSmall
                val fmEffort = g2d.fontMetrics
                val effX = pixelX + boxSize - fmEffort.stringWidth(effortText) - 4
                val effY = pixelY + fmEffort.ascent + 2
                g2d.drawString(effortText, effX, effY)
            } else {
                // Eredeti viselkedés: Csak az effort van középen
                g2d.color = Color(20, 20, 20)
                g2d.font = fontEffortBig
                val fm = g2d.fontMetrics
                val textX = pixelX + (boxSize - fm.stringWidth(effortText)) / 2
                val textY = pixelY + ((boxSize - fm.height) / 2) + fm.ascent
                g2d.drawString(effortText, textX, textY)
            }
        }

        val leftHomeCols = leftcolumnConfig.filter { it.finger == Finger.RING || it.finger == Finger.MIDDLE }.map { it.anchorColIndex }
        val leftHomeRange = ((leftHomeCols.minOrNull() ?: 0) - 1)..((leftHomeCols.maxOrNull() ?: 0) + 1)

        val rightHomeCols = rightcolumnConfig.filter { it.finger == Finger.RING || it.finger == Finger.MIDDLE }.map { it.anchorColIndex }
        val rightHomeRange = ((rightHomeCols.minOrNull() ?: 0) - 1)..((rightHomeCols.maxOrNull() ?: 0) + 1)

        // Bal kéz kirajzolása (Karakterek biztonságos lekérésével)
        for (r in 0 until leftHalf.numRows) {
            for (c in 0 until leftHalf.numColumns) {
                if (leftEffort[r][c] < effortlimit) { // Biztonsági szűrés a dummy (rongyos tömbös) gombokra
                    val coord = leftHalf.getPhysicalCoordinate(r, c)
                    val isHome = (r == leftHalf.homeRowIndex && c in leftHomeRange)
                    val charToDraw = if (leftChars != null && r < leftChars.size && c < leftChars[r].size) leftChars[r][c] else null
                    drawKey(coord.x - leftMinX, coord.y - globalMinY, leftEffort[r][c], isHome, charToDraw)
                }
            }
        }

        // Jobb kéz kirajzolása
        for (r in 0 until rightHalf.numRows) {
            for (c in 0 until rightHalf.numColumns) {
                if (rightEffort[r][c] < effortlimit) {
                    val coord = rightHalf.getPhysicalCoordinate(r, c)
                    val isHome = (r == rightHalf.homeRowIndex && c in rightHomeRange)
                    val charToDraw = if (rightChars != null && r < rightChars.size && c < rightChars[r].size) rightChars[r][c] else null
                    drawKey(rightStartX + (coord.x - rightMinX), coord.y - globalMinY, rightEffort[r][c], isHome, charToDraw)
                }
            }
        }

        g2d.dispose()
        ImageIO.write(image, "png", File(outputPath))
    }
}