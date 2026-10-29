package keyboard_analysis

fun fullScissorAnalysis(
    charPositions: Map<Char, KeyPosition>,
    bigrams: Map<String, Long>,
    verbose: Boolean
): Double {
    var scissorCount = 0L
    var totalBigrams = 0L

    for ((bigram, freq) in bigrams) {
        if (bigram.length != 2) continue
        val p1 = charPositions[bigram[0]] ?: continue
        val p2 = charPositions[bigram[1]] ?: continue
        totalBigrams += freq

        // Azonos kéz, különböző ujjak, hüvelykujjat (finger == 4 vagy 5) kihagyjuk
        if (p1.hand == p2.hand && p1.finger != p2.finger && p1.finger != 4 && p1.finger != 5 && p2.finger != 4 && p2.finger != 5) {
            if (Math.abs(p1.row - p2.row) >= 2) {//legalább két sor különbség van a két billentyű között
                // A 'row' index fentről indul, tehát a nagyobb index a fizikailag alsóbb sor
                val lowerPos = if (p1.row > p2.row) p1 else p2
                val higherPos = if (p1.row < p2.row) p1 else p2

                // Normalizáljuk az ujjindexet kéztől függetlenül, hogy 0-tól (kisujj) 3-ig (mutató) fusson
                // Bal kéz: 0=P, 1=R, 2=M, 3=I
                // Jobb kéz: 9=P, 8=R, 7=M, 6=I
                val fLower = if (lowerPos.hand == 0) lowerPos.finger else 9 - lowerPos.finger
                val fHigher = if (higherPos.hand == 0) higherPos.finger else 9 - higherPos.finger


                //todo ezeket ízlés szerint finomhangolni
                val isScissor = when (fLower) {
                    2 -> true // Középső ujj lent -> Mindig büntetendő
                    1 -> true // Gyűrűs ujj lent -> Mindig büntetendő
                    0 -> fHigher == 2 || fHigher == 1//kisujj lent keyboard layout doc szerint csak a magas mutatóujj büntetendő itt, de nekem pont az elviselhető, a több meg nem szeretem//fHigher == 3 || fHigher == 1 // Kisujj lent -> Csak I és R ellen büntetendő
                    else -> false // Mutatóujj lent (fLower == 3) -> Sosem büntetendő
                }
                if (isScissor) scissorCount += freq
            }
        }
    }
    val percentage = if (totalBigrams > 0) (scissorCount.toDouble() / totalBigrams) * 100.0 else 0.0
    if (verbose) println("Full Scissor Bigrams (FSB): ${String.format("%.3f", percentage)}%")
    return percentage
}