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

    // Ha a user lehagyta a mappanévről a .txt-t, kiegészítjük, mert a mappáid így vannak elnevezve
    if (!datasetName.endsWith(".txt")) datasetName += ".txt"

    // Így az útvonal pontosan ez lesz: analysis_results/szoveg.txt/trigramfrequency.txt
    val trigramFile = File("analysis_results/$datasetName/trigramfrequency.txt")
    if (!trigramFile.exists()) {
        println("Hiba: A trigram gyakorisági fájl nem található ($trigramFile).")
        return
    }

    // A layoutName-ről továbbra is levesszük a .txt-t, de csak azért, hogy a kimeneti
    // elemzés fájlneve szép maradjon (pl. qwerty_alternation_analyzed.txt)
    alternationAnalysis(layoutFile, trigramFile, layoutName.removeSuffix(".txt"))
}
fun alternationAnalysis(layoutFile: File, trigramFile: File, rawLayoutName: String) {
    println("Alternálás (1-1-1) elemzése folyamatban...")

    // 1. Kéz-hozzárendelés feltérképezése (Karakter -> Kéz: 0 = Bal, 1 = Jobb)
    val charToHand = mutableMapOf<Char, Int>()
    layoutFile.useLines { lines ->
        lines.forEachIndexed { fingerIndex, line ->
            if (fingerIndex < 8) {
                // A 0, 1, 2, 3-as indexű sorok a bal kéz ujjai, a 4, 5, 6, 7-esek a jobb kézé
                val hand = if (fingerIndex < 4) 0 else 1

                for (char in line.lowercase().filter { !it.isWhitespace()&&it!='_' }) {
                    charToHand[char] = hand
                }
            }
        }
    }

    var totalAlternation = 0.0

    // 2. Trigram fájl feldolgozása
    trigramFile.useLines { lines ->
        var isFirstLine = true

        lines.forEach { line ->
            // Az első sorban csak az összesített trigramszám van, ezt átugorjuk
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
        }
    }

    // 3. Eredmények kiírása
    val datasetName = trigramFile.parentFile.name
    val resultDir = File("analysis_results/$datasetName/$rawLayoutName")
    resultDir.mkdirs()

    val outputFile = File(resultDir, "alternation_analyzed.txt")

    outputFile.bufferedWriter().use { writer ->
        writer.write("Teljes Alternálás (Alternate 1-1-1): %.4f%%".format(totalAlternation))
        writer.newLine()
    }

    println("Sikeresen végrehajtva: ${outputFile.path} létrehozva. (Eredmény: %.4f%%)".format(totalAlternation))
}
