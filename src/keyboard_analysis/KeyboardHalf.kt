package keyboard_analysis
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

class KeyboardHalf(
    val hand: Hand,
    val staggerType: StaggerType,
    val numRows: Int,
    val numColumns: Int,
    val offsets: List<Double>,
    // Alapértelmezett érték: mivel alulról mindig a második az alapsor,
    // az indexe (0-tól indulva) pontosan numRows - 2 lesz.
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