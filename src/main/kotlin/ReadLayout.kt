import EffortModel.*
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

data class KeyboardConfigYaml(
    @JsonProperty("name") val name: String = "Unknown",
    @JsonProperty("hardware") val hardware: HardwareYaml,
    @JsonProperty("matrix") val matrix: String
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

    // Gyors csere művelet a Simulated Annealing számára
    fun swapKeys(left1: Boolean, r1: Int, c1: Int, left2: Boolean, r2: Int, c2: Int) {
        val grid1 = if (left1) leftChars else rightChars
        val grid2 = if (left2) leftChars else rightChars
        val temp = grid1[r1][c1]
        grid1[r1][c1] = grid2[r2][c2]
        grid2[r2][c2] = temp
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
                if (ch != '_') map[ch] = leftEffortMatrix[r][c]
            }
            for (c in rightChars[r].indices) {
                val ch = rightChars[r][c]
                if (ch != '_') map[ch] = rightEffortMatrix[r][c]
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

        for (c in hardwareYaml.left.fingers.indices) {  //minden egyes oszlopra bal kézen
            val fingerKey = "L_${hardwareYaml.left.fingers[c]}" //legyen a fingerkey L_ujj
            val set = fingerMap.getOrPut(fingerKey) { HashSet() }  //ha van már L_ujj a fingermapben, akkor az lesz a set változó, ha nincs, létrehozzuk
            for (r in 0 until numRows) { //az r változó segítségével megyünk lefele végig az egész oszlopon
                val ch = leftChars[r][c]  //vesszük az adott koordinátán levő karaktert
                if (ch != '_') set.add(ch) //belerakjuk az ujjhoz tartozó halmazba
            }
        }
        for (c in hardwareYaml.right.fingers.indices) {
            val fingerKey = "R_${hardwareYaml.right.fingers[c]}"
            val set = fingerMap.getOrPut(fingerKey) { HashSet() }
            for (r in 0 until numRows) {
                val ch = rightChars[r][c]
                if (ch != '_') set.add(ch)
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

        // Javítva: A KeyboardHalf most 6 paramétert kap, megfelelő sorrendben.
        // A hw.left.offsets.size a numColumns.
        val leftHalf = KeyboardHalf(Hand.LEFT, stagger, hw.numRows, hw.left.offsets.size, hw.left.offsets, hw.homeRowIndex)
        val rightHalf = KeyboardHalf(Hand.RIGHT, stagger, hw.numRows, hw.right.offsets.size, hw.right.offsets, hw.homeRowIndex)

        // Javítva: A ColumnConfig most a megfelelő 3 paramétert kapja: colIndex, anchorColIndex, finger.
        // A hw.left.fingers mérete adja az oszlopok számát.  Végigmegyünk az indexeken, hogy
        // a colIndex meglegyen.
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

        val leftCols = hw.left.offsets.size
        val rightCols = hw.right.offsets.size
        val leftChars = Array(hw.numRows) { CharArray(leftCols) { '_' } }
        val rightChars = Array(hw.numRows) { CharArray(rightCols) { '_' } }

        // Javítva: String felbontása sorokra a beolvasáshoz
        val matrixLines = config.matrix.trim().lines().filter { it.isNotBlank() }

        for (r in 0 until minOf(hw.numRows, matrixLines.size)) {
            val tokens = matrixLines[r].trim().split(Regex("\\s+"))

            // A bal kéz pontosan annyi karaktert vesz ki, ahány fizikai oszlopa van
            val leftString = tokens.take(leftCols).joinToString("")

            // A jobb kéz a bal oldaliak levágása után kapja meg a saját adagját
            val rightString = tokens.drop(leftCols).take(rightCols).joinToString("")

            for (c in 0 until minOf(leftCols, leftString.length)) leftChars[r][c] = leftString[c]
            for (c in 0 until minOf(rightCols, rightString.length)) rightChars[r][c] = rightString[c]
        }

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