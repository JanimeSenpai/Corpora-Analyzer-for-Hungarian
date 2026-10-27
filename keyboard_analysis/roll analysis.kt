package keyboard_analysis

import java.io.File

fun launchRollAnalysis() {
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

    rollAnalysis(layoutFile, trigramFile, layoutName.removeSuffix(".txt"))
}

fun rollAnalysis(layoutFile: File, trigramFile: File, rawLayoutName: String) {
    println("Rollok (2-1 és 1-2) elemzése folyamatban...")

    // 1. Kéz- és ujj-hozzárendelés feltérképezése
    // A szótár értéke egy Pair lesz: (Kéz indexe, Ujj indexe)
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

    var totalRolls = 0.0
    var rolls2_1 = 0.0 // Bal-Bal-Jobb vagy Jobb-Jobb-Bal
    var rolls1_2 = 0.0 // Bal-Jobb-Jobb vagy Jobb-Bal-Bal

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

                        // --- ROLL (2-1) VIZSGÁLAT ---
                        // 1. és 2. betű azonos kézen, 3. betű másik kézen, ÉS az 1-2 betű más ujjon van
                        if (hand1 == hand2 && hand2 != hand3 && finger1 != finger2) {
                            rolls2_1 += percentage
                            totalRolls += percentage
                        }

                        // --- ROLL (1-2) VIZSGÁLAT ---
                        // 1. betű egyik kézen, 2. és 3. betű azonos másik kézen, ÉS a 2-3 betű más ujjon van
                        else if (hand1 != hand2 && hand2 == hand3 && finger2 != finger3) {
                            rolls1_2 += percentage
                            totalRolls += percentage
                        }
                    }
                }
            }
        }
    }

    // 3. Eredmények kiírása
    val datasetName = trigramFile.parentFile.name
    val resultDir = File("analysis_results/$datasetName/$rawLayoutName")
    resultDir.mkdirs()

    val outputFile = File(resultDir, "2roll_analyzed.txt")

    outputFile.bufferedWriter().use { writer ->
        writer.write("Teljes Roll (2-1 és 1-2 együttesen): %.4f%%".format(totalRolls))
        writer.newLine()
        writer.newLine()
        writer.write("Részletes bontás:")
        writer.newLine()
        writer.write("-> 2-1 Rollok (Azonos-Azonos-Más): %.4f%%".format(rolls2_1))
        writer.newLine()
        writer.write("-> 1-2 Rollok (Más-Azonos-Azonos): %.4f%%".format(rolls1_2))
        writer.newLine()
    }
    saveRawMetric(datasetName, rawLayoutName, "2roll", totalRolls)
    println("Sikeresen végrehajtva: ${outputFile.path} létrehozva. (Teljes Roll: %.4f%%)".format(totalRolls))
}