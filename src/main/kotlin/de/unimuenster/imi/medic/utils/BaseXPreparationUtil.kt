package de.unimuenster.imi.medic.utils

import de.unimuenster.imi.medic.basex.BaseX
import de.unimuenster.imi.medic.fhir.helper.decodeFromString
import de.unimuenster.imi.medic.fhir.helper.encodeFromResource
import de.unimuenster.imi.medic.fhir.helper.getResourceNames
import mu.KotlinLogging
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.formats.ParserType
import java.io.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private val log = KotlinLogging.logger {}

fun main() {
    fillServer()
}

fun fillServer() {
    val files = File("src/main/resources/fhirResources/examples-json")
    for (file in files.listFiles()!!) {
        val resource = decodeFromString(file.readText())
        if (resource != null) {
            log.info("Adding resource: ${resource.fhirType()} - ID: ${resource.idElement}")
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
