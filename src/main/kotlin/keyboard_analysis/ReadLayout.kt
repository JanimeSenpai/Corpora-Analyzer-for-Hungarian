package keyboard_analysis

import EffortModel.*
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

data class KeyboardConfigYaml(
    @JsonProperty("name") val name: String = "Unknown",
    @JsonProperty("hardware") val hardware: HardwareYaml,
    @JsonProperty("matrix") val matrix: String,
    @JsonProperty("thumb_left") val thumbLeft: String = "_",
    @JsonProperty("thumb_right") val thumbRight: String = "_"
)

data class HardwareYaml(
    @JsonProperty("stagger_type") val staggerType: String,
    @JsonProperty("num_rows") val numRows: Int,
    @JsonProperty("home_row_index") val homeRowIndex: Int,
    @JsonProperty("optimal_finger_assignment") val optimalFingerAssignment: Boolean = true,
    @JsonProperty("angle_of_approach_degrees") val angleOfApproachDegrees: Double = 10.0,
    @JsonProperty("left") val left: HardwareHalfYaml,
    @JsonProperty("right") val right: HardwareHalfYaml
)

data class HardwareHalfYaml(
    @JsonProperty("offsets") val offsets: List<Double>,
    @JsonProperty("fingers") val fingers: List<String>,
    @JsonProperty("anchors") val anchors: List<Int>
)

class CombinedLayoutData(
    var name: String,
    val hardwareYaml: HardwareYaml,
    val leftEffortMatrix: Array<DoubleArray>,
    val rightEffortMatrix: Array<DoubleArray>,
    val leftChars: Array<CharArray>,
    val rightChars: Array<CharArray>
) {
    val numRows: Int get() = hardwareYaml.numRows
    val homeRowIndex: Int get() = hardwareYaml.homeRowIndex

    // A fizikai oszlop indexének megkeresése a hüvelykujjhoz
    private val leftThumbPhysicalCol = hardwareYaml.left.fingers.indexOfFirst { it.equals("T", true) }.takeIf { it >= 0 } ?: (hardwareYaml.left.fingers.size - 1)
    private val rightThumbPhysicalCol = hardwareYaml.right.fingers.indexOfFirst { it.equals("T", true) }.takeIf { it >= 0 } ?: 0

    // Gyors csere művelet a Simulated Annealing számára
    fun swapKeys(left1: Boolean, r1: Int, c1: Int, left2: Boolean, r2: Int, c2: Int):Boolean {
        val grid1 = if (left1) leftChars else rightChars
        val grid2 = if (left2) leftChars else rightChars
        val temp = grid1[r1][c1]
        grid1[r1][c1] = grid2[r2][c2]
        grid2[r2][c2] = temp
        return true
    }

    // 1. Oszlopcsere (üres cellákkal együtt, Thumb key-ek kihagyásával)
    fun swapColumns(left1: Boolean, col1: Int, left2: Boolean, col2: Int): Boolean {
        val grid1 = if (left1) leftChars else rightChars
        val grid2 = if (left2) leftChars else rightChars
        val thumbRow = numRows - 1

        for (r in 0 until thumbRow) {
            val temp = grid1[r][col1]
            grid1[r][col1] = grid2[r][col2]
            grid2[r][col2] = temp
        }
        return true
    }

    // 2. Helyi Sorcsere (üres cellákkal együtt, Thumb sor kizárva)
    fun swapRowsLocal(isLeft: Boolean, row1: Int, row2: Int): Boolean {
        val thumbRow = numRows - 1
        if (row1 == thumbRow || row2 == thumbRow) return false

        val grid = if (isLeft) leftChars else rightChars

        for (c in grid[row1].indices) {
            val temp = grid[row1][c]
            grid[row1][c] = grid[row2][c]
            grid[row2][c] = temp
        }
        return true
    }

    // 3. Globális Sorcsere (üres cellákkal együtt, Thumb sor kizárva)
    fun swapRowsGlobal(row1: Int, row2: Int): Boolean {
        val thumbRow = numRows - 1
        if (row1 == thumbRow || row2 == thumbRow) return false

        swapRowsLocal(true, row1, row2)
        swapRowsLocal(false, row1, row2)
        return true
    }

    // 4. Kézen belüli tükrözés (üres cellákkal együtt, Thumb sor kihagyásával)
    fun mirrorHand(isLeft: Boolean) {
        val grid = if (isLeft) leftChars else rightChars
        val thumbRow = numRows - 1

        for (r in 0 until thumbRow) {
            grid[r].reverse()
        }
    }

    // Mélymásolat készítése, ha az SA el akarja menteni az eddigi legjobb állapotot
    fun deepCopy(newName: String = this.name): CombinedLayoutData {
        return CombinedLayoutData(
            name = newName,
            hardwareYaml = hardwareYaml,
            leftEffortMatrix = leftEffortMatrix, // A fizikai effort mátrix konstans, nem kell klónozni
            rightEffortMatrix = rightEffortMatrix,
            leftChars = Array(leftChars.size) { r -> leftChars[r].clone() },
            rightChars = Array(rightChars.size) { r -> rightChars[r].clone() }
        )
    }

    // Karakter -> Effort hozzárendelés az aktuális betűpozíciók alapján
    fun buildCharEffortMap(): Map<Char, Double> {
        val map = HashMap<Char, Double>(64)
        for (r in 0 until numRows) {
            for (c in leftChars[r].indices) {
                val ch = leftChars[r][c]
                if (ch != '_') {
                    val physCol = if (r == numRows - 1) leftThumbPhysicalCol else c
                    map[ch] = leftEffortMatrix[r][physCol]
                }
            }
            for (c in rightChars[r].indices) {
                val ch = rightChars[r][c]
                if (ch != '_') {
                    val physCol = if (r == numRows - 1) rightThumbPhysicalCol else c
                    map[ch] = rightEffortMatrix[r][physCol]
                }
            }
        }
        return map
    }

    // Sorok kinyerése a row analysis számára (bal és jobb oldal egyesítve soronként)
    fun getRows(): List<CharArray> {
        return List(numRows) { r ->
            (leftChars[r] + rightChars[r]).filter { it != '_' }.toCharArray()
        }
    }

    // Ujjankénti karakterhalmazok (pl. "L_P", "R_I") az SFB és pinky elemzésekhez
    fun getFingerCharacterSets(): Map<String, Set<Char>> {
        val fingerMap = HashMap<String, MutableSet<Char>>()

        for (r in 0 until numRows) { //az r változó segítségével megyünk lefele végig az egész oszlopon
            for (c in leftChars[r].indices) { //minden egyes oszlopra bal kézen
                val ch = leftChars[r][c] //vesszük az adott koordinátán levő karaktert
                if (ch != '_') {
                    // Anatómiai ujj direkthezrendelése az SFB miatt
                    val fingerKey = if (r == numRows - 1) "L_T" else "L_${hardwareYaml.left.fingers[c]}"
                    val set = fingerMap.getOrPut(fingerKey) { HashSet() }
                    set.add(ch)
                }
            }
            for (c in rightChars[r].indices) {
                val ch = rightChars[r][c]
                if (ch != '_') {
                    // Anatómiai ujj direkthezrendelése az SFB miatt
                    val fingerKey = if (r == numRows - 1) "R_T" else "R_${hardwareYaml.right.fingers[c]}"
                    val set = fingerMap.getOrPut(fingerKey) { HashSet() }
                    set.add(ch)
                }
            }
        }
        return fingerMap
    }
}

object LayoutParser {
    private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    fun parseLayout(yamlFile: File): CombinedLayoutData {
        val config = mapper.readValue(yamlFile, KeyboardConfigYaml::class.java)
        val hw = config.hardware

        val stagger = when (hw.staggerType.lowercase()) {
            "columnar_stagger" -> StaggerType.COLUMNAR_STAGGER
            "row_stagger" -> StaggerType.ROW_STAGGER
            else -> StaggerType.MATRIX
        }

        // --- JAVÍTOTT OSZLOPSZÁM MEGHATÁROZÁS ---
        // Az oszlopok számát mindig az ujjak listájának hossza adja, nem az offsets mérete!
        val leftCols = hw.left.fingers.size
        val rightCols = hw.right.fingers.size

        val leftHalf = KeyboardHalf(Hand.LEFT, stagger, hw.numRows, leftCols, hw.left.offsets, hw.homeRowIndex)
        val rightHalf = KeyboardHalf(Hand.RIGHT, stagger, hw.numRows, rightCols, hw.right.offsets, hw.homeRowIndex)

        val leftConfigs = hw.left.fingers.indices.map { i ->
            ColumnConfig(
                colIndex = i,
                anchorColIndex = hw.left.anchors[i],
                finger = parseFinger(hw.left.fingers[i])
            )
        }
        val rightConfigs = hw.right.fingers.indices.map { i ->
            ColumnConfig(
                colIndex = i,
                anchorColIndex = hw.right.anchors[i],
                finger = parseFinger(hw.right.fingers[i])
            )
        }

        // Javítva: List<List<Double>> konvertálása Array<DoubleArray>-ré
        val leftEffort = CustomKeyboardEffortGenerator(
            keyboardHalf = leftHalf,
            columnConfigs = leftConfigs,
            angleOfApproachDegrees = hw.angleOfApproachDegrees,
            useOptimalFingerAssignment = hw.optimalFingerAssignment
        ).generateEffortMatrix().map { it.toDoubleArray() }.toTypedArray()

        val rightEffort = CustomKeyboardEffortGenerator(
            keyboardHalf = rightHalf,
            columnConfigs = rightConfigs,
            angleOfApproachDegrees = hw.angleOfApproachDegrees,
            useOptimalFingerAssignment = hw.optimalFingerAssignment
        ).generateEffortMatrix().map { it.toDoubleArray() }.toTypedArray()


        val leftChars = Array(hw.numRows) { CharArray(leftCols) { '_' } }
        val rightChars = Array(hw.numRows) { CharArray(rightCols) { '_' } }

        // --- ÚJ, JAVÍTOTT BEOLVASÁS ---
        val matrixLines = config.matrix.trim().lines().filter { it.isNotBlank() }
        val mainRows = hw.numRows - 1

        for (r in 0 until minOf(mainRows, matrixLines.size)) {
            // Eltávolítjuk az összes szóközt a sorból (pl. "___ _ _ ___   ___ _ w xq_" -> "________wxq_")
            val cleanLine = matrixLines[r].replace(Regex("\\s+"), "")

            // A bal kéz levágja az elejéről a saját karaktereit (pontosan annyit, ahány oszlopa van)
            val leftString = cleanLine.take(leftCols)

            // A jobb kéz megkapja a maradékot
            val rightString = cleanLine.drop(leftCols).take(rightCols)

            for (c in 0 until minOf(leftCols, leftString.length)) leftChars[r][c] = leftString[c]
            for (c in 0 until minOf(rightCols, rightString.length)) rightChars[r][c] = rightString[c]
        }

        // Thumb sor beolvasása közvetlenül a stringekből (1 karakteres méretű tömb)
        val thumbRowIdx = hw.numRows - 1
        leftChars[thumbRowIdx] = charArrayOf(config.thumbLeft.firstOrNull() ?: '_')
        rightChars[thumbRowIdx] = charArrayOf(config.thumbRight.firstOrNull() ?: '_')

        return CombinedLayoutData(config.name, hw, leftEffort, rightEffort, leftChars, rightChars)
    }

    private fun parseFinger(code: String): Finger = when (code.uppercase()) {
        "P" -> Finger.PINKY
        "R" -> Finger.RING
        "M" -> Finger.MIDDLE
        "I" -> Finger.INDEX
        "T" -> Finger.THUMB
        else -> throw IllegalArgumentException("Ismeretlen ujj kód: $code")
    }
}