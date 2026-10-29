package KeyboardLayoutGeneration


import jdk.jfr.Enabled
import keyboard_analysis.CombinedLayoutData
import keyboard_analysis.CorpusData
import keyboard_analysis.LayoutAnalysisResult
import keyboard_analysis.completeLayoutAnalysis
import kotlin.math.abs
import kotlin.math.exp
import kotlin.random.Random

/**
 * Kiszámítja a billentyűzetkiosztás összesített jósági/terhelési mutatóját (Célfüggvény).
 * Minél KISEBB a visszaadott érték, annál jobb a kiosztás.
 */
/**
 * Kiszámítja a billentyűzetkiosztás összesített jósági/terhelési mutatóját (Célfüggvény).
 * Minél KISEBB a visszaadott érték, annál jobb a kiosztás.
 */
fun calculateTotalEffort(result: LayoutAnalysisResult): Double {

    // --- 1. BÜNTETŐ SÚLYOK (Minimalizálandó értékek -> növelik a Total Effortot) ---
    val weightCharReachEffort = 40.0
    val weightHandImbalance = 0.5

    // Cél: < 4-6%
    val weightPinkyOff = 5.0

    // Cél: < 1-1.5%
    val weightSfb = 10.0

    // Cél: < 0.5% (ÚJ)
    val weightFullScissor = 15.0
    val weightHalfScissor = weightFullScissor/2

    // Cél: < 1.5-2% (ÚJ)
    val weightLateralStretch = 8.0

    // Cél: < 2-3%
    val weightRedirect = 4.0

    val weightSpaceSfs1 = 2.0

    val weightGravity = 0.5

    // Soronkénti terhelés súlyai (0: legfelső sor ... 5: thumb sor)
    val weightRow0 = 16.0
    val weightRow1 = 15.0
    val weightRow2 = 0.0
    val weightRow3 = 0.0 // Home row (ezt külön jutalmazzuk lent)
    val weightRow4 = 0.0
    val weightRow5 = 0.0 // Thumb row

    // --- 2. JUTALMAZÓ SÚLYOK (Maximalizálandó értékek -> csökkentik a Total Effortot) ---
    val weightHomeRow = 0.86

    // Cél: minél magasabb, együttesen > 40-50%
    val weightAlternation = 1.0
    val weightRoll = 1.3
    val weightThreeRoll = 1.6

    // Kéz-aszimmetria kiszámítása: |bal% - jobb%|
    val handImbalance = kotlin.math.abs(result.handBalanceLeft - result.handBalanceRight)

    // Sorok súlyozott összege (index-biztos lekéréssel, ha változna a sorok száma)
    val rowPenalty =
        (result.rowPercentages.getOrElse(0) { 0.0 } * weightRow0) +
                (result.rowPercentages.getOrElse(1) { 0.0 } * weightRow1) +
                (result.rowPercentages.getOrElse(2) { 0.0 } * weightRow2) +
                (result.rowPercentages.getOrElse(3) { 0.0 } * weightRow3) +
                (result.rowPercentages.getOrElse(4) { 0.0 } * weightRow4) +
                (result.rowPercentages.getOrElse(5) { 0.0 } * weightRow5)

    // Büntetések összege (beleértve a két új metrikát)
    val penalties =
        (result.charReachEffort * weightCharReachEffort) +
                (result.baseGravityEffort * weightGravity) +
                (handImbalance * weightHandImbalance) +
                (result.pinkyOffPercentage * weightPinkyOff) +
                (result.sfbPercentage * weightSfb) +
                (result.fullScissorPercentage * weightFullScissor) +
                (result.halfScissorPercentage * weightHalfScissor) +
                (result.lateralStretchPercentage * weightLateralStretch) +
                (result.redirectPercentage * weightRedirect) +
                (result.spaceSfs1Penalty * weightSpaceSfs1) +
                rowPenalty

    // Jutalmak összege
    val rewards =
        (result.homeRowPercentage * weightHomeRow) +
                (result.alternationPercentage * weightAlternation) +
                (result.rollPercentage * weightRoll) +
                (result.threerollPercentage * weightThreeRoll)

    return penalties - rewards
}

/**
 * Simulated Annealing alapú optimalizáló függvény.
 * Bemenetként kap egy kezdeti layoutot, és visszaadja a megtalált legjobb (legkisebb effortú) változatot.
 */
fun improveLayout(
    initialLayout: CombinedLayoutData,
    corpusData: CorpusData,
    iterations: Int = 10000,
    initialTemp: Double = 10.0,
    coolingRate: Double = 0.999
): CombinedLayoutData {
    // Dolgozzunk egy másolaton, hogy az eredeti objektum érintetlen maradjon, ha kell
    var currentLayout = initialLayout.deepCopy("${initialLayout.name}_current")
    var currentAnalysis = completeLayoutAnalysis(currentLayout, corpusData, verbose = false)
    var currentEnergy = currentAnalysis.totalEffort

    var bestLayout = currentLayout.deepCopy("${initialLayout.name}_optimized")
    var bestEnergy = currentEnergy

    var temp = initialTemp
    val mainRows = currentLayout.numRows - 1 // A fő mátrix sorai (a thumb sort figyelmen kívül hagyva)

    println("=== Optimalizálás indítása: ${initialLayout.name} ===")
    println("Kezdeti Total Effort: ${String.format("%.4f", currentEnergy)}")

    for (step in 1..iterations) {
        // 1. Készítünk egy munkamásolatot és végrehajtunk rajta egy véletlen cserét (mutációt)
        val candidateLayout = currentLayout.deepCopy()
        performMutation(candidateLayout,false, false,mainRows)

        // 2. Kiértékeljük az új állapotot (verbose = false, hogy ne írjon a konzolra és fájlba!)
        val candidateAnalysis = completeLayoutAnalysis(candidateLayout, corpusData, verbose = false)
        val candidateEnergy = candidateAnalysis.totalEffort

        // 3. Energiakülönbség (Delta E)
        val deltaE = candidateEnergy - currentEnergy

        // 4. Metropolis-kritérium: elfogadjuk-e az új állapotot?
        val accept = if (deltaE < 0.0) {
            true // Jobb állapot -> mindig elfogadjuk
        } else {
            val acceptanceProbability = exp(-deltaE / temp)
            Random.nextDouble() < acceptanceProbability // Rosszabb állapot -> valószínűségi elfogadás
        }

        if (accept) {
            currentLayout = candidateLayout
            currentEnergy = candidateEnergy

            // Ha ez az eddigi globálisan legjobb állapot, elmentjük
            if (currentEnergy < bestEnergy) {
                bestLayout = currentLayout.deepCopy("${initialLayout.name}_optimized")
                bestEnergy = currentEnergy
                println("[Lépés: $step | T: ${String.format("%.4f", temp)}] Új legjobb Effort: ${String.format("%.4f", bestEnergy)}")
            }
        }

        // 5. Hűtés
        temp *= coolingRate
    }

    println("=== Optimalizálás befejezve ===")
    println("Végső legjobb Total Effort: ${String.format("%.4f", bestEnergy)}")

    return bestLayout
}

/**
 * Segédfüggvény: Elvégzi valamely korábban definiált mutációt a billentyűzeten
 */
private fun performMutation(layout: CombinedLayoutData, leftThumbEnabled: Boolean=false, rightThumbEnabled:Boolean=false, mainRows:Int) {
   val mutation = Random.nextDouble(0.0, 1.0)
    when {
        mutation < 0.2 -> {
            while (true) {
                val left1 = Random.nextBoolean()
                // Ha a thumb ki van kapcsolva, csak 0 until mainRows között sorsolunk sort
                val maxRow1 = if (left1 && leftThumbEnabled || !left1 && rightThumbEnabled) layout.numRows else mainRows
                val r1 = Random.nextInt(maxRow1)
                val grid1 = if (left1) layout.leftChars else layout.rightChars
                val c1 = Random.nextInt(grid1[r1].size)

                val left2 = Random.nextBoolean()
                val maxRow2 = if (left2 && leftThumbEnabled || !left2 && rightThumbEnabled) layout.numRows else mainRows
                val r2 = Random.nextInt(maxRow2)
                val grid2 = if (left2) layout.leftChars else layout.rightChars
                val c2 = Random.nextInt(grid2[r2].size)

                if (left1 == left2 && r1 == r2 && c1 == c2) continue

                val ch1 = grid1[r1][c1]
                val ch2 = grid2[r2][c2]

                if (ch1 == '_' && ch2 == '_') continue
                layout.swapKeys(left1, r1, c1, left2, r2, c2)
                break
            }
        }
        mutation < 0.4 -> {
            val left1 = Random.nextBoolean()
            val maxCols1 = if (left1) layout.leftChars[0].size else layout.rightChars[0].size
            val col1 = Random.nextInt(maxCols1)

            val left2 = Random.nextBoolean()
            val maxCols2 = if (left2) layout.leftChars[0].size else layout.rightChars[0].size
            val col2 = Random.nextInt(maxCols2)

            layout.swapColumns(left1, col1, left2, col2)
        }
        mutation < 0.6 -> {
            val isLeft = Random.nextBoolean()
            if (mainRows >= 2) {
                val r1 = Random.nextInt(mainRows)
                var r2 = Random.nextInt(mainRows)
                while (r2 == r1) {
                    r2 = Random.nextInt(mainRows) // Biztosítjuk, hogy ne ugyanazt a sort cseréljük önmagával
                }
                layout.swapRowsLocal(isLeft, r1, r2)
            }
        }
        mutation < 0.8 -> {
            if (mainRows >= 2) {
                val r1 = Random.nextInt(mainRows)
                var r2 = Random.nextInt(mainRows)
                while (r2 == r1) {
                    r2 = Random.nextInt(mainRows)
                }
                layout.swapRowsGlobal(r1, r2)
            }
        }
        else -> {
            val isLeft = Random.nextBoolean()
            layout.mirrorHand(isLeft)
        }
    }
}