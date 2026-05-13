package keyboard_analysis

import java.io.File

fun launchCompleteLayoutAnalysis() {
    println("--- TELJES BILLENTYŰZET ELEMZÉS ---")

    // 1. Billentyűzet bekérése
    println("Kérem az elemzendő billentyűzetkiosztás nevét (a keyboards_simple mappából):")
    var layoutName = readln().trim()
    if (!layoutName.endsWith(".txt")) layoutName += ".txt"

    val layoutFile = File("keyboards_simple/$layoutName")
    if (!layoutFile.exists()) {
        println("Hiba: A billentyűzet fájl nem található ($layoutFile).")
        return
    }

    // 2. Adathalmaz bekérése
    println("Kérem az adathalmaz nevét (pontosan úgy, ahogy a mappa neve szerepel, pl. 'szoveg.txt'):")
    var datasetName = readln().trim()
    if (!datasetName.endsWith(".txt")) datasetName += ".txt"

    // 3. Fájlok ellenőrzése (Karakter, Bigram és Trigram is kell a teljes elemzéshez)
    val charFreqFile = File("analysis_results/$datasetName/letterfrequency.txt")
    val bigramFile = File("analysis_results/$datasetName/bigramfrequency.txt")
    val trigramFile = File("analysis_results/$datasetName/trigramfrequency.txt")

    if (!charFreqFile.exists()) {
        println("Hiba: A karaktergyakorisági fájl nem található ($charFreqFile). Kérlek futtasd le előbb a megfelelő adatelőkészítő parancsot!")
        return
    }
    if (!bigramFile.exists()) {
        println("Hiba: A bigram gyakorisági fájl nem található ($bigramFile). Kérlek futtasd le előbb a 'ba' parancsot!")
        return
    }
    if (!trigramFile.exists()) {
        println("Hiba: A trigram gyakorisági fájl nem található ($trigramFile). Kérlek futtasd le előbb a 'ta' parancsot!")
        return
    }

    val rawLayoutName = layoutName.removeSuffix(".txt")

    // 4. Elemzések sorozatos lefuttatása
    println("\n==================================================")
    println("ELEMZÉS INDÍTÁSA: $rawLayoutName (Adathalmaz: $datasetName)")
    println("==================================================")

    // Korábbi nyers adatok törlése
    clearRawMetrics(datasetName, rawLayoutName)

    handBalanceAnalysis(layoutFile, charFreqFile, rawLayoutName)
    println("--------------------------------------------------")

    rowUsageAnalysis(layoutFile, charFreqFile, rawLayoutName)
    println("--------------------------------------------------")

    simplesfbanalysis(layoutFile, bigramFile, rawLayoutName)
    println("--------------------------------------------------")

    alternationAnalysis(layoutFile, trigramFile, rawLayoutName)
    println("--------------------------------------------------")

    rollAnalysis(layoutFile, trigramFile, rawLayoutName)
    println("--------------------------------------------------")

    threeRollAnalysis(layoutFile, trigramFile, rawLayoutName)
    println("--------------------------------------------------")

    redirectAnalysis(layoutFile, trigramFile, rawLayoutName)
    println("--------------------------------------------------")

    spaceSfs1Analysis(layoutFile, trigramFile, rawLayoutName)

    println("==================================================")
    println("MINDEN ELEMZÉS SIKERESEN BEFEJEZŐDÖTT!")
    println("A kimeneti fájlok az 'analysis_results/$datasetName/$rawLayoutName' mappában találhatók.")
    println("==================================================")
}