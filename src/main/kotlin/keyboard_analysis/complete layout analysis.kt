package keyboard_analysis

import CombinedLayoutData
import LayoutParser
import java.io.File

data class LayoutAnalysisResult(
    val layoutName: String,
    val totalEffort: Double,
    val charReachEffort: Double,//based on effort layout, keyboard layout and letter frequency in corpus
    val rowPercentages: DoubleArray,
    val homeRowPercentage: Double,
    val handBalanceLeft: Double,
    val handBalanceRight: Double,
    val pinkyOffPercentage: Double,
    val sfbPercentage: Double,
    val alternationPercentage: Double,
    val rollPercentage: Double, // 2-1 és 1-2 együtt
    val redirectPercentage: Double,
    val spaceSfs1Penalty: Double
)

// 1. KONZOLOS INDÍTÓ FÜGGVÉNY (Egyszeri futás)
fun launchCompleteLayoutAnalysis() {
    println("--- TELJES BILLENTYŰZET ELEMZÉS ---")

    // 1. Billentyűzet bekérése (Már az új YAML rendszert használjuk)
    println("Kérem az elemzendő billentyűzetkiosztás nevét (a keyboards mappából):")
    var layoutName = readln().trim()
    if (!layoutName.endsWith(".yaml")) layoutName += ".yaml"

    val layoutFile = File("keyboards/$layoutName")
    if (!layoutFile.exists()) {
        println("Hiba: A billentyűzet fájl nem található ($layoutFile).")
        return
    }

    // 2. Adathalmaz bekérése
    println("Kérem az adathalmaz nevét (pontosan úgy, ahogy a mappa neve szerepel, pl. 'szoveg.txt'):")
    var datasetName = readln().trim()

    // 3. Adatok beolvasása a memóriába
    val layoutData = try {
        LayoutParser.parseLayout(layoutFile)
    } catch (e: Exception) {
        println("Hiba a billentyűzet beolvasásakor: ${e.message}")
        return
    }

    val corpusData = CorpusData.loadFromDataset(datasetName)

    if (corpusData.totalLetters == 0L) {
        println("Hiba: A korpusz adatok nem töltődtek be megfelelően.")
        return
    }

    println("\n==================================================")
    println("ELEMZÉS INDÍTÁSA: ${layoutData.name} (Adathalmaz: $datasetName)")
    println("==================================================")

    // 4. Elemző motor meghívása beszédes (verbose) módban
    val result = completeLayoutAnalysis(layoutData, corpusData, verbose = true)

    println("==================================================")
    println("MINDEN ELEMZÉS SIKERESEN BEFEJEZŐDÖTT!")
    println("Összesített Effort: ${String.format("%.4f", result.totalEffort)}")
    println("==================================================")
}

// 2. MAGFÜGGVÉNY / MOTOR (A Simulated Annealing és a launch hívja)
fun completeLayoutAnalysis(
    layoutData: CombinedLayoutData,
    corpusData: CorpusData,
    verbose: Boolean,
): LayoutAnalysisResult {

    if (verbose) {
        println("--------------------------------------------------")
        println("1. Effort (Fizikai Terhelés) számítása...")
    }
    val charEffortMap = layoutData.buildCharEffortMap()
    val charReachEffort = calculateLayoutEffort(charEffortMap, corpusData, verbose)

    if (verbose){
        //hogyha egy darab layoutot elemzünk, akkor ki fogjuk írni a végeredményt fájlba, és először töröljük a fájlban levő adatokat, hogy appendeljük az új információt
        clearRawMetrics(corpusData.datasetName, layoutData.name)
    }


    if (verbose) println("--------------------------------------------------")
    val rows = layoutData.getRows()
    val rowPercentages = rowUsageAnalysis(rows, layoutData.homeRowIndex, corpusData.letters, verbose, layoutData.name,corpusData.datasetName)
    val homeRowPercentage = if (layoutData.homeRowIndex in rowPercentages.indices) rowPercentages[layoutData.homeRowIndex] else 0.0


    if (verbose) println("--------------------------------------------------")
    val (handBalanceLeft, handBalanceRight) = handBalanceAnalysis(layoutData, corpusData, verbose)

    if (verbose) println("--------------------------------------------------")
    val pinkyOffPercentage = pinkyOffAnalysis(layoutData, corpusData, verbose)

    if (verbose) println("--------------------------------------------------")
    val sfbPercentage = simpleSfbAnalysis(layoutData, corpusData, verbose)

    if (verbose) println("--------------------------------------------------")
    val alternationPercentage = alternationAnalysis(layoutData, corpusData, verbose)

    if (verbose) println("--------------------------------------------------")
    val rollPercentage = rollAnalysis(layoutData, corpusData, verbose)

    // A 3-roll elemzést is lefuttathatjuk, de egyelőre nem rakjuk be a kötelező eredmény objektumba
    if (verbose) println("--------------------------------------------------")
    threeRollAnalysis(layoutData, corpusData, verbose)

    if (verbose) println("--------------------------------------------------")
    val redirectPercentage = redirectAnalysis(layoutData, corpusData, verbose)

    if (verbose) println("--------------------------------------------------")
    val spaceSfs1Penalty = spaceSfs1Analysis(layoutData, corpusData, verbose)

    val totalEffort = calculateTotalEffort()


    return LayoutAnalysisResult(
        layoutName = layoutData.name,
        totalEffort = totalEffort,
        charReachEffort = charReachEffort,
        rowPercentages = rowPercentages,
        homeRowPercentage = homeRowPercentage,
        handBalanceLeft = handBalanceLeft,
        handBalanceRight = handBalanceRight,
        pinkyOffPercentage = pinkyOffPercentage,
        sfbPercentage = sfbPercentage,
        alternationPercentage = alternationPercentage,
        rollPercentage = rollPercentage,
        redirectPercentage = redirectPercentage,
        spaceSfs1Penalty = spaceSfs1Penalty
    )
}

fun calculateTotalEffort():Double {
    //az lesz a nagyon érdekes, hogy milyen arányban és súlyokkal vesszük figyelembe az összes eddigi paramétert, hogy aztán egy darab végső szám formájában jellemezzük az egész billentyűzetet betűelrendezéssel, fizikai kialakítással mindennel együtt
    TODO("Not yet implemented")
}
