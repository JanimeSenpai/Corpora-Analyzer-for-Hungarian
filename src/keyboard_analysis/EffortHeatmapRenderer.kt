package keyboard_analysis

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.math.max

object EffortHeatmapRenderer {
    fun renderFullKeyboard(
        leftHalf: KeyboardHalf,
        rightHalf: KeyboardHalf,
        leftEffort: List<List<Double>>,
        rightEffort: List<List<Double>>,
        outputPath: String = "full_keyboard_heatmap.png"
    ) {
        val keySize = 65
        val padding = 5
        val margin = 40
        val splitGap = 2.0 // Két gombnyi távolság a két kéz között a képen

        // Bal oldal maximumainak keresése
        var leftMaxX = 0.0
        var maxY = 0.0
        var maxEffort = 0.0

        for (r in 0 until leftHalf.numRows) {
            for (c in 0 until leftHalf.numColumns) {
                val coord = leftHalf.getPhysicalCoordinate(r, c)
                if (coord.x > leftMaxX) leftMaxX = coord.x
                if (coord.y > maxY) maxY = coord.y
                if (leftEffort[r][c] > maxEffort) maxEffort = leftEffort[r][c]
            }
        }

        // Jobb oldal kezdő X pozíciója a képen
        val rightStartX = leftMaxX + splitGap

        // Jobb oldal maximumainak keresése
        var rightMaxX = 0.0
        for (r in 0 until rightHalf.numRows) {
            for (c in 0 until rightHalf.numColumns) {
                val coord = rightHalf.getPhysicalCoordinate(r, c)
                if (coord.x > rightMaxX) rightMaxX = coord.x
                if (coord.y > maxY) maxY = coord.y
                if (rightEffort[r][c] > maxEffort) maxEffort = rightEffort[r][c]
            }
        }

        val totalWidth = margin * 2 + ((rightStartX + rightMaxX + 1) * keySize).toInt()
        val totalHeight = margin * 2 + ((maxY + 1) * keySize).toInt()

        val image = BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        g2d.color = Color(35, 35, 40)
        g2d.fillRect(0, 0, totalWidth, totalHeight)
        g2d.font = Font("SansSerif", Font.BOLD, 16)

        // Helper függvény egyetlen gomb kirajzolásához
        fun drawKey(xOffset: Double, coord: Coordinate, effort: Double) {
            val pixelX = margin + ((xOffset + coord.x) * keySize).toInt()
            val pixelY = margin + (coord.y * keySize).toInt()
            val ratio = if (maxEffort > 0) (effort / maxEffort).toFloat().coerceIn(0f, 1f) else 0f

            val red = if (ratio < 0.5f) (255 * (ratio * 2)).toInt() else 255
            val green = if (ratio > 0.5f) (255 * (1f - (ratio - 0.5f) * 2)).toInt() else 255
            g2d.color = Color(red.coerceIn(0, 255), green.coerceIn(0, 255), 50)

            g2d.fillRoundRect(pixelX, pixelY, keySize - padding, keySize - padding, 15, 15)

            g2d.color = Color.BLACK
            val text = String.format(Locale.US, "%.1f", effort)
            val fm = g2d.fontMetrics
            val textX = pixelX + (keySize - padding - fm.stringWidth(text)) / 2
            val textY = pixelY + ((keySize - padding - fm.height) / 2) + fm.ascent
            g2d.drawString(text, textX, textY)
        }

        // Bal kéz rajzolása (nincs X eltolás)
        for (r in 0 until leftHalf.numRows) {
            for (c in 0 until leftHalf.numColumns) {
                drawKey(0.0, leftHalf.getPhysicalCoordinate(r, c), leftEffort[r][c])
            }
        }

        // Jobb kéz rajzolása (rightStartX eltolással)
        for (r in 0 until rightHalf.numRows) {
            for (c in 0 until rightHalf.numColumns) {
                drawKey(rightStartX, rightHalf.getPhysicalCoordinate(r, c), rightEffort[r][c])
            }
        }

        g2d.dispose()
        ImageIO.write(image, "png", File(outputPath))
    }
}