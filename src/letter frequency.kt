import java.io.File

//kezdésnek írjunk egy programot, ami egy fájlra működik
//nyissuk meg a fájlt
//dobjuk be az összes karaktert egy listába
//számoljuk meg a szöveg teljes hosszát
//készítsünk egy listát amiben egész számok lehetnek, töltsük fel nullákkal
//menjünk végig a szövegen és amilyen karaktert találunk annak az elemszámát növeljük eggyel
//készítsünk egy data classt, aminek két eleme van: karakter és hozzá tartozó előfordulási szám
//a két lista elemeiből készítsünk ilyen példányokat
//a példányokat rakjuk be egy listába
//a listát rendezzük példányszám szerint
//a listát írjuk ki egy fájlba, aminek az elérési útja: analysys_results/filepath/letterfrequency.txt
//A lista legyen táblázatos alakú
//első elem: karakter, után tabulátor, utána a karakter gyakorisága százalékban, amit úgy számítunk ki, hogy az előfordulást elosztjuk a teljes szöveg hosszával és szorzunk százzal, aztán tabulátor aztán előfordulás szám

// 1. Data class a karakter és a gyakoriság tárolására




data class CharFrequency(val character: Char, val count: Int)

fun letterfrequency(filepath: String) {
    val inputFile = File("processable_texts\\$filepath")

    // Szótár a karakterek számolásához és egy változó a teljes hosszhoz
    val frequencyMap = mutableMapOf<Char, Int>()
    var totalLength = 0L

    // 2. Fájl streamelése karakterenként (nagyon memóriabarát)
    inputFile.bufferedReader().use { reader ->
        var charInt: Int
        // Olvasás amíg el nem érjük a fájl végét (-1)
        while (reader.read().also { charInt = it } != -1) {
            val char = charInt.toChar().lowercaseChar()

            // --- ÚJ RÉSZ: Formázó karakterek eldobása ---
            if (char == '\n' || char == '\t' || char == '\r') {
                continue // Azonnal visszaugrik a while ciklus elejére, a következő karakterre
            }

            // Ez a rész már csak az érvényes karaktereknél fut le
            // Ha a karakter még nincs a szótárban, 0-ról indul, amúgy hozzáad egyet
            frequencyMap[char] = frequencyMap.getOrDefault(char, 0) + 1
            totalLength++
        }
    }

    // 3. Példányosítás és listába rendezés
    // A szótár elemeiből CharFrequency objektumokat csinálunk, majd csökkenő sorrendbe rakjuk
    val frequencyList = frequencyMap.map { CharFrequency(it.key, it.value) }
        .sortedByDescending { it.count }

    // 4. Kimeneti fájl előkészítése
    // A fájlnév kinyerése (pl. "szoveg.txt"), hogy a mappa útvonala ne törjön el,
    // ha a filepath mondjuk "raw_texts/szoveg.txt" volt.
    val filename = inputFile.name
    val outputFile = File("analysis_results/$filename/letterfrequency.txt")

    // Létrehozzuk a mappaszerkezetet, ha még nem létezik
    outputFile.parentFile?.mkdirs()
    println("processing text...")
    // 5. Kiírás fájlba táblázatos formában
    outputFile.bufferedWriter().use { writer ->
        for (item in frequencyList) {
            // A sortörések és tabulátorok elrontanák a táblázat formátumát,
            // ezért ezeket olvasható formára (escape karakterekre) cseréljük a kiírásnál.
            val charDisplay = when (item.character) {
                '\n' -> "\\n"
                '\t' -> "\\t"
                '\r' -> "\\r"
                ' '  -> "[SPACE]"
                else -> item.character.toString()
            }

            // Százalék kiszámítása
            val percentage = (item.count.toDouble() / totalLength) * 100

            // Kiírás a kért formátumban: Karakter \t Százalék% \t Darabszám
            // A százalékot 4 tizedesjegyre formázzuk a pontosság kedvéért
            val formattedPercentage = "%.4f".format(percentage)
            writer.write("$charDisplay\t$formattedPercentage%\t${item.count}")
            writer.newLine()
        }
    }
    println("letterfrequency calculation finished.")
}