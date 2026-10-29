package keyboard_analysis

// Ezt deklaráld a fájl tetején, ha még nincs globálisan kint
data class KeyPosition(val finger: Int, val row: Int, val col: Int, val hand: Int)

fun buildCharPositionMap(layoutData: CombinedLayoutData): Map<Char, KeyPosition> {
    val charToPos = mutableMapOf<Char, KeyPosition>()

    // Ujj indexek meghatározása (10 ujj a hüvelykujjak miatt)
    val charToFinger = mutableMapOf<Char, Int>()
    val fingerSets = layoutData.getFingerCharacterSets()
    val fingerKeys = listOf("L_P", "L_R", "L_M", "L_I", "L_T", "R_T", "R_I", "R_M", "R_R", "R_P")
    for ((index, key) in fingerKeys.withIndex()) {
        fingerSets[key]?.forEach { char -> charToFinger[char] = index }
    }

    // Koordináták kinyerése a valós mátrixokból
    // Bal kéz (hand = 0)
    for (r in layoutData.leftChars.indices) {
        for (c in layoutData.leftChars[r].indices) {
            val char = layoutData.leftChars[r][c]
            if (char != '_') {
                val finger = charToFinger[char]
                if (finger != null) charToPos[char] = KeyPosition(finger, r, c, 0)
            }
        }
    }
    // Jobb kéz (hand = 1)
    for (r in layoutData.rightChars.indices) {
        for (c in layoutData.rightChars[r].indices) {
            val char = layoutData.rightChars[r][c]
            if (char != '_') {
                val finger = charToFinger[char]
                if (finger != null) charToPos[char] = KeyPosition(finger, r, c, 1)
            }
        }
    }
    return charToPos
}