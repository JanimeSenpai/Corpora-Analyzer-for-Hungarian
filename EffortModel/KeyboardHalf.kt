package EffortModel
data class Coordinate(val x: Double, val y: Double)

enum class Hand {
    LEFT,
    RIGHT
}

enum class StaggerType {
    MATRIX,
    ROW_STAGGER,
    COLUMNAR_STAGGER
}

/**
 * Az ujjak anatómiai paraméterei a Colemak-DH dokumentáció alapján:
 * - baseEffort (Pf): Az ujj alap terhelési szorzója
 * - homeOffsetX: Vízszintes eltolás az alapsori gomb középpontjától (bal kézre értelmezve)
 * - homeOffsetY: Függőleges eltolás az alapsori gomb középpontjától (+ felfelé, - lefelé)
 */
enum class Finger(
    val baseEffort: Double,
    val homeOffsetX: Double,
    val homeOffsetY: Double
) {
    INDEX(baseEffort = 1.0, homeOffsetX = -0.04, homeOffsetY = -0.32),
    MIDDLE(baseEffort = 1.1, homeOffsetX = 0.03, homeOffsetY = 0.16),
    RING(baseEffort = 1.3, homeOffsetX = 0.00, homeOffsetY = 0.08),
    PINKY(baseEffort = 1.6, homeOffsetX = 0.01, homeOffsetY = 0.09)
}

data class ColumnConfig(
    val colIndex: Int,
    val anchorColIndex: Int,
    val finger: Finger
)

class KeyboardHalf(
    val hand: Hand,
    val staggerType: StaggerType,
    val numRows: Int,
    val numColumns: Int,
    val offsets: List<Double>,
    val homeRowIndex: Int = numRows - 2
) {
    fun getPhysicalCoordinate(rowIndex: Int, colIndex: Int): Coordinate {
        var x = colIndex.toDouble()
        var y = rowIndex.toDouble()

        when (staggerType) {
            StaggerType.MATRIX -> {}
            StaggerType.ROW_STAGGER -> {
                x += offsets.getOrElse(rowIndex) { 0.0 }
            }
            StaggerType.COLUMNAR_STAGGER -> {
                y += offsets.getOrElse(colIndex) { 0.0 }
            }
        }
        return Coordinate(x, y)
    }
}