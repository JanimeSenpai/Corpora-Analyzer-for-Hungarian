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

    // A leggyakoribb mássalhangzók kisbetűsítve
    val mostFrequentConsonants = "ltsnkrz"

    // Az összes mássalhangzó (magánhangzók és az 'y' nélkül)
    val consonants = "bcdfghjklmnpqrstvwxz".toSet()

    val bigramMap = mutableMapOf<String, Int>()
    var totalBigrams = 0L

    // 1. Fájl feldolgozása szavanként a memóriabarát streameléssel
    println("Szöveg feldolgozása és bigramok kinyerése...")
    inputFile.useLines { lines ->
        lines.forEach { line ->
            // Szétvágás whitespace-ek (szóköz, tabulátor) mentén
            val words = line.split(Regex("\\s+"))

            for (word in words) {
                val lowerWord = word.lowercase()

                // Ha a szó nem üres és CSAK a mi valid karaktereinket tartalmazza
                if (lowerWord.isNotEmpty() && lowerWord.all { it in validcharacters }) {

                    // Bigramok kinyerése a szóból
                    for (i in 0 until lowerWord.length - 1) {
                        val c1 = lowerWord[i]
                        val c2 = lowerWord[i + 1]

                        // Irányítatlan bigram: abc sorrendbe rakjuk a két karaktert
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

    // Eredménymappa előkészítése
    val resultDir = File("analysis_results/$filename")
    resultDir.mkdirs()

    // 3. Fő bigramfrekvencia fájl kiírása (az ÖSSZES bigram)
    val mainOutputFile = File(resultDir, "bigramfrequency.txt")
    mainOutputFile.bufferedWriter().use { writer ->
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

            // Kiszűrjük a tiszta mássalhangzó-párosokat
            val top12ConsonantPairs = sortedBigrams.filter {
                // 1. Szerepelnie kell benne az aktuális cél-mássalhangzónak (pl. 'l')
                it.bigram.contains(targetConsonant) &&
                        // 2. Az első karakternek a mássalhangzók listájában kell lennie
                        it.bigram[0] in consonants &&
                        // 3. A második karakternek is a mássalhangzók listájában kell lennie
                        it.bigram[1] in consonants
            }.take(12) // Vesszük az első 12-t a korábbi 10 helyett

            for (item in top12ConsonantPairs) {
                // Százalék kiszámítása
                val percentage = (item.count.toDouble() / totalBigrams) * 100
                val formattedPercentage = "%.4f".format(percentage)

                // SFB besorolás a kért küszöbértékek alapján
                val sfbCategory = when {
                    percentage <= 0.040 -> "minimal"
                    percentage <= 0.070 -> "low"
                    percentage <= 0.178 -> "medium"
                    else -> "high"
                }

                // Kiírás: Bigram \t Százalék% \t SFB Kategória \t Darabszám
                writer.write("${item.bigram}\t$formattedPercentage%\t$sfbCategory\t${item.count}")
                writer.newLine()
            }
            writer.newLine() // Egy üres sor a vizuális elkülönítéshez
        }
    }
    println("Sikeresen végrehajtva: mostfrequentConsonantBigrams.txt létrehozva.")

    // 5. Leggyakoribb mássalhangzók 12 LEGRITKÁBB MÁSSALHANGZÓ-MÁSSALHANGZÓ bigramjai
    val leastConsonantOutputFile = File(resultDir, "leastfrequentConsonantBigrams.txt")
    leastConsonantOutputFile.bufferedWriter().use { writer ->
        for (targetConsonant in mostFrequentConsonants) {
            writer.write("${targetConsonant.uppercaseChar()} legritkább bigramjai:")
            writer.newLine()

            // Kiszűrjük a tiszta mássalhangzó-párosokat, majd a lista legvégéről (legkisebb darabszám)
            // kivesszük az utolsó 12-t. A .reversed() biztosítja, hogy a legritkább legyen legelöl.
            val bottom12ConsonantPairs = sortedBigrams.filter {
                it.bigram.contains(targetConsonant) &&
                        it.bigram[0] in consonants &&
                        it.bigram[1] in consonants
            }.takeLast(12).reversed()

            for (item in bottom12ConsonantPairs) {
                // Százalék kiszámítása
                val percentage = (item.count.toDouble() / totalBigrams) * 100
                val formattedPercentage = "%.4f".format(percentage)

                // SFB besorolás a kért küszöbértékek alapján
                // (Ezeknél a ritka bigramoknál szinte mind "minimal" lesz, de a logika ugyanaz)
                val sfbCategory = when {
                    percentage <= 0.040 -> "minimal"
                    percentage <= 0.070 -> "low"
                    percentage <= 0.178 -> "medium"
                    else -> "high"
                }

                // Kiírás: Bigram \t Százalék% \t SFB Kategória \t Darabszám
                writer.write("${item.bigram}\t$formattedPercentage%\t$sfbCategory\t${item.count}")
                writer.newLine()
            }
            writer.newLine() // Egy üres sor a vizuális elkülönítéshez
        }
    }
    println("Sikeresen végrehajtva: leastfrequentConsonantBigrams.txt létrehozva.")

}