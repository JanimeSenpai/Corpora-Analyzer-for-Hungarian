import java.io.File

// Data class a trigramok tárolásához
data class SpaceIncludedTrigramFrequency(val trigram: String, val count: Int)

fun launchSpaceIncludedTrigramAnalysis() {
    println("Kérem a fájl nevét a szóközös trigram elemzéshez (A processable_texts mappában kell lennie):")
    val filename = readln().trim()

    val file = File("processable_texts/$filename")
    if (!file.exists()) {
        println("Hiba: A fájl nem található ($filename).")
        return
    }

    // Átadjuk a fájlnevet a fő függvénynek
    spaceIncludedTrigramAnalysis(filename)
}

fun spaceIncludedTrigramAnalysis(filename: String) {
    val inputFile = File("processable_texts/$filename")

    // Valid karakterek halmaza
    val validcharacters = "aábcdeéfghiíjklmnoóöőpqrstuúüűvwxyz.,!?:;-\"'()".toSet()

    val trigramMap = mutableMapOf<String, Int>()
    var totalTrigrams = 0L

    // 1. Fájl feldolgozása folytonos ablakcsúsztatással
    println("Szöveg feldolgozása és szóközös trigramok kinyerése...")

    // Ez a változó tárolja az aktuális 3 karakteres ablakot
    var window = ""

    inputFile.useLines { lines ->
        lines.forEach { line ->
            val words = line.split(Regex("\\s+"))

            for (word in words) {
                val lowerWord = word.lowercase().hungarianpunctuationreplace()

                // Ha a szó valid, hozzácsapunk egy szóközt a végéhez, mintha begépeltük volna
                if (lowerWord.isNotEmpty() && lowerWord.all { it in validcharacters }) {
                    val wordWithSpace = "$lowerWord "

                    // Karakterenként csúsztatjuk a 3-as ablakot
                    for (char in wordWithSpace) {
                        window += char//eleinte a window üres

                        // Ha az ablak mérete túlnőtt a 3-on, levágjuk a legelső karaktert
                        if (window.length > 3) {
                            window = window.substring(1)
                        }

                        // Ha az ablak pontosan 3 karakter, elmentjük trigramként
                        if (window.length == 3) {
                            trigramMap[window] = trigramMap.getOrDefault(window, 0) + 1
                            totalTrigrams++
                        }
                    }
                }
            }
        }
    }

    // 2. Példányosítás és listába rendezés gyakoriság szerint (csökkenő)
    val sortedTrigrams = trigramMap.map { SpaceIncludedTrigramFrequency(it.key, it.value) }
        .sortedByDescending { it.count }

    val resultDir = File("analysis_results/$filename")
    resultDir.mkdirs()

    // 3. Fő szóközös trigramfrekvencia fájl kiírása
    val mainOutputFile = File(resultDir, "spaceincludedtrigramfrequency.txt")
    mainOutputFile.bufferedWriter().use { writer ->

        // Teljes trigramszám kiírása az első sorba
        writer.write(totalTrigrams.toString())
        writer.newLine()

        for (item in sortedTrigrams) {
            val percentage = (item.count.toDouble() / totalTrigrams) * 100
            val formattedPercentage = "%.4f".format(percentage)

            // A szóközt _ jelre cseréljük a vizuális átláthatóság kedvéért (pl. "a b" -> "a_b")
            val displayTrigram = item.trigram.replace(' ', '_')

            writer.write("$displayTrigram\t$formattedPercentage%\t${item.count}")
            writer.newLine()
        }
    }
    println("Sikeresen végrehajtva: spaceincludedtrigramfrequency.txt létrehozva.")
}