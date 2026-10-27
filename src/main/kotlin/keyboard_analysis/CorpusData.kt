package keyboard_analysis

import java.io.File

class CorpusData(
    val datasetName: String,
    val letters: Map<String, Double>,  //ez a layout effort elemzéshez és a rowusage-hoz fog kelleni, ezért itt is százalékos arány kell
    val spaceincludedletters: Map<String, Double>,
    val bigrams: Map<String, Double> , //betűpáros és a négytizedes pontosságú százalékos aránya
    val trigrams: Map<String, Double>,
    val spaceIncludedTrigrams: Map<String, Double>,
) {

    companion object {
        fun loadFromDataset(datasetName: String): CorpusData {
            val cleanName = if (datasetName.endsWith(".txt")) datasetName else "$datasetName.txt"
            val baseDir = File("analysis_results/$cleanName")

            // 1. Karaktergyakoriság beolvasása (Total az első sorban itt is)
            val spaceincludedlettersFile = File(baseDir, "CorpusAnalyzer.letterfrequency.txt")
            val loadedspaceincludedletters = mutableMapOf<String, Double>()
            if (!spaceincludedlettersFile.exists()) {
                println("Hiba: A betűgyakorisági fájl nem található ($spaceincludedlettersFile).")
            } else {
                spaceincludedlettersFile.useLines { lines ->
                    var isFirstLine = true

                    lines.forEach { line ->
                        if (isFirstLine) {
                            isFirstLine = false
                            return@forEach
                        }

                        val parts = line.split('\t')
                        if (parts.size >= 2) {
                            val spaceincludedletter = parts[0]//lehet "[SPACE] is"
                            val percentageStr = parts[1].replace("%", "").replace(",", ".")
                            val percentage = percentageStr.toDoubleOrNull() ?: 0.0
                                loadedspaceincludedletters[spaceincludedletter] = percentage
                        }
                    }
                }
            }

            val lettersFile = File(baseDir, "nospaceletterfrequency.txt")
            val loadedletters = mutableMapOf<String, Double>()
            if (!lettersFile.exists()) {
                println("Hiba: A betűgyakorisági fájl nem található ($lettersFile).")
            } else {
                lettersFile.useLines { lines ->
                    var isFirstLine = true

                    lines.forEach { line ->
                        if (isFirstLine) {
                            isFirstLine = false
                            return@forEach
                        }

                        val parts = line.split('\t')
                        if (parts.size >= 2) {
                            val letter = parts[0]//ezek között nem lesz SPACE
                            val percentageStr = parts[1].replace("%", "").replace(",", ".")
                            val percentage = percentageStr.toDoubleOrNull() ?: 0.0
                            loadedletters[letter] = percentage
                        }
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
                letters = loadedletters,
                spaceincludedletters=loadedspaceincludedletters,
                bigrams = loadedBigrams,
                trigrams = loadedTrigrams,
                spaceIncludedTrigrams = loadedSpaceIncludedTrigrams,
            )
        }
    }
}