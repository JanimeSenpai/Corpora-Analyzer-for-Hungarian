package keyboard_analysis

fun calculateLayoutEffort(
    charEffortMap: Map<Char, Double>,
    corpus: CorpusData,
    verbose: Boolean = false
): Double {
    if (corpus.totalLetters == 0L) return 0.0

    var weightedEffortSum = 0.0
    var coveredLetters = 0L

    for ((char, count) in corpus.letterCounts) {
        val effort = charEffortMap[char]
        if (effort != null) {
            weightedEffortSum += effort * count
            coveredLetters += count
        }
    }

    val averageEffort = if (coveredLetters > 0) weightedEffortSum / coveredLetters else 0.0

    if (verbose) {
        println("Átlagos súlyozott leütési terhelés (Effort): ${String.format("%.4f", averageEffort)}")
    }

    return averageEffort
}