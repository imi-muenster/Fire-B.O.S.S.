package de.uni_muenster.imi.fhirFacade.fhir.helper

import ca.uhn.fhir.context.FhirContext
import de.uni_muenster.imi.fhirFacade.basex.BaseXQueries
import de.uni_muenster.imi.fhirFacade.fhir.FhirServer
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.formats.ParserType
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*


private val fhirContext = FhirContext.forR4()
private val xmlParser = fhirContext.newXmlParser()
private val jsonParser = fhirContext.newJsonParser()
private val logger = KotlinLogging.logger {  }

fun encodeFromResource(resource: IBaseResource, parserType: ParserType = ParserType.XML): String? {
    return when (parserType) {
        ParserType.XML -> stripNamespaceFromXML(xmlParser.encodeResourceToString(resource))
        ParserType.JSON -> jsonParser.encodeResourceToString(resource)
        else -> null
    }
}


fun decodeFromString(resourceString: String): IBaseResource? {
    return try {
        if (resourceString.startsWith("<")) {
            decodeFromString(resourceString, ParserType.XML)
        } else {
            decodeFromString(resourceString, ParserType.JSON)
        }
    } catch(e: Exception) {
        null
    }
}

private fun decodeFromString(resourceString: String, parserType: ParserType): IBaseResource? {
    return when (parserType) {
        ParserType.XML -> xmlParser.parseResource(resourceString)
        ParserType.JSON -> jsonParser.parseResource(resourceString)
        else -> null
    }
}

fun decodeQueryResults(resultString: String): List<IBaseResource> {
    return splitSearchResults(resultString).mapNotNull {
        decodeFromString(it.trim())
    }
}

fun getDBNames(): List<String> {
    val result = FhirServer.baseX.executeXQuery(BaseXQueries.getDBNames())
    return result.split("\n")
}

fun getNewestVersionFromBundle(resources: List<IBaseResource>): IBaseResource? {
    return resources.sortedWith(compareBy {it.idElement.versionIdPart.toInt()}).lastOrNull()
}

fun splitSearchResults(result: String): List<String> {
    return try {
        StringUtils.substringsBetween(result, "<result>", "</result>").asList()
    } catch(e: Exception) {
        listOf()
    }
}

fun stripNamespaceFromXML(resource: String): String {
    return resource.replace("xmlns=\"http://hl7.org/fhir\"", "")
}

fun getAllResourceProviders(): Array<Class<*>?>? {
    return getClasses("de.uni_muenster.imi.fhirFacade.generated.r4")
}

/**
 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
 *
 * @param packageName The base package
 * @return The classes
 * @throws ClassNotFoundException
 * @throws IOException
 */
@Throws(ClassNotFoundException::class, IOException::class)
private fun getClasses(packageName: String): Array<Class<*>?>? {
    val classLoader = Thread.currentThread().contextClassLoader!!
    val path = packageName.replace('.', '/')
    val resources: Enumeration<URL> = classLoader.getResources(path)
    val dirs: MutableList<File> = ArrayList<File>()
    while (resources.hasMoreElements()) {
        val resource: URL = resources.nextElement()
        dirs.add(File(resource.file))
    }
    val classes = ArrayList<Class<*>>()
    for (directory in dirs) {
        classes.addAll(findClasses(directory, packageName))
    }
    return classes.toArray(arrayOfNulls(classes.size))
}

/**
 * Recursive method used to find all classes in a given directory and subdirs.
 *
 * @param directory   The base directory
 * @param packageName The package name for classes found inside the base directory
 * @return The classes
 * @throws ClassNotFoundException
 */
@Throws(ClassNotFoundException::class)
private fun findClasses(directory: File, packageName: String): List<Class<*>> {
    val classes: MutableList<Class<*>> = ArrayList()
    if (!directory.exists()) {
        return classes
    }
    val files: Array<File> = directory.listFiles()
    for (file in files) {
        if (file.isDirectory) {
            assert(!file.name.contains("."))
            classes.addAll(findClasses(file, packageName + "." + file.name))
        } else if (file.name.endsWith(".class")) {
            classes.add(Class.forName(packageName + '.' + file.name.substring(0, file.name.length - 6)))
        }
    }
    return classes
}

fun getResourceNames(): List<String> {
    val fhirContext = FhirContext.forR4()

    val baseResourceNames: MutableList<String> = mutableListOf()

    val p = Properties()
    try {
        p.load(fhirContext.version.fhirVersionPropertiesFile)
    } catch (e: java.io.IOException) {
        logger.error("Failed to load version property file", e)
    }


    for (next in p.keys) {
        val str = next as String

        if (str.startsWith("resource.")) {
            baseResourceNames.add(str.substring("resource.".length))
        }
    }

    return baseResourceNames
}



