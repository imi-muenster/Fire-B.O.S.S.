package de.unimuenster.imi.medic.fhir

import ca.uhn.fhir.model.api.annotation.Description
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.api.Constants
import ca.uhn.fhir.rest.api.server.RequestDetails
import ca.uhn.fhir.rest.param.StringAndListParam
import de.unimuenster.imi.medic.basex.BaseX
import de.unimuenster.imi.medic.basex.BaseXQueries
import de.unimuenster.imi.medic.basex.generator.QueryGenerator
import de.unimuenster.imi.medic.fhir.helper.*
import mu.KotlinLogging
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent
import org.hl7.fhir.r4.model.Bundle.HTTPVerb
import org.hl7.fhir.r4.model.Meta
import org.springframework.http.HttpStatus
import java.net.URI
import java.net.http.HttpRequest
import java.time.Instant
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ServerProvider {

    private val log = KotlinLogging.logger {  }

    //TODO: Implement
    @Transaction
    fun transaction(@TransactionParam theInput: Bundle): Bundle {
        if (theInput.type == Bundle.BundleType.TRANSACTION) {
            for (nextEntry in theInput.entry) {

                when (nextEntry.request.method) {
                    HTTPVerb.GET -> {}
                    HTTPVerb.POST -> {}
                    HTTPVerb.PUT -> {}
                    HTTPVerb.DELETE -> {}
                    HTTPVerb.PATCH -> {}
                    else -> {}
                }

            }
        }
        return theInput
    }

    //TODO: _since and _at not working
    @History
    fun getServerHistory(): List<IBaseResource> {
        val paramMap = ParameterConverter.getSearchParameterMapForTypeHistory(null, null)
        val gen = QueryGenerator()

        val resourceParts = buildString {
            for (resType in getDBNames()) {
                val pathMap: HashMap<String, String> = HashMap<String, String>().apply {
                    put("_at", "${resType}.meta.lastUpdated")
                    put("_since", "${resType}.meta.lastUpdated")
                }

                append(gen.getHistoryForType(paramMap, pathMap, resType))
            }
        }

        return decodeQueryResults(
            getBaseX().executeXQuery(
                BaseXQueries.getServerHistory(resourceParts)
            )
        )
    }

    @Search(allowUnknownParams = true)
    fun search(
        theServletRequest: HttpServletRequest,
        theServletResponse: HttpServletResponse,
        theRequestDetails: RequestDetails,

        @Description("The _type parameter is used to perform a search on multiple resources")
        @OptionalParam(name=Constants.PARAM_TYPE)
        theSearchForType: StringAndListParam

    ): Bundle {
        val types = filterResourceTypes(theSearchForType)
        val bundlesEntries: MutableList<BundleEntryComponent> = mutableListOf()
        val params = theRequestDetails.parameters.toMutableMap()
        params.remove("_type")
        val paramString = createParamString(params)

        for (type in types) {
            val baseURL = FhirServer.settings["baseURL"]
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseURL/$type?$paramString"))
                .build()

            val response = RequestUtil().performQuery(request)
            if (response != null && response.statusCode() == HttpStatus.OK.value())  {
                try {
                    bundlesEntries.addAll((decodeFromString(response.body()) as Bundle).entry)
                } catch (e: Exception) {
                    log.info("No valid bundle. Skipping Resource Type")
                }
            }
        }

        return createBundleFromEntries(bundlesEntries, theRequestDetails.completeUrl)
    }

    private fun createBundleFromEntries(entries: List<BundleEntryComponent>, searchURL: String): Bundle {
        return Bundle().apply {
            meta = Meta().apply {
                lastUpdated = Date.from(Instant.now())
            }
            type = Bundle.BundleType.SEARCHSET
            link = listOf(
                Bundle.BundleLinkComponent().apply {
                    relation="self"
                    url = searchURL
                }
            )
            total = entries.size
            entry = entries
        }
    }
    private fun createParamString(params: Map<String, Array<String>>): String {
        return buildString {
            for ((paramKey, paramValue) in params) {
                append("${paramKey}=${paramValue.joinToString(",")}&")
            }
        }.substringBeforeLast("&")
    }

    private fun filterResourceTypes(typeParam: StringAndListParam): List<String> {
        val result = mutableListOf<String>()
        for (stringOrListParam in typeParam.valuesAsQueryTokens) {
            for (stringParam in stringOrListParam.valuesAsQueryTokens) {
                result.add(stringParam.value)
            }
        }
        return result
    }

    private fun getBaseX(): BaseX {
        return FhirServer.baseX
    }

}