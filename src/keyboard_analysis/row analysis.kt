package keyboard_analysis

import java.io.File

fun launchRowUsageAnalysis() {
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

    // Ide is a szimpla karaktergyakoriság kell
    val charFreqFile = File("analysis_results/$datasetName/charfrequency.txt")
    if (!charFreqFile.exists()) {
        println("Hiba: A karaktergyakorisági fájl nem található ($charFreqFile).")
        return
    }

    rowUsageAnalysis(layoutFile, charFreqFile, layoutName.removeSuffix(".txt"))
}

fun rowUsageAnalysis(layoutFile: File, charFreqFile: File, rawLayoutName: String) {
    println("Sorok Terhelésének Elemzése (Row Usage) folyamatban...")

    // 1. Kiosztás beolvasása (Itt a Sor (Row) koordináta kell)
    val charToRow = mutableMapOf<Char, Int>()

    layoutFile.useLines { lines ->
        lines.forEachIndexed { fingerIndex, line ->
            if (fingerIndex < 8) {
                val cleanLine = line.lowercase().filter { !it.isWhitespace() }

                for (i in cleanLine.indices) {
                    val char = cleanLine[i]

                    // A helykitöltő alulvonást átugorjuk
                    if (char == '_') continue

                    // A sor kiszámítása (0: Num, 1: Top, 2: Home, 3: Bottom)
                    val row = i % 4

                    charToRow[char] = row
                }
            }
        }
    }

    // Változók a 4 sor százalékainak gyűjtésére
    val rowTotals = DoubleArray(4) { 0.0 }

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

                    val row = charToRow[char]
                    if (row != null) {
                        rowTotals[row] += percentage
                    }
                }
            }
        }
    }

    // Normalizálás 100%-ra (szóköz és ismeretlen karakterek nélkül)
    val totalValidHits = rowTotals.sum()

    val rowRatios = DoubleArray(4) { 0.0 }
    if (totalValidHits > 0) {
        for (i in 0 until 4) {
            rowRatios[i] = (rowTotals[i] / totalValidHits) * 100.0
        }
    }

    // 3. Eredmények kiírása
    val datasetName = charFreqFile.parentFile.name
    val resultDir = File("analysis_results/$datasetName/$rawLayoutName")
    resultDir.mkdirs()

    val outputFile = File(resultDir, "row_usage_analyzed.txt")
    val rowNames = listOf("Számsor (Num)", "Felső sor (Top)", "Alapsor (Home)", "Alsó sor (Bottom)")

    outputFile.bufferedWriter().use { writer ->
        writer.write("--- SOROK TERHELÉSÉNEK ARÁNYA (ROW USAGE) ---")
        writer.newLine()
        writer.write("(Normalizálva, kizárólag a kiosztáson szereplő karakterekre)")
        writer.newLine()
        writer.newLine()

        for (i in 0 until 4) {
            writer.write("${rowNames[i]}: %.2f%%".format(rowRatios[i]))
            writer.newLine()
        }

        writer.newLine()
        writer.write("Nyers gyakoriság összeg (szóköz nélkül):")
        writer.newLine()
        for (i in 0 until 4) {
            writer.write("${rowNames[i]}: %.4f%%".format(rowTotals[i]))
            writer.newLine()
        }
    }

    println("Sikeresen végrehajtva: ${outputFile.path} létrehozva. (Home sor aránya: %.2f%%)".format(rowRatios[2]))
}