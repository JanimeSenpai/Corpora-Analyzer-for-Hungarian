package EffortModel

import kotlin.math.*

data class FingerAnatomy(
    val baseEffort: Double, // Pf
    val homeOffsetX: Double,
    val homeOffsetY: Double
)

class ColemakDhEffortGenerator(
    private val keyboardHalf: KeyboardHalf,
    private val columnConfigs: List<ColumnConfig>,
    private val angleOfApproachDegrees: Double = 10.0,
    private val lateralPenaltyPx: Double = 2.0,
    private val distancePenaltyPd: Double = 1.35, // Finomhangolva a kerekítési határokhoz (1.8, 2.4, 2.2)
    private val activationMargin: Double = 0.3,
    private val useOptimalFingerAssignment: Boolean = true
) {
    private val handFingers: List<Pair<Int, Finger>> = columnConfigs
        .map { it.anchorColIndex to it.finger }
        .distinct()

    private fun calculateEffortForFinger(
        targetCenter: Coordinate,
        anchorColIndex: Int,
        finger: Finger
    ): Double {
        val anchorCenter = keyboardHalf.getPhysicalCoordinate(keyboardHalf.homeRowIndex, anchorColIndex)

        val xSign = if (keyboardHalf.hand == Hand.LEFT) 1.0 else -1.0 //Előjel
        val homeX = anchorCenter.x + (finger.homeOffsetX * xSign)
        val homeY = anchorCenter.y - finger.homeOffsetY

        val dx = targetCenter.x - homeX
        val dy = targetCenter.y - homeY

        // 3. ELŐSZÖR: A mozgásvektor felbontása az alkar 10 fokos megközelítési szöge szerint
        val theta = Math.toRadians(angleOfApproachDegrees) * xSign
        val rotX = dx * cos(theta) + dy * sin(theta)
        val rotY = -dx * sin(theta) + dy * cos(theta)

        // 4. MÁSODSZOR: Távolság az aktivációs terület (0.3u margó) határáig a forgatott térben
        val distX = max(0.0, abs(rotX) - activationMargin)
        val distY = max(0.0, abs(rotY) - activationMargin)

        // 5. Fitts törvénye szerinti logaritmikus büntetés
        val fX = log2(1.0 + distancePenaltyPd * distX)
        val fY = log2(1.0 + distancePenaltyPd * distY)

        // 6. Eredő terhelés
        val pf = finger.baseEffort
        return pf + hypot(pf * fY, lateralPenaltyPx * fX)
    }

    fun calculateEffort(targetRow: Int, targetCol: Int): Double {
        val targetCenter = keyboardHalf.getPhysicalCoordinate(targetRow, targetCol)

        return if (useOptimalFingerAssignment) {
            // Colemak-DH "Standard Keyboard" mód: a legkisebb terhelést adó ujjat választja
            handFingers.minOf { (anchorCol, finger) ->
                calculateEffortForFinger(targetCenter, anchorCol, finger)
            }
        } else {
            // Colemak-DH "Traditional finger pattern" mód: az oszlophoz rendelt ujjat használja
            val config = columnConfigs.find { it.colIndex == targetCol }
                ?: throw IllegalArgumentException("Hiányzó konfiguráció a(z) $targetCol. oszlophoz!")
            calculateEffortForFinger(targetCenter, config.anchorColIndex, config.finger)
        }
    }

    fun generateEffortMatrix(): List<List<Double>> {
        return (0 until keyboardHalf.numRows).map { r ->
            (0 until keyboardHalf.numColumns).map { c ->
                calculateEffort(r, c)
            }
        }
    }
}