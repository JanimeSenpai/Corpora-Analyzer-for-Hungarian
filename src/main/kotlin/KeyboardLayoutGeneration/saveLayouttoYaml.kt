package KeyboardLayoutGeneration

import keyboard_analysis.CombinedLayoutData
import java.io.File

/**
 * Segédfüggvény: Kimásolja az eredeti YAML fájl fejlécét (a mátrixig),
 * majd beilleszti az új kiosztás karaktereit és a thumb billentyűket.
 */
fun saveLayoutToYaml(layout: CombinedLayoutData, originalFile: File, outputFile: File) {
    val originalLines = originalFile.readLines()
    var thumbLeftStr = "thumb_left: \"_\""
    var thumbRightStr = "thumb_right: \"_\""

    outputFile.bufferedWriter().use { writer ->
        // Kimásoljuk az eredeti fájl tetejét a mátrixig
        for (line in originalLines) {
            // Eltároljuk az eredeti thumb értékeket, hátha kellenek (ha a kódod nem mutálja őket)
            if (line.trim().startsWith("thumb_left:")) {
                thumbLeftStr = line
                continue
            }
            if (line.trim().startsWith("thumb_right:")) {
                thumbRightStr = line
                continue
            }
            if (line.trim().startsWith("matrix:")) {
                writer.write("matrix: |\n")
                break // Itt megállunk az olvasással, jön az új mátrix
            }
            writer.write(line + "\n")
        }

        // Beillesztjük az optimalizált mátrixot
        for (r in 0 until layout.leftChars.size) {
            writer.write("  ")
            // Egy kis szóközös formázás, hogy vizuálisan szép maradjon a YAML
            writer.write(layout.leftChars[r].joinToString(" "))
            writer.write("   ")
            writer.write(layout.rightChars[r].joinToString(" "))
            writer.write("\n")
        }

        // Visszaírjuk a thumb karaktereket
        // Megjegyzés: Ha a kódod menet közben mutálja a hüvelykujjat is,
        // és eltárolja mondjuk a layout.thumbLeft változóban, akkor ezt cseréld le erre:
        // writer.write("thumb_left: \"${layout.thumbLeft}\"\n")
        writer.write(thumbLeftStr + "\n")
        writer.write(thumbRightStr + "\n")
    }
}