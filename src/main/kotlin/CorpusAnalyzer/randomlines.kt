package CorpusAnalyzer

import java.io.File
import kotlin.random.Random

fun createRandomSubset(sourceFilePath: String, n: Int) {
    val sourceFile = File(sourceFilePath)

    if (!sourceFile.exists() || !sourceFile.isFile) {
        println("Hiba: A megadott fájl nem található ($sourceFilePath)")
        return
    }

    val reservoir = mutableListOf<String>()
    var lineCount = 0

    // Reservoir sampling: O(N) futási idő, O(n) memóriaigény, ahol n a minta mérete
    sourceFile.useLines { lines ->
        for (line in lines) {
            if (lineCount < n) {
                // Az első N sort egyenesen betesszük a mintába
                reservoir.add(line)
            } else {
                // Generálunk egy véletlenszámot 0 és az eddig beolvasott sorok száma között
                val j = Random.nextInt(lineCount + 1)
                // Ha a szám kisebb, mint N, kicseréljük a minta egyik elemét
                if (j < n) {
                    reservoir[j] = line
                }
            }
            lineCount++
        }
    }

    // A fájlnév és a kiterjesztés szétválasztása
    val name = sourceFile.nameWithoutExtension
    val ext = sourceFile.extension
    val extPart = if (ext.isNotEmpty()) ".$ext" else ""

    // Új fájl létrehozása a forrásfájl könyvtárában
    val targetFileName = "${name}_random_$n$extPart"
    val targetFile = File(sourceFile.parentFile, targetFileName)

    // A kiválasztott sorok kiírása
    targetFile.bufferedWriter().use { writer ->
        reservoir.forEach { line ->
            writer.appendLine(line)
        }
    }

    println("Feldolgozva: $lineCount sor. Minta ($n sor) elmentve ide: ${targetFile.absolutePath}")
}

// Használati példa:
fun main() {
    // 5000 sort választ ki a megadott korpuszból
    createRandomSubset("processable_texts/huszonkettoHuszonnegyeswikipediahuszonegy.txt", 5000)
}