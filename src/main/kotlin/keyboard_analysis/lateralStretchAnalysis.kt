package keyboard_analysis

fun lateralStretchAnalysis(
    charPositions: Map<Char, KeyPosition>,
    bigrams: Map<String, Long>,
    verbose: Boolean
): Double {
    var stretchCount = 0L
    var totalBigrams = 0L

    for ((bigram, freq) in bigrams) {
        if (bigram.length != 2) continue
        val p1 = charPositions[bigram[0]] ?: continue
        val p2 = charPositions[bigram[1]] ?: continue
        totalBigrams += freq

        // Azonos kéz, és a logikai oszlopok távolsága >= 4
        if (p1.hand == p2.hand && Math.abs(p1.col - p2.col) >= 4) {
            stretchCount += freq
        }
    }
    val percentage = if (totalBigrams > 0) (stretchCount.toDouble() / totalBigrams) * 100.0 else 0.0
    if (verbose) println("Lateral Stretch Bigrams: ${String.format("%.3f", percentage)}%")
    return percentage
}