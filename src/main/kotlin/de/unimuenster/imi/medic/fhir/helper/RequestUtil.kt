package de.unimuenster.imi.medic.fhir.helper

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class RequestUtil {

    fun performQuery(theRequest: HttpRequest): HttpResponse<String>? {
        val client = HttpClient.newBuilder().build()

        val response = client.send(theRequest, HttpResponse.BodyHandlers.ofString())
        return response
    }

}