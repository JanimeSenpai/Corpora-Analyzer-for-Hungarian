package KeyboardLayoutGeneration

import keyboard_analysis.CombinedLayoutData
import keyboard_analysis.CorpusData
import keyboard_analysis.LayoutParser
import keyboard_analysis.completeLayoutAnalysis
import keyboard_analysis.printLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import layoutcharefforts
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main(){
    parallelimprovementTest(75)
}

fun improvementTest (){
    var layout = LayoutParser.parseLayout(File("keyboards/ltsnergomini.yaml"))
    var corpusData = CorpusData.loadFromDataset("huszonkettoHuszonnegyeswikipediahuszonegy.txt")

  var bestlayout=  improveLayout(layout, corpusData)
    printLayout(bestlayout)
    completeLayoutAnalysis(bestlayout, corpusData, verbose = true)
    layoutcharefforts(bestlayout)
    renderCharEffortlayout(bestlayout)

}

fun longerimprovementTest(runs:Int=15) {
    val initialLayout = try {
        LayoutParser.parseLayout(File("keyboards/ltsnergomini.yaml"))
    } catch (e: Exception) {
        println("Hiba a beolvasáskor: ${e.message}")
        return
    }

    val corpusData = CorpusData.loadFromDataset("huszonkettoHuszonnegyeswikipediahuszonegy.txt")

    val results = mutableListOf<Pair<Double, CombinedLayoutData>>()

    println("=== SA OPTIMALIZÁLÁS INDÍTÁSA ($runs FUTÁS) ===")
    val startTime = System.currentTimeMillis()

    // 1. Lefuttatjuk az optimalizálást 15 alkalommal
    for (i in 1..runs) {
        println("\n--- $i. optimalizációs futás indítása ---")

        // Maga a Simulated Annealing motor
        val bestLayoutInRun = improveLayout(initialLayout, corpusData)

        // Még egyszer gyorsan kiértékeljük csendben, hogy biztosan megkapjuk a végleges Total Effort értéket
        val finalAnalysis = completeLayoutAnalysis(bestLayoutInRun, corpusData, verbose = false)

        results.add(Pair(finalAnalysis.totalEffort, bestLayoutInRun))
        println("--- $i. futás kész. Effort: ${String.format("%.4f", finalAnalysis.totalEffort)} ---")
    }

    val totalTimeSeconds = (System.currentTimeMillis() - startTime) / 1000.0
    println("\nMind a $runs futás befejeződött. Teljes idő: ${String.format("%.1f", totalTimeSeconds)} mp")

    // 2. Rendezzük az eredményeket a legkisebb (legjobb) Effort szerint növekvő sorrendbe
    results.sortBy { it.first }

    // 3. Kiválasztjuk a legjobb 3-at
    val top3 = results.take(3)

    println("\n==================================================")
    println("=== A LEGJOBB 3 KIOSZTÁS KIÉRTÉKELÉSE ÉS MENTÉSE ===")
    println("==================================================")

    for ((index, pair) in top3.withIndex()) {
        val rank = index + 1
        val (effort, layout) = pair

        // Dinamikusan átnevezzük a layoutot. Így a completeLayoutAnalysis
        // automatikusan ebbe a nevű mappába generálja a fájlokat!
        layout.name = "${initialLayout.name}_optimized_$rank"

        println("\n>>> $rank. HELYEZETT (${layout.name}) - Total Effort: ${String.format("%.4f", effort)} <<<")

        printLayout(layout)

        // Verbose = true: itt készülnek el az analysis_results mappába a txt fájlok
        completeLayoutAnalysis(layout, corpusData, verbose = true)

        layoutcharefforts(layout)
        renderCharEffortlayout(layout,)
    }
}

// A runBlocking összeköti a hagyományos szinkron kódot a korutinok világával
fun parallelimprovementTest(runs: Int = 25, keyboardname: String = "ltsnergomini") = runBlocking {
    val originalYamlFile = File("keyboards/${keyboardname}.yaml")
    val initialLayout = try {
        LayoutParser.parseLayout(originalYamlFile)
    } catch (e: Exception) {
        println("Hiba a beolvasáskor: ${e.message}")
        return@runBlocking
    }

    val corpusData = CorpusData.loadFromDataset("huszonkettoHuszonnegyeswikipediahuszonegy.txt")

    println("=== SA OPTIMALIZÁLÁS INDÍTÁSA ($runs FUTÁS PÁRHUZAMOSAN) ===")
    val startTime = System.currentTimeMillis()

    // Párhuzamos indítás a CPU szálain (Dispatchers.Default optimalizálja a magok használatát)
    val deferredResults = (1..runs).map { i ->
        async(Dispatchers.Default) {
            // A deepCopy() miatt minden szál a saját layout példányával dolgozik
            val bestLayoutInRun = improveLayout(initialLayout, corpusData, iterations = 20000)

            // Csendes kiértékelés a végeredményhez (fájlba írás nélkül)
            val finalAnalysis = completeLayoutAnalysis(bestLayoutInRun, corpusData, verbose = false)

            println("--- $i. szál végzett. Effort: ${String.format("%.4f", finalAnalysis.totalEffort)} ---")

            // Visszaadjuk a kiszámolt párost
            Pair(finalAnalysis.totalEffort, bestLayoutInRun)
        }
    }

    // Bevárjuk mind a 15 szálat, és egy listába gyűjtjük az eredményeiket
    val results = deferredResults.awaitAll().toMutableList()
    val totalTimeSeconds = (System.currentTimeMillis() - startTime) / 1000.0
    println("\nMind a $runs futás befejeződött. Teljes idő: ${String.format("%.1f", totalTimeSeconds)} mp")

    // 2. Rendezzük az eredményeket a legkisebb (legjobb) Effort szerint növekvő sorrendbe
    results.sortBy { it.first }

    // 3. Kiválasztjuk a legjobb 3-at
    val top3 = results.take(3)

    println("\n==================================================")
    println("=== A LEGJOBB 3 KIOSZTÁS KIÉRTÉKELÉSE ÉS MENTÉSE ===")
    println("==================================================")

    // --- ÚJ: Mappa létrehozása a mentéshez ---
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
    val timestamp = LocalDateTime.now().format(formatter)
    val safeLayoutName = initialLayout.name.replace(" ", "_")

    val runDirName = "$safeLayoutName-$timestamp"
    val outputDir = File("generatedkeyboards/$runDirName")
    outputDir.mkdirs() // Létrehozza a mappát, ha nem létezik

    println("Eredmények mentése ide: ${outputDir.absolutePath}")

    // A top 3 mentése már hagyományosan, egymás után történik, hogy a fájlba írás véletlenül se akadjon össze
    for ((index, pair) in top3.withIndex()) {
        val rank = index + 1
        val (effort, layout) = pair

        layout.name = "${safeLayoutName}_optimized_$rank"

        println("\n>>> $rank. HELYEZETT (${layout.name}) - Total Effort: ${String.format("%.4f", effort)} <<<")

        printLayout(layout)
        // Verbose = true: itt készülnek el az analysis_results mappába a txt fájlok
        completeLayoutAnalysis(layout, corpusData, verbose = true)
        layoutcharefforts(layout)

        // 1. Kép mentése az új mappába
        val imageOutputPath = File(outputDir, "${layout.name}_heatmap.png").absolutePath
        renderCharEffortlayout(layout, imageOutputPath)

        // 2. YAML fájl mentése az új mappába
        val yamlOutputPath = File(outputDir, "${layout.name}.yaml")
        saveLayoutToYaml(layout, originalYamlFile, yamlOutputPath)
    }
}