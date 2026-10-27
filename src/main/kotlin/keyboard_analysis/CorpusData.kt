package keyboard_analysis
import java.io.File

class CorpusData(
    val letterCounts: Map<Char, Long>,
    val totalLetters: Long,
    val bigramCounts: Map<String, Long>,
    val totalBigrams: Long,
    val trigramCounts: Map<String, Long>,
    val totalTrigrams: Long
) {
    companion object {
        fun loadFromDataset(datasetName: String): CorpusData {
            val cleanName = if (datasetName.endsWith(".txt")) datasetName else "$datasetName.txt"
            val baseDir = File("analysis_results/$cleanName")

            val letters = HashMap<Char, Long>()
            var totalL = 0L
            File(baseDir, "letterfrequency.txt").forEachLine { line ->
                val parts = line.split(":")
                if (parts.size >= 2 && parts[0].isNotEmpty()) {
                    val count = parts[1].trim().split(" ")[0].toLongOrNull() ?: 0L
                    letters[parts[0][0]] = count
                    totalL += count
                }
            }

            val bigrams = HashMap<String, Long>()
            var totalB = 0L
            File(baseDir, "bigramfrequency.txt").forEachLine { line ->
                val parts = line.split(":")
                if (parts.size >= 2 && parts[0].length == 2) {
                    val count = parts[1].trim().split(" ")[0].toLongOrNull() ?: 0L
                    bigrams[parts[0]] = count
                    totalB += count
                }
            }

            val trigrams = HashMap<String, Long>()
            var totalT = 0L
            File(baseDir, "trigramfrequency.txt").forEachLine { line ->
                val parts = line.split(":")
                if (parts.size >= 2 && parts[0].length == 3) {
                    val count = parts[1].trim().split(" ")[0].toLongOrNull() ?: 0L
                    trigrams[parts[0]] = count
                    totalT += count
                }
            }

            return CorpusData(letters, totalL, bigrams, totalB, trigrams, totalT)
        }
    }
}