import keyboard_analysis.launch3RollAnalysis
import keyboard_analysis.launchAlternationAnalysis
import keyboard_analysis.launchCompleteLayoutAnalysis
import keyboard_analysis.launchRedirectAnalysis
import keyboard_analysis.launchRollAnalysis
import keyboard_analysis.launchsimplesfbanalysis
import kotlin.system.exitProcess

fun main() {
commandLineInterface()
}

fun commandLineInterface(){
    println("this is the command line interface of corpora analyzer")
    println("for available commands enter \"commands\"")
    readcommands()

}

fun readcommands(){
    val command  = readLine()

    if(command == null){
        println("the command should not be null")
    }else{
        when(command){
            "commands" -> listcommands()
            "preprocess file"->preprocess()
            "pf"->preprocess()
            "letter frequency"->{
               launchletterfrequency()
            }
            "lf"->launchletterfrequency()
            "buildcorpora"->launchbuildcorpora()
            "bc" -> launchbuildcorpora()
            "bigram analysis" -> launchbigramanalysis()
            "ba" -> launchbigramanalysis()
            "keyboard bigram analysis" -> launchsimplesfbanalysis()
            "kba" -> launchsimplesfbanalysis()
            "trigram analysis" -> launchtrigramanalysis()
            "ta" -> launchtrigramanalysis()
            "layout alternation analysis" -> launchAlternationAnalysis()
            "laa" -> launchAlternationAnalysis()
            "layout 2roll analysis" -> launchRollAnalysis()
            "l2ra" -> launchRollAnalysis()
            "layout 3roll analysis" -> launch3RollAnalysis()
            "l3ra" -> launch3RollAnalysis()
            "layout redirect analysis" ->launchRedirectAnalysis()
            "lra"->launchRedirectAnalysis()
            "complete layout analysis" -> launchCompleteLayoutAnalysis()
            "cla" -> launchCompleteLayoutAnalysis()
            "sta" ->launchSpaceIncludedTrigramAnalysis()
            "calculate word frequency"->launchCalculateWordFrequency()
            "cwf"-> launchCalculateWordFrequency()
            "exit"-> exitProcess(0)
        }

    }

    //utasítás végrehajtása után olvassa a következő utasítást
    readcommands()
}

fun listcommands(){
    println("letter frequency\n" +
            "bigram frequency\n"+
            "preprocess file")
}


fun launchletterfrequency(){
    println("enter filename")
    val filename =readLine()
    if(filename==null){
        println("filename should not be null")
        return
    }
    letterfrequency(filename)
}
