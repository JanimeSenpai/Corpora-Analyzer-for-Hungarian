package keyboard_analysis

import java.io.File
import kotlin.math.abs
import kotlin.math.max

// Egy egyszerű osztály a billentyűk pontos helyzetének (koordinátáinak) tárolására
data class KeyPosition(val finger: Int, val row: Int, val col: Int)


fun launchSpaceSfs1Analysis() {
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

    // ÚJ: A szóközös trigram fájlt keressük
    val trigramFile = File("analysis_results/$datasetName/spaceincludedtrigramfrequency.txt")
    if (!trigramFile.exists()) {
        println("Hiba: A szóközös trigram fájl nem található ($trigramFile). Futtasd le előbb az 'sta' parancsot!")
        return
    }

    spaceSfs1Analysis(layoutFile, trigramFile, layoutName.removeSuffix(".txt"))
}

fun spaceSfs1Analysis(layoutFile: File, trigramFile: File, rawLayoutName: String) {
    println("Szóközökön átívelő SFS-1 Távolságalapú elemzése folyamatban...")

    // 1. Kéz, Ujj és Koordináta hozzárendelés feltérképezése (4-es rács logikával)
    val charToPos = mutableMapOf<Char, KeyPosition>()

    layoutFile.useLines { lines ->
        lines.forEachIndexed { fingerIndex, line ->
            if (fingerIndex < 8) {
                val cleanLine = line.lowercase().filter { !it.isWhitespace() }
                for (i in cleanLine.indices) {
                    val char = cleanLine[i]
                    if (char == '_') continue // A layout üres celláit átugorjuk

                    val col = i / 4
                    val row = i % 4
                    charToPos[char] = KeyPosition(fingerIndex, row, col)
                }
            }
        }
    }

    val fingerSfs1 = DoubleArray(8) { 0.0 }
    val fingerSks1 = DoubleArray(8) { 0.0 }
    var totalSfs1 = 0.0
    var totalSks1 = 0.0
    val fingerSfsPenalty = DoubleArray(8) { 0.0 }
    var totalSfsPenalty = 0.0

    // 2. Szóközös Trigram fájl feldolgozása
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
                    val char1 = trigram[0]
                    val char2 = trigram[1]
                    val char3 = trigram[2]

                    val pos1 = charToPos[char1]
                    val pos3 = charToPos[char3]

                    // Csak akkor vizsgáljuk, ha az 1. és 3. karakter érvényes betű (tehát nem szóköz)
                    if (pos1 != null && pos3 != null) {

                        // Ha ugyanaz az ujj nyomja az 1. és 3. betűt
                        if (pos1.finger == pos3.finger) {

                            var isInterruptedByDifferentFinger = false

                            // Ha a középső karakter szóköz (_), akkor a hüvelykujj nyomta, ami MÁS ujj
                            if (char2 == '_') {
                                isInterruptedByDifferentFinger = true
                            } else {
                                //ha nem szóköz, de rajta van egy másik ujjon, akkor is másik ujj
                                val pos2 = charToPos[char2]
                                if (pos2 != null && pos2.finger != pos1.finger) {
                                    isInterruptedByDifferentFinger = true
                                }
                            }

                            if (isInterruptedByDifferentFinger) {
                                // SKS (Azonos gomb, pl. e_e)
                                if (char1 == char3) {
                                    fingerSks1[pos1.finger] += percentage
                                    totalSks1 += percentage
                                }
                                // SFS (Külön gomb, pl. m_y)
                                else {
                                    fingerSfs1[pos1.finger] += percentage
                                    totalSfs1 += percentage

                                    val rowDiff = abs(pos1.row - pos3.row)
                                    val colDiff = abs(pos1.col - pos3.col)
                                    val distance = max(rowDiff, colDiff).toDouble()

                                    val penalty = percentage * distance
                                    fingerSfsPenalty[pos1.finger] += penalty
                                    totalSfsPenalty += penalty
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

    val outputFile = File(resultDir, "space_sfs1_analyzed.txt")
    val fingerNames = listOf("Left Pinky", "Left Ring", "Left Middle", "Left Index", "Right Index", "Right Middle", "Right Ring", "Right Pinky")

    outputFile.bufferedWriter().use { writer ->
        writer.write("--- SZÓKÖZÖKÖN ÁTÍVELŐ ÖSSZESÍTETT EREDMÉNYEK ---")
        writer.newLine()
        writer.write("Teljes SKS-1 (Azonos gomb): %.4f%%".format(totalSks1))
        writer.newLine()
        writer.write("Teljes SFS-1 Gyakoriság (Külön gomb): %.4f%%".format(totalSfs1))
        writer.newLine()

        val avgDistance = if (totalSfs1 > 0) totalSfsPenalty / totalSfs1 else 0.0
        writer.write("SFS-1 Átlagos Ugrási Távolság: %.2f Egység".format(avgDistance))
        writer.newLine()
        writer.write("Teljes SFS-1 Büntetőpont: %.4f".format(totalSfsPenalty))
        writer.newLine()
        writer.newLine()

        writer.write("--- UJJANKÉNTI BÜNTETŐPONTOK (Space SFS-1 Penalty) ---")
        writer.newLine()
        for (i in 0 until 8) { writer.write("${fingerNames[i]}: %.4f".format(fingerSfsPenalty[i])); writer.newLine() }

        writer.newLine()
        writer.write("--- UJJANKÉNTI SFS-1 GYAKORISÁG (%) ---")
        writer.newLine()
        for (i in 0 until 8) { writer.write("${fingerNames[i]}: %.4f%%".format(fingerSfs1[i])); writer.newLine() }
    }
    saveRawMetric(datasetName, rawLayoutName, "space_sfs1_total", totalSfs1)
    println("Sikeresen végrehajtva: ${outputFile.path} létrehozva. (Teljes Space SFS-1 Penalty: %.4f)".format(totalSfsPenalty))
}