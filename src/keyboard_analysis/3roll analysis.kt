package keyboard_analysis

import java.io.File


fun launch3RollAnalysis() {
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

    val trigramFile = File("analysis_results/$datasetName/trigramfrequency.txt")
    if (!trigramFile.exists()) {
        println("Hiba: A trigram gyakorisági fájl nem található ($trigramFile).")
        return
    }

    threeRollAnalysis(layoutFile, trigramFile, layoutName.removeSuffix(".txt"))
}

fun threeRollAnalysis(layoutFile: File, trigramFile: File, rawLayoutName: String) {
    println("3Roll (Egykezes rollok Inward/Outward) elemzése folyamatban...")

    // 1. Kéz- és ujj-hozzárendelés feltérképezése
    val charMap = mutableMapOf<Char, Pair<Int, Int>>()
    layoutFile.useLines { lines ->
        lines.forEachIndexed { fingerIndex, line ->
            if (fingerIndex < 8) {
                val hand = if (fingerIndex < 4) 0 else 1 // 0 = Bal, 1 = Jobb
                for (char in line.lowercase().filter { !it.isWhitespace()&&it!='_' }) {
                    charMap[char] = Pair(hand, fingerIndex)
                }
            }
        }
    }

    var total3Rolls = 0.0
    var inward3Rolls = 0.0
    var outward3Rolls = 0.0

    // 2. Trigram fájl feldolgozása
    trigramFile.useLines { lines ->
        var isFirstLine = true
        lines.forEach { line ->
            if (isFirstLine) {
                isFirstLine = false
                return@forEach
            }

            val parts = line.split('\t')
            if (parts.size >= 2) {
                val trigram = parts[0]
                val percentageStr = parts[1].replace("%", "").replace(",", ".")
                val percentage = percentageStr.toDoubleOrNull() ?: 0.0

                if (trigram.length == 3) {
                    val info1 = charMap[trigram[0]]
                    val info2 = charMap[trigram[1]]
                    val info3 = charMap[trigram[2]]

                    if (info1 != null && info2 != null && info3 != null) {
                        val hand1 = info1.first
                        val hand2 = info2.first
                        val hand3 = info3.first
                        val finger1 = info1.second
                        val finger2 = info2.second
                        val finger3 = info3.second

                        if (hand1 == hand2 && hand2 == hand3) {
                            val isIncreasing = finger1 < finger2 && finger2 < finger3
                            val isDecreasing = finger1 > finger2 && finger2 > finger3

                            if (isIncreasing || isDecreasing) {
                                total3Rolls += percentage
                                if (hand1 == 0) { // BAL KÉZ
                                    if (isIncreasing) inward3Rolls += percentage
                                    if (isDecreasing) outward3Rolls += percentage
                                } else { // JOBB KÉZ
                                    if (isDecreasing) inward3Rolls += percentage
                                    if (isIncreasing) outward3Rolls += percentage
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ÚJ RÉSZ: In:Out Ratio kiszámítása ---
    val inOutRatio = if (outward3Rolls > 0) inward3Rolls / outward3Rolls else 0.0

    // 3. Eredmények kiírása
    val datasetName = trigramFile.parentFile.name
    val resultDir = File("analysis_results/$datasetName/$rawLayoutName")
    resultDir.mkdirs()

    val outputFile = File(resultDir, "3roll_analyzed.txt")

    outputFile.bufferedWriter().use { writer ->
        writer.write("Teljes 3Roll (Egykezes gördülés): %.4f%%".format(total3Rolls))
        writer.newLine()
        writer.newLine()
        writer.write("Irány szerinti bontás:")
        writer.newLine()
        writer.write("-> Inward 3Roll: %.4f%%".format(inward3Rolls))
        writer.newLine()
        writer.write("-> Outward 3Roll: %.4f%%".format(outward3Rolls))
        writer.newLine()
        writer.newLine()

        // Az In:Out arány kiírása
        writer.write("In:Out Ratio: %.4f".format(inOutRatio))
        writer.newLine()
        if (inOutRatio > 1.0) {
            writer.write("(A kiosztás belső orientáltságú - kényelmesebb)")
        } else if (inOutRatio < 1.0 && inOutRatio > 0.0) {
            writer.write("(A kiosztás külső orientáltságú - megterhelőbb)")
        }
    }

    println("Sikeresen végrehajtva: ${outputFile.path} létrehozva. In:Out Ratio: %.4f".format(inOutRatio))
}