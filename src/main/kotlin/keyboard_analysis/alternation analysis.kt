package keyboard_analysis

import java.io.File

/* bemenő paraméterek:
* billentyűzetkiosztás
* mappa, amelyikhez tartozó trigramfrequency.txt felhasználásra kerül
*
* rendeljük a bal és jobb kézhez a betűket a layout alapján
* menjünk végig a trigramokon, és nézzük meg, hogy alternáló trigramnak számítanak-e
* ha alternálás, akkor adjuk hozzá az alternálás százalékhoz a trigram százalékos gyakoriságát
*
* */

fun launchAlternationAnalysis() {
    println("Kérem az elemzendő billentyűzetkiosztás nevét (a keyboards mappából):")
    var layoutName = readln().trim()
    if (!layoutName.endsWith(".yaml")) layoutName += ".yaml"

    val layoutFile = File("keyboards/$layoutName")
    if (!layoutFile.exists()) {
        println("Hiba: A billentyűzet fájl nem található ($layoutFile).")
        return
    }

    println("Kérem az adathalmaz nevét (pontosan úgy, ahogy a mappa neve szerepel, pl. 'szoveg.txt'):")
    var datasetName = readln().trim()

    // Ha a user lehagyta a mappanévről a .txt-t, kiegészítjük, mert a mappáid így vannak elnevezve
    if (!datasetName.endsWith(".txt")) datasetName += ".txt"

    val layoutData = try {
        LayoutParser.parseLayout(layoutFile)
    } catch (e: Exception) {
        println("Hiba a billentyűzet beolvasásakor: ${e.message}")
        return
    }

    val corpusData = CorpusData.loadFromDataset(datasetName)
    if (corpusData.trigrams.isEmpty()) {
        println("Hiba: A trigram gyakorisági adatok nem töltődtek be.")
        return
    }

    // 1. Kéz-hozzárendelés feltérképezése (Karakter -> Kéz: 0 = Bal, 1 = Jobb)
    val charToHandMap = mutableMapOf<Char, Int>()
    val fingerSets = layoutData.getFingerCharacterSets()
    for ((key, chars) in fingerSets) {
        val hand = if (key.startsWith("L")) 0 else 1
        chars.forEach { char -> charToHandMap[char] = hand }
    }

    // A layoutName-ről továbbra is levesszük a kiterjesztést, de csak azért, hogy a kimeneti
    // elemzés fájlneve szép maradjon (pl. qwerty_alternation_analyzed.txt)
    alternationAnalysis(
        charToHand = charToHandMap,
        trigrams = corpusData.trigrams,
        verbose = true,
        layoutName = layoutData.name,
        datasetName = corpusData.datasetName
    )
}

fun alternationAnalysis(
    charToHand: Map<Char, Int>,
    trigrams: Map<String, Double>,
    verbose: Boolean,
    layoutName: String,
    datasetName: String
): Double {
    if (verbose) println("Alternálás (1-1-1) elemzése folyamatban...")

    var totalAlternation = 0.0

    // 2. Trigram adatok feldolgozása
    for ((trigram, percentage) in trigrams) {
        if (trigram.length == 3) {
            val hand1 = charToHand[trigram[0]]
            val hand2 = charToHand[trigram[1]]
            val hand3 = charToHand[trigram[2]]

            // Ha mindhárom betű létezik a billentyűzeten
            if (hand1 != null && hand2 != null && hand3 != null) {

                // TISZTA ALTERNÁLÁS LOGIKÁJA
                // Az 1. kéz nem egyezik a 2. kézzel ÉS a 2. kéz nem egyezik a 3. kézzel.
                if (hand1 != hand2 && hand2 != hand3) {
                    totalAlternation += percentage
                }
            }
        }
    }

    // 3. Eredmények kiírása
    if (verbose) {
        val resultDir = File("analysis_results/$datasetName/$layoutName")
        resultDir.mkdirs()

        val outputFile = File(resultDir, "alternation_analyzed.txt")

        outputFile.bufferedWriter().use { writer ->
            writer.write("Teljes Alternálás (Alternate 1-1-1): %.4f%%".format(totalAlternation))
            writer.newLine()
        }
        saveRawMetric(datasetName, layoutName, "alternation_total", totalAlternation)
        println("Sikeresen végrehajtva: ${outputFile.path} létrehozva. (Eredmény: %.4f%%)".format(totalAlternation))
    }

    return totalAlternation
}