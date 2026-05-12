package keyboard_analysis

import java.io.File

fun launchHandBalanceAnalysis() {
    println("Kérem az elemzendő billentyűzetkiosztás nevét (a keyboards_simple mappából):")
    var layoutName = readln().trim()
    if (!layoutName.endsWith(".txt")) layoutName += ".txt"

    val layoutFile = File("keyboards_simple/$layoutName")
    if (!layoutFile.exists()) {
        println("Hiba: A billentyűzet fájl nem található ($layoutFile).")
        return
    }

    println("Kérem az adathalmaz nevét (pontosan úgy, ahogy a mappa neve szerepel, pl. 'szoveg.txt'):")
    var datasetName = readln().trim()
    if (!datasetName.endsWith(".txt")) datasetName += ".txt"

    // Ehhez az elemzéshez a szimpla karaktergyakoriság kell
    val charFreqFile = File("analysis_results/$datasetName/letterfrequency.txt")
    if (!charFreqFile.exists()) {
        println("Hiba: A karaktergyakorisági fájl nem található ($charFreqFile).")
        return
    }

    handBalanceAnalysis(layoutFile, charFreqFile, layoutName.removeSuffix(".txt"))
}

fun handBalanceAnalysis(layoutFile: File, charFreqFile: File, rawLayoutName: String) {
    println("Bal-Jobb Kéz Terhelés Elemzése folyamatban...")

    // 1. Kiosztás beolvasása (Csak az ujj-azonosító kell, a pozíció nem)
    val charToFinger = mutableMapOf<Char, Int>()

    layoutFile.useLines { lines ->
        lines.forEachIndexed { fingerIndex, line ->
            if (fingerIndex < 8) {
                // Itt nyugodtan használhatjuk az összevont, letisztult szűrőt
                for (char in line.lowercase().filter { !it.isWhitespace() && it != '_' }) {
                    charToFinger[char] = fingerIndex
                }
            }
        }
    }

    var leftHandTotal = 0.0
    var rightHandTotal = 0.0

    // 2. Karaktergyakoriság fájl feldolgozása
    charFreqFile.useLines { lines ->
        var isFirstLine = true

        lines.forEach { line ->
            if (isFirstLine) {
                isFirstLine = false
                return@forEach
            }

            val parts = line.split('\t')
            if (parts.size >= 2) {
                val charStr = parts[0]
                if (charStr.isNotEmpty()) {
                    val char = charStr[0]
                    val percentageStr = parts[1].replace("%", "").replace(",", ".")
                    val percentage = percentageStr.toDoubleOrNull() ?: 0.0

                    val finger = charToFinger[char]
                    if (finger != null) {
                        // 0, 1, 2, 3 -> Bal kéz
                        if (finger < 4) {
                            leftHandTotal += percentage
                        }
                        // 4, 5, 6, 7 -> Jobb kéz
                        else {
                            rightHandTotal += percentage
                        }
                    }
                }
            }
        }
    }

    // A szóköz nélküli teljes leütésszám (mivel a szóköz nincs benne a 8 ujjban,
    // a left + right nem feltétlenül ad ki 100%-ot a nyers gyakoriságból).
    // Ezért normalizáljuk őket egymáshoz képest:
    val totalValidHits = leftHandTotal + rightHandTotal

    val leftRatio = if (totalValidHits > 0) (leftHandTotal / totalValidHits) * 100.0 else 0.0
    val rightRatio = if (totalValidHits > 0) (rightHandTotal / totalValidHits) * 100.0 else 0.0

    // 3. Eredmények kiírása
    val datasetName = charFreqFile.parentFile.name
    val resultDir = File("analysis_results/$datasetName/$rawLayoutName")
    resultDir.mkdirs()

    val outputFile = File(resultDir, "hand_balance_analyzed.txt")

    outputFile.bufferedWriter().use { writer ->
        writer.write("--- BAL-JOBB KÉZ HASZNÁLATI ARÁNY ---")
        writer.newLine()
        writer.write("Bal kéz (0-3. ujjak): %.2f%%".format(leftRatio))
        writer.newLine()
        writer.write("Jobb kéz (4-7. ujjak): %.2f%%".format(rightRatio))
        writer.newLine()
        writer.newLine()
        writer.write("Nyers gyakoriság összeg (szóköz és layoutban nem szereplő karakterek nélkül):")
        writer.newLine()
        writer.write("Bal kéz: %.4f%%".format(leftHandTotal))
        writer.newLine()
        writer.write("Jobb kéz: %.4f%%".format(rightHandTotal))
        writer.newLine()
    }

    println("Sikeresen végrehajtva: ${outputFile.path} létrehozva. (Bal: %.2f%% - Jobb: %.2f%%)".format(leftRatio, rightRatio))
}