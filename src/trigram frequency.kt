import java.io.File

// Data class a trigramok tárolásához
data class TrigramFrequency(val trigram: String, val count: Int)

fun launchtrigramanalysis() {
    println("Kérem a fájl nevét a trigram elemzéshez (A processable_texts mappában kell lennie):")
    val filename = readln().trim()

    val file = File("processable_texts/$filename")
    if (!file.exists()) {
        println("Hiba: A fájl nem található ($filename).")
        return
    }

    // Átadjuk a fájlnevet a fő függvénynek
    trigramanalysis(filename)
}

fun trigramanalysis(filename: String) {
    val inputFile = File("processable_texts/$filename")

    // Valid karakterek halmaza
    val validcharacters = "aábcdeéfghiíjklmnoóöőpqrstuúüűvwxyz.,!?:;-\"'()".toSet()

    // --- MÁSSALHANGZÓK ---
    val mostFrequentConsonants = "ltsnkrz"
    val consonants = "bcdfghjklmnpqrstvwxz".toSet()

    val trigramMap = mutableMapOf<String, Int>()
    var totalTrigrams = 0L

    // 1. Fájl feldolgozása szavanként
    println("Szöveg feldolgozása és trigramok kinyerése...")
    inputFile.useLines { lines ->
        lines.forEach { line ->
            val words = line.split(Regex("\\s+"))

            for (word in words) {
                val lowerWord = word.lowercase().hungarianpunctuationreplace()

                if (lowerWord.isNotEmpty() && lowerWord.all { it in validcharacters }) {

                    // Figyelem: a ciklus length - 2-ig megy, mert 3 betűt vizsgálunk egyszerre
                    for (i in 0 until lowerWord.length - 2) {

                        // Irányított trigram: pontosan abban a sorrendben emeljük ki, ahogy a szövegben van
                        val trigramKey = lowerWord.substring(i, i + 3)

                        trigramMap[trigramKey] = trigramMap.getOrDefault(trigramKey, 0) + 1
                        totalTrigrams++
                    }
                }
            }
        }
    }

    // 2. Példányosítás és listába rendezés gyakoriság szerint (csökkenő)
    val sortedTrigrams = trigramMap.map { TrigramFrequency(it.key, it.value) }
        .sortedByDescending { it.count }

    val resultDir = File("analysis_results/$filename")
    resultDir.mkdirs()

    // 3. Fő trigramfrekvencia fájl kiírása (az ÖSSZES trigram)
    val mainOutputFile = File(resultDir, "trigramfrequency.txt")
    mainOutputFile.bufferedWriter().use { writer ->

        // Teljes trigramszám kiírása az első sorba
        writer.write(totalTrigrams.toString())
        writer.newLine()

        for (item in sortedTrigrams) {
            val percentage = (item.count.toDouble() / totalTrigrams) * 100
            val formattedPercentage = "%.4f".format(percentage)
            writer.write("${item.trigram}\t$formattedPercentage%\t${item.count}")
            writer.newLine()
        }
    }
    println("Sikeresen végrehajtva: trigramfrequency.txt létrehozva.")

    // 4. Leggyakoribb mássalhangzók top 12 MÁSSALHANGZÓ-MÁSSALHANGZÓ-MÁSSALHANGZÓ trigramjai
    val consonantOutputFile = File(resultDir, "mostfrequentConsonantTrigrams.txt")
    consonantOutputFile.bufferedWriter().use { writer ->
        for (targetConsonant in mostFrequentConsonants) {
            writer.write("${targetConsonant.uppercaseChar()} trigramjai:")
            writer.newLine()

            val top12ConsonantTrios = sortedTrigrams.filter {
                // 1. Tartalmaznia kell a cél-mássalhangzót
                it.trigram.contains(targetConsonant) &&
                        // 2. MINDEGYIK karakternek a mássalhangzó-halmazban kell lennie
                        it.trigram[0] in consonants &&
                        it.trigram[1] in consonants &&
                        it.trigram[2] in consonants
            }.take(12)

            for (item in top12ConsonantTrios) {
                val percentage = (item.count.toDouble() / totalTrigrams) * 100
                val formattedPercentage = "%.4f".format(percentage)

                // Kiírás: Trigram \t Százalék% \t Darabszám
                writer.write("${item.trigram}\t$formattedPercentage%\t${item.count}")
                writer.newLine()
            }
            writer.newLine()
        }
    }
    println("Sikeresen végrehajtva: mostfrequentConsonantTrigrams.txt létrehozva.")

    // 5. Az ÖSSZES tisztán mássalhangzó trigram (gyakorisági sorrendben, csoportosítás nélkül)
    val pureConsonantOutputFile = File(resultDir, "consonanttrigrams.txt")
    pureConsonantOutputFile.bufferedWriter().use { writer ->

        // Kiszűrjük azokat a trigramokat, ahol mind a 3 karakter mássalhangzó
        val pureConsonantTrios = sortedTrigrams.filter {
            it.trigram[0] in consonants &&
                    it.trigram[1] in consonants &&
                    it.trigram[2] in consonants
        }

        // Írjuk ki a formátumnak megfelelően
        for (item in pureConsonantTrios) {
            // A százalékot továbbra is a teljes szöveg trigramjaihoz (totalTrigrams) viszonyítjuk,
            // hogy lásd a globális súlyukat.
            val percentage = (item.count.toDouble() / totalTrigrams) * 100
            val formattedPercentage = "%.4f".format(percentage)
            writer.write("${item.trigram}\t$formattedPercentage%\t${item.count}")
            writer.newLine()
        }
    }
    println("Sikeresen végrehajtva: consonanttrigrams.txt létrehozva.")
}