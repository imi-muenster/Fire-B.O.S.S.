package de.uni_muenster.imi.fhirFacade.utils

import de.uni_muenster.imi.fhirFacade.basex.BaseX
import de.uni_muenster.imi.fhirFacade.fhir.helper.decodeFromString
import de.uni_muenster.imi.fhirFacade.fhir.helper.encodeFromResource
import de.uni_muenster.imi.fhirFacade.fhir.helper.getResourceNames
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.formats.ParserType
import java.io.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main() {
    createAllDatabases()
}

fun fillServer() {
    val files = File("src/main/resources/fhirResources/examples-json")
    for (file in files.listFiles()!!) {
        val resource = decodeFromString(file.readText())
        if (resource != null) {
            addToServer(resource)
        }
    }
}

fun addToServer(resource: IBaseResource) {
    val body = encodeFromResource(resource, ParserType.JSON)

    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/fhir/${resource.fhirType()}"))
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .setHeader("Content-type", "application/json")
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    println(response.body())
}

fun createAllDatabases() {
    val basex = BaseX(Properties(path = "settings/default.properties"))
    getResourceNames().forEach {
        basex.createDbIfNotExist(it)
        basex.createDbIfNotExist("${it}_history")
    }
}
