package keyboard_analysis

/*
* keyboard layouts are located in keyboards_simple folder
* such as qwerty.txt
* each line of characters corresponds to a finger
* the order of line goes as the following: left pinky, left ring, left middle, left index, right index, right middle, right ring, right pinky
*
* kérdezd meg a usert, hogy mi az elemzendő keyboardlayout neve
* A névből képezd a file elérési útvonalát: keyboards_simple\NÉV.txt
* a .txt végződést adjuk hozzá
*
* azután kérdezzük meg, hogy mely adathalmaz alapján végezzük az elemzést
* nyissuk meg az adathalmazt, melynek elérési útja: analysisresults\adathalmazneve.txt\bigramfrequency.txt
* itt az adathalmazneve.txt egyben kapott paraméter, mert mappa elérési útját másolja ki a felhasználó a mappakezelővel
*
* ezután kezdődik a tényleges elemzés
* olvassuk be soronként a billyentyűzetet
* képezd az egyes betűkből az ujjakra eső összes lehetséges bigramot.
*
* javasolt adatstruktúra: egy nyolcelemű lista, aminek az egyes elemei az egyes ujjakat reprezentálják
* az egyes elemekben az egyes betűpárok
* ezután menjünk végig minden egyes ujj minden egyes betűpárján, és keressük meg, hogy hol van a bigramfrequency.txt fájlban.
* a hozzá tartozó bigram százalékot adjuk hozzá az ujjhoz tartozó bigramszázalékhoz
*
* A végén készítsünk egy output filet: billentyűzetneve_sfb_analyzed.txt
* első sor: teljes sfb%
* következő sorok: egyes ujjakhoz tartozó sfb%-ok
*
*
*
* */
import java.io.File

fun launchsimplesfbanalysis() {
    println("Kérem az elemzendő billentyűzetkiosztás nevét (a keyboards_simple mappából):")
    var layoutName = readln().trim()
    if (!layoutName.endsWith(".txt")) layoutName += ".txt"

    val layoutFile = File("keyboards_simple/$layoutName")
    if (!layoutFile.exists()) {
        println("Hiba: A billentyűzet fájl nem található ($layoutFile).")
        return
    }

    println("Kérem az adathalmaz nevét (pl. aminek a mappája az analysis_results-ban van):")
    var datasetName = readln().trim()
    if (!datasetName.endsWith(".txt")) datasetName += ".txt"

    // A korábbi kódjaink az analysis_results mappát használták
    val bigramFile = File("analysis_results/$datasetName/bigramfrequency.txt")
    if (!bigramFile.exists()) {
        println("Hiba: A bigram gyakorisági fájl nem található ($bigramFile).")
        return
    }

    simplesfbanalysis(layoutFile, bigramFile, layoutName.removeSuffix(".txt"))
}

fun simplesfbanalysis(layoutFile: File, bigramFile: File, rawLayoutName: String) {
    println("Billentyűzet elemzése folyamatban (SFB és SKS szétválasztásával)...")

    // 1. Billentyűzet betűinek feltérképezése (Karakter -> Ujj indexe 0-7)
    val charToFinger = mutableMapOf<Char, Int>()
    layoutFile.useLines { lines ->
        lines.forEachIndexed { fingerIndex, line ->
            if (fingerIndex < 8) {
                for (char in line.lowercase().filter { !it.isWhitespace()&&it!='_' }) {
                    charToFinger[char] = fingerIndex
                }
            }
        }
    }

    // Külön tömbök és számlálók az SFB (külön gomb) és SKS (azonos gomb) tárolására
    val fingerSfbs = DoubleArray(8) { 0.0 }
    val fingerSks = DoubleArray(8) { 0.0 }
    var totalSfb = 0.0
    var totalSks = 0.0

    // 2. Bigram fájl feldolgozása
    bigramFile.useLines { lines ->
        var isFirstLine = true

        lines.forEach { line ->
            if (isFirstLine) {
                isFirstLine = false
                return@forEach
            }

            val parts = line.split('\t')
            if (parts.size >= 2) {
                val bigram = parts[0]
                val percentageStr = parts[1].replace("%", "").replace(",", ".")
                val percentage = percentageStr.toDoubleOrNull() ?: 0.0

                if (bigram.length == 2) {
                    val finger1 = charToFinger[bigram[0]]
                    val finger2 = charToFinger[bigram[1]]

                    // Ha mindkét betű rajta van a billentyűzeten, ÉS ugyanaz az ujj nyomja őket
                    if (finger1 != null && finger1 == finger2) {

                        // --- ÚJ RÉSZ: SKS és SFB szétválasztása ---
                        if (bigram[0] == bigram[1]) {
                            // SKS: Ugyanaz a betű egymás után (pl. "tt", "ll")
                            fingerSks[finger1] += percentage
                            totalSks += percentage
                        } else {
                            // Valódi SFB: Két különböző betű ugyanazzal az ujjal (pl. "ed", "rt")
                            fingerSfbs[finger1] += percentage
                            totalSfb += percentage
                        }
                    }
                }
            }
        }
    }

    // 3. Eredmények kiírása
    val datasetName = bigramFile.parentFile.name
    val resultDir = File("analysis_results/$datasetName/$rawLayoutName")
    resultDir.mkdirs()

    val outputFile = File(resultDir, "sfb_analyzed.txt")

    val fingerNames = listOf(
        "Left Pinky", "Left Ring", "Left Middle", "Left Index",
        "Right Index", "Right Middle", "Right Ring", "Right Pinky"
    )

    outputFile.bufferedWriter().use { writer ->
        // Összesített eredmények a fájl tetején
        writer.write("Teljes SFB (Valódi, külön gombos): %.4f%%".format(totalSfb))
        writer.newLine()
        writer.write("Teljes SKB (Azonos gomb duplázása): %.4f%%".format(totalSks))
        writer.newLine()
        writer.newLine()

        // Valódi SFB ujjankénti lebontás
        writer.write("--- UJJANKÉNTI SFB ---")
        writer.newLine()
        for (i in 0 until 8) {
            writer.write("${fingerNames[i]}: %.4f%%".format(fingerSfbs[i]))
            writer.newLine()
        }

        writer.newLine()

        // SKS ujjankénti lebontás (Informatív, hogy lásd, melyik ujjat terhelik a duplázások)
        writer.write("--- UJJANKÉNTI SKB (same key bigram) ---")
        writer.newLine()
        for (i in 0 until 8) {
            writer.write("${fingerNames[i]}: %.4f%%".format(fingerSks[i]))
            writer.newLine()
        }
    }
    saveRawMetric(datasetName, rawLayoutName, "sfb_total", totalSfb)
    println("Sikeresen végrehajtva: ${outputFile.path} létrehozva.")
}