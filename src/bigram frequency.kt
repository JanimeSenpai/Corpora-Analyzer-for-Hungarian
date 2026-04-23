//paraméterben kapott txt fájlon végezzük el a bigramelemzést
//hasonló a betűgyakoriság elemzéshez
//nyissuk meg a fájlt
//szavanként menjünk végig a fájlon, és dobjuk el azokat a szavakat, amik nem valid karaktert tartalmaznak
//a valid karakterek a magyar abc betűi, és a magyar nyelv által használt szimbólumok. a számokat is eldobjuk.
//számoljuk meg a szövegben levő valid karaktereket, mivel az majd kelleni fog a bigramgyakoriság számításához
//betűpárosokat sorrendre való tekintet nélkül tároljuk el megfelelő adastruktúrában (betűpárosok írányítottsága egy másik elemzés lesz)
/*
*
* számoljuk meg, hogy mennyi van az egyes párosokból
* az egyes párosokat rakjuk be egy listába és rendezzük a listát gyakoriság szerint
*a listát írjuk ki egy fájlba, aminek az elérési útja: analysys_results/filepath/bigramfrequency.txt
*A lista legyen táblázatos alakú
*első elem: bigram, utána tabulátor, utána a bigram gyakorisága százalékban, amit úgy számítunk ki, hogy az előfordulást elosztjuk a szövegben található összes bigram számával majd szorzunk százzal, aztán tabulátor aztán előfordulás szám

* layout wikiről pszeudokód
* sfb_count := 0
for each bigram in bigrams:
  finger1 := the finger that presses bigram[0]
  finger2 := the finger that presses bigram[1]
  if finger1 == finger2:
    add bigram's count to sfb_count
sfb_ratio := divide by the total bigrams count
*
* ezután kezdjünk el írni egy "mostfrequentConsonantBigrams.txt" fájlt
* ebben a leggyakoribb 7 mássalhangzóhoz tartozó leggyakoribb 10-10 mássalhangzó bigramot írjuk ki. a leggyakoribb mássalhangzók egy konstans változóban adottak. a mássalhangzók egy konstans változóban adottak. az y nem számít mássalhangzónak
* olyan formában, hogy L bigramjai:
* 10 soron keresztül a bigramok, gyakoriság százalékkal együtt
* aztán T bigramjai, stb
*
* mindegyik fájl kiírásának végén írjunk konzolba üzenetet arról, hogy sikerült végrehajtani a feladatot
* */

import java.io.File

// Data class a bigramok tárolásához
data class BigramFrequency(val bigram: String, val count: Int)

fun launchbigramanalysis() {
    println("Kérem a fájl nevét a bigram elemzéshez (A processable_texts mappában kell lennie):")
    val filename = readln().trim()

    val file = File("processable_texts/$filename")
    if (!file.exists()) {
        println("Hiba: A fájl nem található ($filename).")
        return
    }

    // Átadjuk a fájlnevet a fő függvénynek
    bigramanalysis(filename)
}

fun bigramanalysis(filename: String) {
    val inputFile = File("processable_texts/$filename")

    // Valid karakterek halmaza (gyors kereséshez)
    val validcharacters = "aábcdeéfghiíjklmnoóöőpqrstuúüűvwxyz.,!?:;-\"'()".toSet()

    // --- MÁSSALHANGZÓK ---
    val mostFrequentConsonants = "ltsnkrz"
    val consonants = "bcdfghjklmnpqrstvwxz".toSet()

    // --- MAGÁNHANGZÓK (az 'y'-t is ide sorolva a kérés alapján) ---
    val mostFrequentVowels = "aeioáé"
    val vowels = "aáeéiíoóöőuúüűy.,:?".toSet()

    val bigramMap = mutableMapOf<String, Int>()
    var totalBigrams = 0L

    // 1. Fájl feldolgozása szavanként a memóriabarát streameléssel
    println("Szöveg feldolgozása és bigramok kinyerése...")
    inputFile.useLines { lines ->
        lines.forEach { line ->
            val words = line.split(Regex("\\s+"))

            for (word in words) {
                val lowerWord = word.lowercase().replace('?',',').replace(':','.')

                if (lowerWord.isNotEmpty() && lowerWord.all { it in validcharacters }) {
                    for (i in 0 until lowerWord.length - 1) {
                        val c1 = lowerWord[i]
                        val c2 = lowerWord[i + 1]

                        val bigramKey = if (c1 < c2) "$c1$c2" else "$c2$c1"

                        bigramMap[bigramKey] = bigramMap.getOrDefault(bigramKey, 0) + 1
                        totalBigrams++
                    }
                }
            }
        }
    }

    // 2. Példányosítás és listába rendezés gyakoriság szerint (csökkenő)
    val sortedBigrams = bigramMap.map { BigramFrequency(it.key, it.value) }
        .sortedByDescending { it.count }

    val resultDir = File("analysis_results/$filename")
    resultDir.mkdirs()

    // 3. Fő bigramfrekvencia fájl kiírása (az ÖSSZES bigram)
    val mainOutputFile = File(resultDir, "bigramfrequency.txt")
    mainOutputFile.bufferedWriter().use { writer ->

        // --- ÚJ RÉSZ: Teljes bigramszám kiírása az első sorba ---
        writer.write(totalBigrams.toString())
        writer.newLine()

        for (item in sortedBigrams) {
            val percentage = (item.count.toDouble() / totalBigrams) * 100
            val formattedPercentage = "%.4f".format(percentage)
            writer.write("${item.bigram}\t$formattedPercentage%\t${item.count}")
            writer.newLine()
        }
    }
    println("Sikeresen végrehajtva: bigramfrequency.txt létrehozva.")

    // 4. Leggyakoribb mássalhangzók top 12 MÁSSALHANGZÓ-MÁSSALHANGZÓ bigramjai
    val consonantOutputFile = File(resultDir, "mostfrequentConsonantBigrams.txt")
    consonantOutputFile.bufferedWriter().use { writer ->
        for (targetConsonant in mostFrequentConsonants) {
            writer.write("${targetConsonant.uppercaseChar()} bigramjai:")
            writer.newLine()

            val top12ConsonantPairs = sortedBigrams.filter {
                it.bigram.contains(targetConsonant) &&
                        it.bigram[0] in consonants &&
                        it.bigram[1] in consonants
            }.take(12)

            for (item in top12ConsonantPairs) {
                val percentage = (item.count.toDouble() / totalBigrams) * 100
                val formattedPercentage = "%.4f".format(percentage)

                val sfbCategory = when {
                    percentage <= 0.040 -> "minimal"
                    percentage <= 0.070 -> "low"
                    percentage <= 0.178 -> "medium"
                    else -> "high"
                }

                writer.write("${item.bigram}\t$formattedPercentage%\t$sfbCategory\t${item.count}")
                writer.newLine()
            }
            writer.newLine()
        }
    }
    println("Sikeresen végrehajtva: mostfrequentConsonantBigrams.txt létrehozva.")

    // 5. Leggyakoribb mássalhangzók 12 LEGRITKÁBB MÁSSALHANGZÓ-MÁSSALHANGZÓ bigramjai
    val leastConsonantOutputFile = File(resultDir, "leastfrequentConsonantBigrams.txt")
    leastConsonantOutputFile.bufferedWriter().use { writer ->
        for (targetConsonant in mostFrequentConsonants) {
            writer.write("${targetConsonant.uppercaseChar()} legritkább bigramjai:")
            writer.newLine()

            val bottom12ConsonantPairs = sortedBigrams.filter {
                it.bigram.contains(targetConsonant) &&
                        it.bigram[0] in consonants &&
                        it.bigram[1] in consonants
            }.takeLast(12).reversed()

            for (item in bottom12ConsonantPairs) {
                val percentage = (item.count.toDouble() / totalBigrams) * 100
                val formattedPercentage = "%.4f".format(percentage)

                val sfbCategory = when {
                    percentage <= 0.040 -> "minimal"
                    percentage <= 0.070 -> "low"
                    percentage <= 0.178 -> "medium"
                    else -> "high"
                }

                writer.write("${item.bigram}\t$formattedPercentage%\t$sfbCategory\t${item.count}")
                writer.newLine()
            }
            writer.newLine()
        }
    }
    println("Sikeresen végrehajtva: leastfrequentConsonantBigrams.txt létrehozva.")

    // --- ÚJ RÉSZ: MAGÁNHANGZÓK ELEMZÉSE ---

    // 6. Leggyakoribb magánhangzók top 12 MAGÁNHANGZÓ-MAGÁNHANGZÓ bigramjai
    val vowelOutputFile = File(resultDir, "mostfrequentVowelBigrams.txt")
    vowelOutputFile.bufferedWriter().use { writer ->
        for (targetVowel in mostFrequentVowels) {
            writer.write("${targetVowel.uppercaseChar()} bigramjai:")
            writer.newLine()

            // Kiszűrjük a tiszta magánhangzó-párosokat
            val top12VowelPairs = sortedBigrams.filter {
                it.bigram.contains(targetVowel) &&
                        it.bigram[0] in vowels &&
                        it.bigram[1] in vowels
            }.take(12)

            for (item in top12VowelPairs) {
                val percentage = (item.count.toDouble() / totalBigrams) * 100
                val formattedPercentage = "%.4f".format(percentage)

                val sfbCategory = when {
                    percentage <= 0.040 -> "minimal"
                    percentage <= 0.070 -> "low"
                    percentage <= 0.178 -> "medium"
                    else -> "high"
                }

                writer.write("${item.bigram}\t$formattedPercentage%\t$sfbCategory\t${item.count}")
                writer.newLine()
            }
            writer.newLine()
        }
    }
    println("Sikeresen végrehajtva: mostfrequentVowelBigrams.txt létrehozva.")

    // 7. Leggyakoribb magánhangzók 12 LEGRITKÁBB MAGÁNHANGZÓ-MAGÁNHANGZÓ bigramjai
    val leastVowelOutputFile = File(resultDir, "leastfrequentVowelBigrams.txt")
    leastVowelOutputFile.bufferedWriter().use { writer ->
        for (targetVowel in mostFrequentVowels) {
            writer.write("${targetVowel.uppercaseChar()} legritkább bigramjai:")
            writer.newLine()

            // Kiszűrjük a tiszta magánhangzó-párosokat a lista végéről
            val bottom12VowelPairs = sortedBigrams.filter {
                it.bigram.contains(targetVowel) &&
                        it.bigram[0] in vowels &&
                        it.bigram[1] in vowels
            }.takeLast(12).reversed()

            for (item in bottom12VowelPairs) {
                val percentage = (item.count.toDouble() / totalBigrams) * 100
                val formattedPercentage = "%.4f".format(percentage)

                val sfbCategory = when {
                    percentage <= 0.040 -> "minimal"
                    percentage <= 0.070 -> "low"
                    percentage <= 0.178 -> "medium"
                    else -> "high"
                }

                writer.write("${item.bigram}\t$formattedPercentage%\t$sfbCategory\t${item.count}")
                writer.newLine()
            }
            writer.newLine()
        }
    }
    println("Sikeresen végrehajtva: leastfrequentVowelBigrams.txt létrehozva.")
}