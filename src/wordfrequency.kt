import java.io.File

fun launchCalculateWordFrequency() {
    println("Kérem a fájl nevét a szógyakoriság elemzéshez (A processable_texts mappában kell lennie):")
    val filename = readln().trim()

    val file = File("processable_texts/$filename")
    if (!file.exists()) {
        println("Hiba: A fájl nem található ($filename).")
        return
    }

    println("Kérem a limitet (top hány szót állítsunk össze? pl. 100):")
    val limitInput = readln().trim()
    val limit = limitInput.toIntOrNull() ?: 100 // Ha érvénytelen a bemenet, 100 lesz az alapérték

    // A fájlnevet is átadjuk, hogy a megfelelő mappába tudjuk menteni az eredményt
    calculatewordfrequency(filename, limit)
}

fun calculatewordfrequency(filename: String, limit: Int) {
    println("Szöveg feldolgozása és szógyakoriság számítása (számok kiszűrésével)...")
    val inputFile = File("processable_texts/$filename")

    val wordMap = mutableMapOf<String, Int>()
    var totalWords = 0L

    // 1. Fájl feldolgozása
    inputFile.useLines { lines ->
        lines.forEach { line ->
            // Szóközök mentén darabolunk
            val words = line.split(Regex("\\s+"))

            for (rawWord in words) {
                // Kisbetűssé alakítjuk, és levágjuk a szó eleji/végi leggyakoribb írásjeleket
                val cleanWord = rawWord.lowercase().trim('.', ',', '!', '?', ':', ';', '-', '"', '\'', '(', ')')

                // ÚJ FELTÉTEL: Ha a tisztítás után maradt érvényes szó, ÉS nem tartalmaz egyetlen számjegyet sem
                if (cleanWord.isNotBlank() && cleanWord.none { it.isDigit() }) {
                    wordMap[cleanWord] = wordMap.getOrDefault(cleanWord, 0) + 1
                    totalWords++
                }
            }
        }
    }

    // 2. Rendezzük csökkenő sorrendbe és kivágjuk a top 'limit' elemet
    val sortedTopWords = wordMap.entries
        .sortedByDescending { it.value }
        .take(limit)

    // 3. Kimeneti mappa és fájl előkészítése
    val resultDir = File("analysis_results/$filename")
    resultDir.mkdirs()

    val outputFile = File(resultDir, "top$limit.txt")

    outputFile.bufferedWriter().use { writer ->
        // Első sor: a korpusz összes (számmentes) szavának száma
        writer.write(totalWords.toString())
        writer.newLine()

        // Többi sor: szó \t százalék% \t darabszám
        for ((word, count) in sortedTopWords) {
            val percentage = (count.toDouble() / totalWords) * 100
            val formattedPercentage = "%.4f".format(percentage)

            writer.write("$word\t$formattedPercentage%\t$count")
            writer.newLine()
        }

        // Üres sor elválasztónak
        writer.newLine()

        // Utolsó sor: csak a szavak, egyetlen sorban, szóközzel elválasztva
        val wordsOnlyLine = sortedTopWords.joinToString(" ") { it.key }
        writer.write(wordsOnlyLine)
        writer.newLine()
    }

    println("Sikeresen végrehajtva: top$limit.txt létrehozva a $resultDir mappában.")
}