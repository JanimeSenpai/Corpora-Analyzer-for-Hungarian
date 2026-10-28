package keyboard_analysis

fun calculateWeightedReachEffortSum(
    charEffortMap: Map<Char, Double>,
    corpus: CorpusData,
    verbose: Boolean = false
): Double {
    // Ha a korpusz üres, ne adjunk vissza fals értéket
    if (corpus.spaceincludedletters.isEmpty()) return 0.0

    var weightedEffortSum = 0.0

    // Végigmegyünk a szóközös karakteradatokon
    // (Ideális esetben a szóköz effortja a hüvelykujjon közel van az 1.0-hoz)
    for ((charStr, percentage) in corpus.spaceincludedletters) {
        if (charStr.isNotEmpty()) {
            // A "[SPACE]" speciális stringet vagy a sima karaktert alakítjuk Char-rá
            val char = if (charStr == "[SPACE]") ' ' else charStr[0]

            val effort = charEffortMap[char]

            if (effort != null) {
                // A percentage egy Double érték 0.0 és 100.0 között (pl. 10.5%).
                // Ahhoz, hogy az effort a valóságnak megfeleljen, 100-zal osztjuk (0.105-ös szorzó)
                val probability = percentage / 100.0

                weightedEffortSum += effort * probability
            }
        }
    }

    if (verbose) {
        println("Átlagos súlyozott leütési terhelés (Char Reach Effort): ${String.format("%.4f", weightedEffortSum)}")
    }

    return weightedEffortSum
}