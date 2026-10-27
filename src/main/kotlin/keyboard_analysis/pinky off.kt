package keyboard_analysis
import java.io.File

fun launchPinkyOffAnalysis() {
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

    val charFreqFile = File("analysis_results/$datasetName/letterfrequency.txt")
    if (!charFreqFile.exists()) {
        println("Hiba: A karaktergyakorisági fájl nem található ($charFreqFile).")
        return
    }

    pinkyOffAnalysis(layoutFile, charFreqFile, layoutName.removeSuffix(".txt"))
}

fun pinkyOffAnalysis(layoutFile: File, charFreqFile: File, rawLayoutName: String) {
    println("Pinky Off (Kisujj Nyújtózás) elemzése folyamatban...")

    var leftPinkyHomeChar: Char? = null
    var rightPinkyHomeChar: Char? = null

    val leftPinkyChars = mutableSetOf<Char>()
    val rightPinkyChars = mutableSetOf<Char>()

    // 1. Kiosztás feltérképezése és az alapgombok dinamikus megtalálása
    layoutFile.useLines { lines ->
        lines.forEachIndexed { fingerIndex, line ->
            val cleanLine = line.lowercase()

            if (fingerIndex == 0) { // Bal kisujj
                // Dinamikus alaphelyzet: a legutolsó oszlop Home gombja (hátulról a 2. index a rácsban)
                if (cleanLine.length >= 2) {
                    val homeIndex = cleanLine.length - 2
                    if (cleanLine[homeIndex] != '_') leftPinkyHomeChar = cleanLine[homeIndex]
                }
                for (char in cleanLine) {
                    if (char != '_' && !char.isWhitespace()) leftPinkyChars.add(char)//összes kissujra eső karakter
                }
            } else if (fingerIndex == 7) { // Jobb kisujj
                // Statikus alaphelyzet: a legelső oszlop Home gombja (elölről a 2. index a rácsban)
                if (cleanLine.length > 2 && cleanLine[2] != '_') rightPinkyHomeChar = cleanLine[2]
                for (char in cleanLine) {
                    if (char != '_' && !char.isWhitespace()) rightPinkyChars.add(char)
                }
            }
        }
    }

    var totalPinkyOff = 0.0
    var leftPinkyOff = 0.0
    var rightPinkyOff = 0.0

    // 2. Gyakoriságok beolvasása és összegzése
    charFreqFile.useLines { lines ->
        var isFirstLine = true
        lines.forEach { line ->
            if (isFirstLine) { isFirstLine = false; return@forEach }

            val parts = line.split('\t')
            if (parts.size >= 2) {
                val charStr = parts[0]
                // Kihagyjuk a speciális formátumú [SPACE] sort, mert az nem fizikai gomb
                if (charStr.isNotEmpty() && charStr != "[SPACE]") {
                    val char = charStr[0]
                    val percentageStr = parts[1].replace("%", "").replace(",", ".")
                    val percentage = percentageStr.toDoubleOrNull() ?: 0.0

                    // Ha a betűt a BAL kisujj nyomja, DE az nem az alapgomb
                    if (char in leftPinkyChars && char != leftPinkyHomeChar) {
                        leftPinkyOff += percentage
                        totalPinkyOff += percentage
                    }
                    // Ha a betűt a JOBB kisujj nyomja, DE az nem az alapgomb
                    else if (char in rightPinkyChars && char != rightPinkyHomeChar) {
                        rightPinkyOff += percentage
                        totalPinkyOff += percentage
                    }
                }
            }
        }
    }

    // 3. Eredmények kiírása és CSV mentés hívása
    val datasetName = charFreqFile.parentFile.name
    saveRawMetric(datasetName, rawLayoutName, "pinky_off", totalPinkyOff)

    val resultDir = File("analysis_results/$datasetName/$rawLayoutName")
    resultDir.mkdirs()
    val outputFile = File(resultDir, "pinky_off_analyzed.txt")

    outputFile.bufferedWriter().use { writer ->
        writer.write("--- PINKY OFF (Kizárólag nyújtózásos kisujj leütések) ---")
        writer.newLine()
        writer.write("Bal kisujj alapgomb: ${leftPinkyHomeChar ?: "Nincs"}")
        writer.newLine()
        writer.write("Jobb kisujj alapgomb: ${rightPinkyHomeChar ?: "Nincs"}")
        writer.newLine()
        writer.newLine()
        writer.write("Teljes Pinky Off arány: %.4f%%".format(totalPinkyOff))
        writer.newLine()
        writer.write("-> Bal kéz nyújtózás: %.4f%%".format(leftPinkyOff))
        writer.newLine()
        writer.write("-> Jobb kéz nyújtózás: %.4f%%".format(rightPinkyOff))
        writer.newLine()
    }

    println("Sikeresen végrehajtva: ${outputFile.path} létrehozva. (Pinky Off: %.4f%%)".format(totalPinkyOff))
}