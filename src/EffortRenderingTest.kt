import keyboard_analysis.ColumnConfig
import keyboard_analysis.EffortHeatmapRenderer
import keyboard_analysis.EffortLayoutGenerator
import keyboard_analysis.Hand
import keyboard_analysis.KeyboardHalf
import keyboard_analysis.StaggerType

fun main() {
    // Standard Row-Stagger fizikai eltolások (a Home sorhoz, vagyis a 2. indexhez képest)
    // Num sor (-0.75), Top sor (-0.25), Home sor (0.0), Bottom sor (+0.5)
    val standardRowOffsets = listOf(-0.75, -0.25, 0.0, 0.5)

    // 1. BAL KÉZ beállítása
    val leftHalf = KeyboardHalf(
        hand = Hand.LEFT,
        staggerType = StaggerType.ROW_STAGGER,
        numRows = 4,
        numColumns = 5, // Kisujj, Gyűrűs, Középső, Mutató, Belső-Mutató
        offsets = standardRowOffsets
    )

    val leftConfigs = listOf(
        ColumnConfig(colIndex = 0, anchorColIndex = 0, baseFingerEffort = 2.0), // Kisujj
        ColumnConfig(colIndex = 1, anchorColIndex = 1, baseFingerEffort = 1.5), // Gyűrűs
        ColumnConfig(colIndex = 2, anchorColIndex = 2, baseFingerEffort = 1.0), // Középső
        ColumnConfig(colIndex = 3, anchorColIndex = 3, baseFingerEffort = 0.0), // Mutató
        ColumnConfig(colIndex = 4, anchorColIndex = 3, baseFingerEffort = 0.0)  // Belső-mutató (A mutatóujj horgonyáról nyúl át!)
    )

    // 2. JOBB KÉZ beállítása
    val rightHalf = KeyboardHalf(
        hand = Hand.RIGHT,
        staggerType = StaggerType.ROW_STAGGER,
        numRows = 4,
        numColumns = 5, // Belső-Mutató, Mutató, Középső, Gyűrűs, Kisujj
        offsets = standardRowOffsets
    )

    // A jobb kéznél a sorrend fordított, mert balról jobbra haladunk a mátrixban!
    val rightConfigs = listOf(
        ColumnConfig(colIndex = 0, anchorColIndex = 1, baseFingerEffort = 0.0), // Belső-mutató (A mutatóujj horgonyáról nyúl át!)
        ColumnConfig(colIndex = 1, anchorColIndex = 1, baseFingerEffort = 0.0), // Mutató
        ColumnConfig(colIndex = 2, anchorColIndex = 2, baseFingerEffort = 1.0), // Középső
        ColumnConfig(colIndex = 3, anchorColIndex = 3, baseFingerEffort = 1.5), // Gyűrűs
        ColumnConfig(colIndex = 4, anchorColIndex = 4, baseFingerEffort = 2.0)  // Kisujj
    )

    // 3. Generátorok példányosítása a módosított matematikai modellel
    val leftGenerator = EffortLayoutGenerator(leftHalf, leftConfigs)
    val rightGenerator = EffortLayoutGenerator(rightHalf, rightConfigs)

    val leftEffortMatrix = leftGenerator.generateEffortMatrix()
    val rightEffortMatrix = rightGenerator.generateEffortMatrix()

    // 4. Kirajzolás
    EffortHeatmapRenderer.renderFullKeyboard(
        leftHalf = leftHalf,
        rightHalf = rightHalf,
        leftEffort = leftEffortMatrix,
        rightEffort = rightEffortMatrix
    )

    println("A standard aszimmetrikus billentyűzet hőtérképe legenerálva: full_keyboard_heatmap.png")
}