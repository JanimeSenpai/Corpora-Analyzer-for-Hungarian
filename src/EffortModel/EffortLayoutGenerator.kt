package EffortModel

import kotlin.math.abs
import kotlin.math.hypot

import kotlin.math.*

// Az ujjak anatómai tulajdonságai a Colemak-DH alapján
data class FingerAnatomy(
    val baseEffort: Double, // Pf
    val homeOffsetX: Double,
    val homeOffsetY: Double
)

val FINGER_PROFILES = mapOf(
    Finger.INDEX to FingerAnatomy(1.0, -0.04, -0.32),
    Finger.MIDDLE to FingerAnatomy(1.1, 0.03, 0.16),
    Finger.RING to FingerAnatomy(1.3, 0.00, 0.08),
    Finger.PINKY to FingerAnatomy(1.6, 0.01, 0.09)
)

class ColemakDhEffortGenerator(
    private val keyboardHalf: KeyboardHalf,
    private val columnConfigs: List<ColumnConfig>,
    private val angleOfApproachDegrees: Double = 10.0,
    private val lateralPenaltyPx: Double = 2.0,
    private val distancePenaltyPd: Double = 1.30,
    private val activationMargin: Double = 0.3,
    private val useOptimalFingerAssignment: Boolean = false
) {
    // Kigyűjtjük az adott kézen található 4 ujj egyedi (anchorColIndex, Finger) párjait
    private val handFingers: List<Pair<Int, Finger>> = columnConfigs
        .map { it.anchorColIndex to it.finger }
        .distinct()

    /**
     * Kiszámolja egy adott cél-koordináta leütésének terhelését egy konkrét ujjra.
     */
    private fun calculateEffortForFinger(
        targetCenter: Coordinate,
        anchorColIndex: Int,
        finger: Finger
    ): Double {
        val anchorCenter = keyboardHalf.getPhysicalCoordinate(keyboardHalf.homeRowIndex, anchorColIndex)

        // 1. Anatómiai pihenőhelyzet (Home Position) kiszámítása
        // Bal kéznél +homeOffsetX, jobb kéznél a tükrözés miatt -homeOffsetX.
        // Mivel a mátrixban +Y lefelé mutat, a felfelé értelmezett homeOffsetY-t kivonjuk.
        val xSign = if (keyboardHalf.hand == Hand.LEFT) 1.0 else -1.0
        val homeX = anchorCenter.x + (finger.homeOffsetX * xSign)
        val homeY = anchorCenter.y - finger.homeOffsetY

        // 2. Elmozdulásvektor a pihenőponttól a célgomb középpontjáig
        val dx = targetCenter.x - homeX
        val dy = targetCenter.y - homeY

        // 3. Forgatási transzformáció az alkar megközelítési szöge alapján
        val theta = Math.toRadians(angleOfApproachDegrees) * xSign
        val dLateral = dx * cos(theta) + dy * sin(theta)
        val dMesial = -dx * sin(theta) + dy * cos(theta)

        // 4. Távolság az aktivációs terület (0.3u margójú doboz) határáig
        val distX = max(0.0, abs(dLateral) - activationMargin)
        val distY = max(0.0, abs(dMesial) - activationMargin)

        // 5. Fitts törvénye szerinti logaritmikus büntetés
        val fX = log2(1.0 + distancePenaltyPd * distX)
        val fY = log2(1.0 + distancePenaltyPd * distY)

        // 6. Eredő terhelés: Pf + sqrt((Pf * Fy)^2 + (Px * Fx)^2)
        val pf = finger.baseEffort
        val mesialPenalty = pf * fY
        val lateralPenalty = lateralPenaltyPx * fX

        return pf + hypot(mesialPenalty, lateralPenalty)
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