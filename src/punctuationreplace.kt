fun String.englishpunctuationreplace() : String {
    return this.replace("?","/").replace(":",";")
}

fun String.hungarianpunctuationreplace() : String {
    return this.replace('?',',').replace(':','.')
}
