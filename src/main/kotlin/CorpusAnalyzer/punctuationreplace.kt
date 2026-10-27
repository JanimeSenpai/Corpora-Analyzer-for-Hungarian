package CorpusAnalyzer

import currentLocale

fun String.englishpunctuationreplace() : String {
    return this.replace("?","/").replace(":",";")
}

fun String.hungarianpunctuationreplace() : String {
    return this.replace('?',',').replace(':','.')
}
fun String.punctuationReplace(): String = when (currentLocale) {
    Locale.HU -> this.hungarianpunctuationreplace()
    Locale.EN -> this.englishpunctuationreplace()
}