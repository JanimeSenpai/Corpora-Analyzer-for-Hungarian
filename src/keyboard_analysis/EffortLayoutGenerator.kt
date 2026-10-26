package keyboard_analysis

import kotlin.math.abs
import kotlin.math.hypot

// 1. A Colemak-DH matematikai súlyai
data class EffortWeights(
    val lateralPenalty: Double = 1.5,   // Oldalirányú (X tengely) nyújtózás súlya
    val upwardPenalty: Double = 1.0,    // Felsőbb sorokba nyúlás súlya (-Y irány)
    val downwardPenalty: Double = 2.0,  // Alsó sorokba görbítés súlya (+Y irány)
    val distanceMultiplier: Double = 0.5 // A tiszta euklideszi távolság (reach) bónusz büntetése
)

// 2. Az oszlopok geometriai és anatómiai viselkedése
data class ColumnConfig(
    val colIndex: Int,
    val anchorColIndex: Int,     // A home pozíció oszlopa. A belső mutatóujj oszlopa pl. a sima mutatóujj oszlopára mutat.
    val baseFingerEffort: Double // Az ujj anatómiai alapbüntetése (pl. mutatóujj: 0.0, kisujj: 2.0)
)

// 3. A fizikai terhelés generátora
class EffortLayoutGenerator(
    private val keyboardHalf: KeyboardHalf,
    private val columnConfigs: List<ColumnConfig>,
    private val weights: EffortWeights = EffortWeights()
) {
    /**
     * Kiszámolja egyetlen fizikai gomb terhelését az ujj alaphelyzetéhez képest.
     */
    fun calculateEffort(targetRow: Int, targetCol: Int): Double {
        // Megkeressük az oszlophoz rendelt ujj-konfigurációt
        val config = columnConfigs.find { it.colIndex == targetCol }
            ?: throw IllegalArgumentException("Hiányzó konfiguráció a(z) $targetCol. oszlophoz!")

        // 1. A célgomb tényleges (x,y) koordinátája a hardveren
        val targetCoord = keyboardHalf.getPhysicalCoordinate(targetRow, targetCol)

        // 2. Az ujj pihenő (home) pozíciója a hardveren
        val homeCoord = keyboardHalf.getPhysicalCoordinate(keyboardHalf.homeRowIndex, config.anchorColIndex)

        // 3. A fizikai elmozdulás vektorai
        val dx = targetCoord.x - homeCoord.x
        val dy = targetCoord.y - homeCoord.y

        // 4. Irányított anatómiai büntetések alkalmazása
        val lateralCost = abs(dx) * weights.lateralPenalty

        val verticalCost = when {
            dy > 0 -> dy * weights.downwardPenalty    // Lefelé mozgás (+Y irány a mátrixban)
            dy < 0 -> abs(dy) * weights.upwardPenalty // Felfelé mozgás (-Y irány a mátrixban)
            else -> 0.0
        }

        // 5. Euklideszi "reach" büntetés (az átlós kinyúlás miatt)
        val euclideanDistance = hypot(dx, dy)//négyzetösszeg számítás
        val distanceCost = euclideanDistance * weights.distanceMultiplier

        return config.baseFingerEffort + lateralCost + verticalCost + distanceCost
    }

    /**
     * Generál egy teljes 2D hőtérképet a fél-billentyűzethez (O(N*M) sebességgel).
     */
    fun generateEffortMatrix(): List<List<Double>> {
        return (0 until keyboardHalf.numRows).map { rowIndex ->
            (0 until keyboardHalf.numColumns).map { colIndex ->
                calculateEffort(rowIndex, colIndex)
            }
        }
    }
}