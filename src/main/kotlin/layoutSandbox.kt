package keyboard_analysis

import java.io.File

fun layoutSandbox() {
    println("Kérem a billentyűzetkiosztás nevét (pl. 'LTSN Ergo.yaml'):")
    var layoutName = readln().trim()
    if (!layoutName.endsWith(".yaml")) layoutName += ".yaml"

    val layoutFile = File("keyboards/$layoutName")
    if (!layoutFile.exists()) {
        println("Hiba: A billentyűzet fájl nem található ($layoutFile).")
        return
    }

    val layoutData = try {
        LayoutParser.parseLayout(layoutFile)
    } catch (e: Exception) {
        println("Hiba a betöltéskor: ${e.message}")
        return
    }

    println("\nSikeresen betöltve: ${layoutData.name}")
    printLayout(layoutData)

    println("Interaktív Sandbox elindítva. Elérhető parancsok:")
    println("- swapKeys <kez1> <sor1> <oszlop1> <kez2> <sor2> <oszlop2> (Kéz: l/r)")
    println("- swapColumns <kez1> <oszlop1> <kez2> <oszlop2>")
    println("- swapRowsLocal <kez> <sor1> <sor2>")
    println("- swapRowsGlobal <sor1> <sor2>")
    println("- mirrorHand <kez>")
    println("- exit vagy quit (Kilépés)")
    println("Példa: swapColumns l 1 r 2")

    while (true) {
        print("> ")
        val input = readln().trim()
        if (input.isEmpty()) continue
        if (input.lowercase() == "exit" || input.lowercase() == "quit") break

        val parts = input.split("\\s+".toRegex())
        val cmd = parts[0]

        try {
            var success = true
            when (cmd.lowercase()) {
                "swapkeys" -> {
                    val left1 = parseHand(parts[1])
                    val r1 = parts[2].toInt()
                    val c1 = parts[3].toInt()
                    val left2 = parseHand(parts[4])
                    val r2 = parts[5].toInt()
                    val c2 = parts[6].toInt()
                    layoutData.swapKeys(left1, r1, c1, left2, r2, c2)
                }
                "swapcolumns" -> {
                    val left1 = parseHand(parts[1])
                    val c1 = parts[2].toInt()
                    val left2 = parseHand(parts[3])
                    val c2 = parts[4].toInt()
                    success = layoutData.swapColumns(left1, c1, left2, c2)
                }
                "swaprowslocal" -> {
                    val isLeft = parseHand(parts[1])
                    val r1 = parts[2].toInt()
                    val r2 = parts[3].toInt()
                    success = layoutData.swapRowsLocal(isLeft, r1, r2)
                }
                "swaprowsglobal" -> {
                    val r1 = parts[1].toInt()
                    val r2 = parts[2].toInt()
                    success = layoutData.swapRowsGlobal(r1, r2)
                }
                "mirrorhand" -> {
                    val isLeft = parseHand(parts[1])
                    layoutData.mirrorHand(isLeft)
                }
                else -> {
                    println("Ismeretlen parancs: $cmd")
                    success = false
                }
            }

            if (success) {
                printLayout(layoutData)
            } else {
                println("❌ A módosítás el lett utasítva (pl. tiltott thumb row művelet).")
            }
        } catch (e: Exception) {
            println("❌ Hiba a parancs végrehajtásakor: ${e.message}. Ellenőrizd a paramétereket!")
        }
    }
}

// Segédfüggvény a bal/jobb kéz kényelmesebb beolvasásához
private fun parseHand(str: String): Boolean {
    return when (str.lowercase()) {
        "true", "left", "l", "bal", "1" -> true
        "false", "right", "r", "jobb", "0" -> false
        else -> throw IllegalArgumentException("Nem értelmezhető kéz megadás: $str (használj l vagy r betűt)")
    }
}

// Segédfüggvény a mátrix vizuális kiírásához
private fun printLayout(layout: CombinedLayoutData) {
    println("\n=== ${layout.name} Mátrix ===")

    // 1. Felső sorok (fő mátrix) kiírása
    val mainRows = layout.numRows - 1
    for (r in 0 until mainRows) {
        val leftStr = layout.leftChars[r].joinToString(" ")
        val rightStr = layout.rightChars[r].joinToString(" ")
        println("$leftStr   |   $rightStr")
    }

    // 2. Thumb sor kiírása pozicionálva
    val thumbRow = layout.numRows - 1
    val hw = layout.hardwareYaml

    // Megkeressük a fizikai hüvelykujj oszlopokat (ugyanaz a logika, mint a statisztikánál)
    val leftThumbCol = hw.left.fingers.indexOfFirst { it.equals("T", true) }.takeIf { it >= 0 } ?: (hw.left.fingers.size - 1)
    val rightThumbCol = hw.right.fingers.indexOfFirst { it.equals("T", true) }.takeIf { it >= 0 } ?: 0

    // Minden oszlop 2 karakter széles a konzolon (egy betű + egy szóköz a joinToString miatt)
    val leftPadding = " ".repeat(leftThumbCol * 2)
    val rightPadding = " ".repeat(rightThumbCol * 2)

    val leftThumbChar = layout.leftChars[thumbRow].firstOrNull() ?: '_'
    val rightThumbChar = layout.rightChars[thumbRow].firstOrNull() ?: '_'

    // A bal oldalt kitömjük szóközökkel a végén is, hogy a " | " elválasztó pontosan középen maradjon
    val leftTotalWidth = layout.leftChars[0].size * 2 - 1
    val leftFormatted = (leftPadding + leftThumbChar).padEnd(leftTotalWidth)

    println("$leftFormatted   |   $rightPadding$rightThumbChar")
    println("========================\n")
}