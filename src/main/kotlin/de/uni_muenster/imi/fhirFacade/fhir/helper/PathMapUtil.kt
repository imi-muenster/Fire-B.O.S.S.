package de.uni_muenster.imi.fhirFacade.fhir.helper

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.tinder.model.BaseRootType
import ca.uhn.fhir.tinder.parser.ResourceGeneratorUsingModel
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

    fun getResources(): MutableList<BaseRootType>? {

        val baseResourceNames: MutableList<String> = mutableListOf()
        val fhirContext = FhirContext.forR4()

        val p = Properties()
        try {
            p.load(fhirContext.version.fhirVersionPropertiesFile)
        } catch (e: java.io.IOException) {
        }


        for (next in p.keys) {
            val str = next as String

            if (str.startsWith("resource.")) {
                baseResourceNames.add(str.substring("resource.".length).lowercase())
            }
        }

        if (fhirContext.version.version == ca.uhn.fhir.context.FhirVersionEnum.DSTU3) {
            baseResourceNames.remove("conformance")
        }


        val gen = ResourceGeneratorUsingModel("r4", "")
        gen.setBaseResourceNames(baseResourceNames)

        gen.parse()
        return gen.resources
    }

}