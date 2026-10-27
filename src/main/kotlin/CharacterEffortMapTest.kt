import EffortModel.*
import java.io.File

/*
fun main() {
    // A parser példányosítása (a hardvert és a betűket is a YAML-ből építi fel)
    val parser = LayoutEffortParser()

    // A fájl relatív útvonala a gyökérkönyvtárból indítva
    val yamlFile = File("keyboards/ltsn_ergo.yaml")

    if (!yamlFile.exists()) {
        println("Hiba: A fájl nem található itt: ${yamlFile.absolutePath}")
        return
    }

    // Fájl beolvasása és az effort map legenerálása
    val effortMap = parser.parseAndMapLayout(yamlFile)

    // Eredmény kiíratása (terhelés szerint növekvő sorrendben)
    println("Beolvasott billentyűzet karakter-terhelései:")
    effortMap.entries.sortedBy { it.value }.forEach { (char, effort) ->
        // 3 tizedesjegyre kerekítve a könnyebb olvashatóságért
        println("$char : ${String.format("%.3f", effort)}")
    }
}*/