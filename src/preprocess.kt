import java.io.File


fun preprocess() {
    println("Enter the filename")
    val filename =readLine()!!
    var toprocess = File("raw_texts\\$filename")
    var resultfile =File("processable_texts\\$filename")

    stripIndicesAndSave(toprocess, resultfile)
}





fun stripIndicesAndSave(inputFile: File, outputFile: File) {
    // Open the destination file using a BufferedWriter for fast disk writing
    outputFile.bufferedWriter().use { writer ->

        // Stream the source file line-by-line
        inputFile.useLines { lines ->
            lines.forEach { line ->

                // substringAfter searches for the first tab character ('\t')
                // and returns everything after it.
                val cleanedText = line.substringAfter('\t')

                // Write the extracted text to the new file and add a line break
                writer.write(cleanedText)
                writer.newLine()
            }
        }
    }
    println("Finished processing file")
}