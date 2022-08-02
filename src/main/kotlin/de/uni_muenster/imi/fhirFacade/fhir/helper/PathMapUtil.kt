package de.uni_muenster.imi.fhirFacade.fhir.helper

import com.apicatalog.jsonld.loader.FileLoader
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.HashMap

object PathMapUtil {

    fun getPathMap(): Map<String, Map<String, String>> {
        val loadedMap: MutableMap<String, MutableMap<String, String>> = HashMap()

        val loadedProp = Properties()
        val input = InputStreamReader(FileLoader().javaClass.classLoader.getResourceAsStream("/resources/pathMap.properties")!!)
        loadedProp.load(input)

        for (key in loadedProp.stringPropertyNames()) {
            val (res, sp) = key.split(".")
            if (loadedMap.keys.contains(res)) {
                loadedMap[res]?.put(sp, loadedProp[key].toString())
            } else {
                loadedMap[res] = HashMap()
                loadedMap[res]?.put(sp, loadedProp[key].toString())
            }
        }
        return loadedMap
    }

}