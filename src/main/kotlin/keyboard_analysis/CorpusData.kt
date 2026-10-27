package keyboard_analysis

import java.io.File

class CorpusData(
    val datasetName: String,
    val letterCounts: Map<Char, Long>,  //ez a layout effort elemzéshez fog kelleni, amihez darabszámokat adunk paraméterként, nem százalékos értéket
    val totalLetters: Long,
    val bigrams: Map<String, Double> , //betűpáros és a négytizedes pontosságú százalékos aránya
    val trigrams: Map<String, Double>,
    val spaceIncludedTrigrams: Map<String, Double>,
) {
    companion object {
        fun loadFromDataset(datasetName: String): CorpusData {
            val cleanName = if (datasetName.endsWith(".txt")) datasetName else "$datasetName.txt"
            val baseDir = File("analysis_results/$cleanName")

            // 1. Karaktergyakoriság beolvasása (Itt nincs total az első sorban, muszáj összeadni)
            val letters = HashMap<Char, Long>()
            var totalL = 0L
            val letterFile = File(baseDir, "letterfrequency.txt")
            if (letterFile.exists()) {
                letterFile.forEachLine { line ->
                    val parts = line.split('\t')
                    if (parts.size >= 3) {
                        val charStr = parts[0]
                        val count = parts[2].trim().toLongOrNull() ?: 0L

                        val actualChar = when (charStr) {
                            "[SPACE]" -> ' '
                            "\\n" -> '\n'
                            "\\t" -> '\t'
                            "\\r" -> '\r'
                            else -> {
                                // continue helyett címkézett return a lambda megszakításához
                                if (charStr.isNotEmpty()) charStr[0] else return@forEachLine
                            }
                        }

                        letters[actualChar] = count
                        totalL += count
                    }
                }
            }

            // 2. Bigram gyakoriság beolvasása (Total az első sorban)
            // A korábbi kódjaink az analysis_results mappát használták

            val bigramFile = File(baseDir, "bigramfrequency.txt")
            val loadedBigrams = mutableMapOf<String, Double>()
            if (!bigramFile.exists()) {
                println("Hiba: A bigram gyakorisági fájl nem található ($bigramFile).")
            } else {
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
                                loadedBigrams[bigram] = percentage
                            }
                        }
                    }
                }
            }


            // 3. Trigram gyakoriság beolvasása (Total az első sorban)
            val trigramFile = File(baseDir,"trigramfrequency.txt")
            val loadedTrigrams = mutableMapOf<String, Double>()
            if (!trigramFile.exists()) {
                println("Hiba: A trigram gyakorisági fájl nem található ($trigramFile).")
            }else{
                trigramFile.useLines { lines ->
                    var isFirstLine = true
                    lines.forEach { line ->
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
                                loadedTrigrams[trigram] = percentage
                            }
                        }
                    }
                }
            }



            // 4. Szóközös trigramok beolvasása az SFS-1 elemzéshez (Total az első sorban)
            val spaceIncludedTrigramFile = File(baseDir,"spaceincludedtrigramfrequency.txt")
            val loadedSpaceIncludedTrigrams = mutableMapOf<String, Double>()
            if (!spaceIncludedTrigramFile.exists()) {
                println("Hiba: A spaceIncludedTrigramFile gyakorisági fájl nem található ($spaceIncludedTrigramFile).")
            }else{
                spaceIncludedTrigramFile.useLines { lines ->
                    var isFirstLine = true
                    lines.forEach { line ->
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
                                loadedSpaceIncludedTrigrams[trigram] = percentage
                            }
                        }
                    }
                }
            }



            return CorpusData(
                datasetName=cleanName,
                letterCounts = letters,
                totalLetters = totalL,
                bigrams = loadedBigrams,
                trigrams = loadedTrigrams,
                spaceIncludedTrigrams = loadedSpaceIncludedTrigrams,
            )
        }
    }
}