
import java.io.File

//olvassuk be az összefűzendő fájlok nevét

//használjunk egy while ciklust

//hogyha a beolvasott szöveg done, akkor ne adjunk több fájlnevet a listához

//olvassuk be a fájl nevét

//a buildcorpora függvény fűzze össze soronként a megadott fájlokat és adja ki egy új textfájl formájában az eredményt, ami legyen a processable_texts mappában


fun launchbuildcorpora() {
    println("Enter the name of processable files. These files should be located in the processable_texts folder.")
    println("Add each filename in a new line. Once you are done enter 'done'.")

    val fileNames = mutableListOf<String>()

    // Végtelen ciklus, amiből a 'done' beírásakor lépünk ki
    while (true) {
        val input = readln().trim() // A trim() leszedi a véletlen szóközöket az elejéről/végéről

        if (input.lowercase() == "done") {
            break
        }

        // Ellenőrizzük, hogy a fájl tényleg létezik-e a mappában
        val file = File("processable_texts/$input")
        if (file.exists() && file.isFile) {
            fileNames.add(input)
        } else {
            println("WARNING: File '$input' not found in processable_texts/. Skipped.")
        }
    }

    // Ha egyetlen érvényes fájlt sem adtunk meg, megszakítjuk a folyamatot
    if (fileNames.isEmpty()) {
        println("No valid files were provided. Process aborted.")
        return
    }

    println("Finally, name the new file. '.txt' will be added automatically.")
    val outputName = readln().trim()

    // Biztosítjuk, hogy a fájlnév végén ott legyen a .txt, ha a felhasználó elfelejtené
    val finalOutputName = if (outputName.endsWith(".txt")) outputName else "$outputName.txt"

    // Indítjuk az összefűzést
    buildcorpora(fileNames, finalOutputName)
}

fun buildcorpora(inputFiles: List<String>, outputFileName: String) {
    val outputFile = File("processable_texts/$outputFileName")

    // Biztosítjuk, hogy a mappa létezzen (bár ha voltak benne bemeneti fájlok, akkor már létezik)
    outputFile.parentFile?.mkdirs()

    println("Building corpora...")

    // 1. Megnyitjuk a kimeneti fájlt írásra (egyetlen egyszer!)
    outputFile.bufferedWriter().use { writer ->

        // 2. Végigmegyünk a listában lévő összes fájlnéven
        for (fileName in inputFiles) {
            val inputFile = File("processable_texts/$fileName")

            // 3. Az aktuális fájlt soronként streameljük
            inputFile.useLines { lines ->
                lines.forEach { line ->
                    writer.write(line)
                    writer.newLine()
                }
            }
            println("Merged successfully: $fileName")
        }
    }

    println("Corpora successfully built and saved as: ${outputFile.path}")
}