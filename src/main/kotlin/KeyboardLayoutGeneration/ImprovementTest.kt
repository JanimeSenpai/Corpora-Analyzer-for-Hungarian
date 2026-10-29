package KeyboardLayoutGeneration

import keyboard_analysis.CorpusData
import keyboard_analysis.LayoutParser
import keyboard_analysis.completeLayoutAnalysis
import keyboard_analysis.printLayout
import layoutcharefforts
import renderCharEffortlayout
import java.io.File

fun main(){
    improvementTest()
}

fun improvementTest (){
    var layout = LayoutParser.parseLayout(File("keyboards/ltsnergomini.yaml"))
    var corpusData = CorpusData.loadFromDataset("huszonkettoHuszonnegyeswikipediahuszonegy.txt")

  var bestlayout=  improveLayout(layout, corpusData)
    printLayout(bestlayout)
    completeLayoutAnalysis(bestlayout, corpusData, verbose = true)
    layoutcharefforts(bestlayout)
    renderCharEffortlayout(bestlayout)

}