package keyboard_analysis

import java.io.File

fun launchRedirectAnalysis() {
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

    redirectAnalysis(layoutFile, trigramFile, layoutName.removeSuffix(".txt"))
}

fun redirectAnalysis(layoutFile: File, trigramFile: File, rawLayoutName: String) {
    println("Redirect (Egykezes irányváltás) elemzése folyamatban...")

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

    var totalRedirects = 0.0
    var leftHandRedirects = 0.0
    var rightHandRedirects = 0.0

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

                        // --- REDIRECT VIZSGÁLAT ---
                        // Feltétel 1: Mindhárom betű ugyanazon a kézen van
                        if (hand1 == hand2 && hand2 == hand3) {

                            // Feltétel 2: Az irány menet közben megfordul
                            // "Hegy" (Peak): Befelé indul, aztán kifelé pattan (pl. Kisujj -> Mutatóujj -> Középső)
                            val isPeak = finger1 < finger2 && finger2 > finger3
                            // "Völgy" (Valley): Kifelé indul, aztán befelé pattan (pl. Mutatóujj -> Kisujj -> Gyűrűs)
                            val isValley = finger1 > finger2 && finger2 < finger3

                            if (isPeak || isValley) {
                                totalRedirects += percentage

                                // Kéz szerinti bontás
                                if (hand1 == 0) {
                                    leftHandRedirects += percentage
                                } else {
                                    rightHandRedirects += percentage
                                }
                            }
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

    val outputFile = File(resultDir, "redirect_analyzed.txt")

    outputFile.bufferedWriter().use { writer ->
        writer.write("Teljes Redirect (Egykezes irányváltások): %.4f%%".format(totalRedirects))
        writer.newLine()
        writer.write("(Cél: Ezt az értéket minél alacsonyabban tartani)")
        writer.newLine()
        writer.newLine()

        writer.write("Kéz szerinti bontás:")
        writer.newLine()
        writer.write("-> Bal kéz Redirect: %.4f%%".format(leftHandRedirects))
        writer.newLine()
        writer.write("-> Jobb kéz Redirect: %.4f%%".format(rightHandRedirects))
        writer.newLine()
    }

    println("Sikeresen végrehajtva: ${outputFile.path}\" létrehozva. (Teljes Redirect: %.4f%%)".format(totalRedirects))
}