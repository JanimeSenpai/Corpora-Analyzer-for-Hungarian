import EffortModel.ColemakDhEffortGenerator
import EffortModel.ColumnConfig
import EffortModel.EffortHeatmapRenderer
import EffortModel.Finger
import EffortModel.Hand
import EffortModel.KeyboardHalf
import EffortModel.StaggerType

fun main() {
    // Standard ISO/ANSI sor-eltolások az alapsorhoz (2. index) képest:

    // Num (-0.5u), Top (-0.25u), Home (0.0u), Bottom (+0.5u)
    val standardRowOffsets = listOf(-0.5, -0.25, 0.0, 0.5)
    val leftHalf = KeyboardHalf(Hand.LEFT, StaggerType.ROW_STAGGER, 4, 6, standardRowOffsets)
    val leftConfigs = listOf(
        ColumnConfig(0, anchorColIndex = 1, Finger.PINKY),  // Külső kisujj oszlop (Tab / CapsLock sáv)
        ColumnConfig(1, anchorColIndex = 1, Finger.PINKY),  // Kisujj (Q, A, Z)
        ColumnConfig(2, anchorColIndex = 2, Finger.RING),   // Gyűrűs (W, S, X)
        ColumnConfig(3, anchorColIndex = 3, Finger.MIDDLE), // Középső (E, D, C)
        ColumnConfig(4, anchorColIndex = 4, Finger.INDEX),  // Mutató (R, F, V)
        ColumnConfig(5, anchorColIndex = 4, Finger.INDEX)   // Belső mutató (T, G, B)
    )

    val rightHalf = KeyboardHalf(Hand.RIGHT, StaggerType.ROW_STAGGER, 4, 6, standardRowOffsets)
    val rightConfigs = listOf(
        ColumnConfig(0, anchorColIndex = 1, Finger.INDEX),  // Belső mutató (Y, H, N)
        ColumnConfig(1, anchorColIndex = 1, Finger.INDEX),  // Mutató (U, J, M)
        ColumnConfig(2, anchorColIndex = 2, Finger.MIDDLE), // Középső (I, K, ,)
        ColumnConfig(3, anchorColIndex = 3, Finger.RING),   // Gyűrűs (O, L, .)
        ColumnConfig(4, anchorColIndex = 4, Finger.PINKY),  // Kisujj (P, ;, /)
        ColumnConfig(5, anchorColIndex = 4, Finger.PINKY)   // Külső kisujj oszlop ([ / ' / RShift sáv)
    )

    // 1. Kép: Hagyományos ujjrend (a PDF 8. oldalának alsó ábrája)
    val leftTrad = ColemakDhEffortGenerator(leftHalf, leftConfigs, useOptimalFingerAssignment = false).generateEffortMatrix()
    val rightTrad = ColemakDhEffortGenerator(rightHalf, rightConfigs, useOptimalFingerAssignment = false).generateEffortMatrix()
    EffortHeatmapRenderer.renderFullKeyboard(leftHalf, rightHalf, leftTrad, rightTrad, "standard_traditional_heatmap.png")

    // 2. Kép: Optimális ujjrend (a PDF 8. oldalának felső ábrája)
    val leftOpt = ColemakDhEffortGenerator(leftHalf, leftConfigs, useOptimalFingerAssignment = true).generateEffortMatrix()
    val rightOpt = ColemakDhEffortGenerator(rightHalf, rightConfigs, useOptimalFingerAssignment = true).generateEffortMatrix()
    EffortHeatmapRenderer.renderFullKeyboard(leftHalf, rightHalf, leftOpt, rightOpt, "standard_optimal_heatmap.png")

    println("Képek sikeresen legenerálva: standard_traditional_heatmap.png és standard_optimal_heatmap.png")
}