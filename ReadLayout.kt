import EffortModel.ColumnConfig
import EffortModel.CustomKeyboardEffortGenerator
import EffortModel.Finger
import EffortModel.Hand
import EffortModel.KeyboardHalf
import EffortModel.StaggerType
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import kotlin.math.max
import kotlin.math.min


data class HardwareHalfYaml(
    @JsonProperty("offsets") val offsets: List<Double>,
    @JsonProperty("fingers") val fingers: List<String>,
    @JsonProperty("anchors") val anchors: List<Int>
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

data class KeyboardLayoutYaml(
    @JsonProperty("name") val name: String = "Unknown",
    @JsonProperty("hardware") val hardware: HardwareYaml,
    @JsonProperty("matrix") val matrix: String
)



class LayoutEffortParser {
    private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    fun parseAndMapLayout(yamlFile: File): Map<Char, Double> {
        // 1. YAML beolvasása
        val layout = mapper.readValue(yamlFile, KeyboardLayoutYaml::class.java)
        val hw = layout.hardware

        // 2. Fizikai modell paramétereinek dekódolása
        val staggerType = if (hw.staggerType.lowercase() == "row_stagger") {
            StaggerType.ROW_STAGGER
        } else {
            StaggerType.COLUMNAR_STAGGER
        }

        // Segédfüggvény a betűkombinációk valódi anatómiai ujjakra való fordításához
        fun buildConfigs(half: HardwareHalfYaml): List<ColumnConfig> {
            return half.fingers.zip(half.anchors).mapIndexed { index, (fingerStr, anchor) ->
                val finger = when (fingerStr.uppercase()) {
                    "P" -> Finger.PINKY
                    "R" -> Finger.RING
                    "M" -> Finger.MIDDLE
                    "I" -> Finger.INDEX
                    else -> Finger.INDEX
                }
                ColumnConfig(index, anchor, finger)
            }
        }

        val leftConfigs = buildConfigs(hw.left)
        val rightConfigs = buildConfigs(hw.right)

        // 3. A fizikai billentyűzet felek létrehozása
        val leftHalf = KeyboardHalf(
            hand = Hand.LEFT,
            staggerType = staggerType,
            numRows = hw.numRows,
            numColumns = hw.left.offsets.size,
            offsets = hw.left.offsets,
            homeRowIndex = hw.homeRowIndex
        )

        val rightHalf = KeyboardHalf(
            hand = Hand.RIGHT,
            staggerType = staggerType,
            numRows = hw.numRows,
            numColumns = hw.right.offsets.size,
            offsets = hw.right.offsets,
            homeRowIndex = hw.homeRowIndex
        )

        // 4. Terhelési mátrix legenerálása az adatokból
        val leftEffortMatrix = CustomKeyboardEffortGenerator(
            keyboardHalf = leftHalf,
            columnConfigs = leftConfigs,
            angleOfApproachDegrees = hw.angleOfApproachDegrees,
            useOptimalFingerAssignment = hw.optimalFingerAssignment
        ).generateEffortMatrix()

        val rightEffortMatrix = CustomKeyboardEffortGenerator(
            keyboardHalf = rightHalf,
            columnConfigs = rightConfigs,
            angleOfApproachDegrees = hw.angleOfApproachDegrees,
            useOptimalFingerAssignment = hw.optimalFingerAssignment
        ).generateEffortMatrix()

        val characterEffortMap = mutableMapOf<Char, Double>()
        val lines = layout.matrix.trimIndent().lines().filter { it.isNotBlank() }

        // 5. A fő mátrix feldolgozása (dupla szóközös elválasztás)
        for (r in 0 until hw.numRows) {
            if (r >= lines.size) break
            val parts = lines[r].trim().split(Regex(" {2,}"))
            val leftStr = if (parts.isNotEmpty()) parts[0].replace(" ", "") else ""
            val rightStr = if (parts.size > 1) parts[1].replace(" ", "") else ""

            if (r < leftEffortMatrix.size) {
                for (c in 0 until min(leftStr.length, leftEffortMatrix[r].size)) {
                    val char = leftStr[c]
                    if (char != '_') characterEffortMap[char.uppercaseChar()] = leftEffortMatrix[r][c]
                }
            }

            if (r < rightEffortMatrix.size) {
                for (c in 0 until min(rightStr.length, rightEffortMatrix[r].size)) {
                    val char = rightStr[c]
                    if (char != '_') characterEffortMap[char.uppercaseChar()] = rightEffortMatrix[r][c]
                }
            }
        }

        // 6. Hüvelykujjak feldolgozása a mátrix alatti utolsó sorból
        if (lines.size > hw.numRows) {
            val parts = lines[hw.numRows].trim().split(Regex(" {2,}"))
            val leftThumbStr = if (parts.isNotEmpty()) parts[0].replace(" ", "") else ""
            val rightThumbStr = if (parts.size > 1) parts[1].replace(" ", "") else ""
            val thumbEffort = Finger.THUMB.baseEffort

            if (leftThumbStr.isNotEmpty() && leftThumbStr[0] != '_') {
                characterEffortMap[leftThumbStr[0].uppercaseChar()] = thumbEffort
            }
            if (rightThumbStr.isNotEmpty() && rightThumbStr[0] != '_') {
                characterEffortMap[rightThumbStr[0].uppercaseChar()] = thumbEffort
            }
        }

        return characterEffortMap
    }
}