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
